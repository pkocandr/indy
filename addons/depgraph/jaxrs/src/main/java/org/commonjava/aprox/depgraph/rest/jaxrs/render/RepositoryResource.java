/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.rest.jaxrs.render;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.galley.util.UrlUtils.buildUrl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.dto.WebOperationConfigDTO;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.galley.CacheOnlyLocation;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.maven.cartographer.util.ProjectVersionRefComparator;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferBatch;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/repo" )
public class RepositoryResource
{

    private static final String URLMAP_DATA_REPO_URL = "repoUrl";

    private static final String URLMAP_DATA_FILES = "files";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ResolveOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private LocationExpander locationExpander;

    @Inject
    private TransferManager transferManager;

    @Inject
    private PresetSelector presets;

    @Inject
    private AproxDepgraphConfig config;

    @POST
    @Path( "/urlmap" )
    @Produces( "application/json" )
    public Response getUrlMap( @Context final HttpServletRequest req, @Context final HttpServletResponse resp, @Context final UriInfo info )
        throws AproxWorkflowException
    {
        final Map<ProjectVersionRef, Map<String, Object>> result = new LinkedHashMap<ProjectVersionRef, Map<String, Object>>();

        try
        {
            final WebOperationConfigDTO dto = readDTO( req );

            final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( dto, req );

            final List<ProjectVersionRef> topKeys = new ArrayList<ProjectVersionRef>( contents.keySet() );
            Collections.sort( topKeys, new ProjectVersionRefComparator() );

            for ( final ProjectVersionRef gav : topKeys )
            {
                final Map<ArtifactRef, ConcreteResource> items = contents.get( gav );

                final Map<String, Object> data = new HashMap<String, Object>();
                result.put( gav, data );

                final Set<String> files = new HashSet<String>();
                KeyedLocation kl = null;

                for ( final ConcreteResource item : items.values() )
                {
                    final KeyedLocation loc = (KeyedLocation) item.getLocation();

                    // FIXME: we're squashing some potential variation in the locations here!
                    // if we're not looking for local urls, allow any cache-only location to be overridden...
                    if ( kl == null || ( !dto.getLocalUrls() && ( kl instanceof CacheOnlyLocation ) ) )
                    {
                        kl = loc;
                    }

                    logger.info( "Adding %s (keyLocation: %s)", item, kl );
                    files.add( new File( item.getPath() ).getName() );
                }

                final List<String> sortedFiles = new ArrayList<String>( files );
                Collections.sort( sortedFiles );
                data.put( URLMAP_DATA_REPO_URL, formatUrlMapRepositoryUrl( kl, info, dto.getLocalUrls() ) );
                data.put( URLMAP_DATA_FILES, sortedFiles );
            }
        }
        catch ( final MalformedURLException e )
        {
            throw new AproxWorkflowException( "Failed to generate runtime repository. Reason: %s", e, e.getMessage() );
        }

        final String json = serializer.toString( result );

        return Response.ok( json )
                       .type( "application/json" )
                       .build();
    }

    @POST
    @Path( "/downlog" )
    @Produces( "text/plain" )
    public Response getDownloadLog( @Context final HttpServletRequest req, @Context final HttpServletResponse resp, @Context final UriInfo info )
        throws AproxWorkflowException
    {
        final Set<String> downLog = new HashSet<String>();
        try
        {
            final WebOperationConfigDTO dto = readDTO( req );

            final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( dto, req );

            final List<ProjectVersionRef> refs = new ArrayList<ProjectVersionRef>( contents.keySet() );
            Collections.sort( refs );

            for ( final ProjectVersionRef ref : refs )
            {
                final Map<ArtifactRef, ConcreteResource> items = contents.get( ref );
                for ( final ConcreteResource item : items.values() )
                {
                    logger.info( "Adding: '%s'", item );
                    downLog.add( formatDownlogEntry( item, info, dto.getLocalUrls() ) );
                }
            }
        }
        catch ( final MalformedURLException e )
        {
            throw new AproxWorkflowException( "Failed to generate runtime repository. Reason: %s", e, e.getMessage() );
        }

        final List<String> sorted = new ArrayList<String>( downLog );
        Collections.sort( sorted );

        final String output = join( sorted, "\n" );

        return Response.ok( output )
                       .type( "text/plain" )
                       .build();
    }

    @POST
    @Path( "/zip" )
    @Produces( "application/zip" )
    public Response getZipRepository( @Context final HttpServletRequest req, @Context final HttpServletResponse resp )
        throws AproxWorkflowException
    {
        Response response = Response.noContent()
                                    .build();

        ZipOutputStream stream = null;
        try
        {
            final WebOperationConfigDTO dto = readDTO( req );

            final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents = resolveContents( dto, req );

            final Set<ConcreteResource> entries = new HashSet<ConcreteResource>();
            final Set<String> seenPaths = new HashSet<String>();

            logger.info( "Iterating contents with %d GAVs.", contents.size() );
            for ( final Map<ArtifactRef, ConcreteResource> artifactResources : contents.values() )
            {
                for ( final Entry<ArtifactRef, ConcreteResource> entry : artifactResources.entrySet() )
                {
                    final ArtifactRef ref = entry.getKey();
                    final ConcreteResource resource = entry.getValue();

                    //                        logger.info( "Checking %s (%s) for inclusion...", ref, resource );

                    final String path = resource.getPath();
                    if ( seenPaths.contains( path ) )
                    {
                        logger.info( "Conflicting path: %s. Skipping %s.", path, ref );
                        continue;
                    }

                    seenPaths.add( path );

                    //                        logger.info( "Adding to batch: %s via resource: %s", ref, resource );
                    entries.add( resource );
                }
            }

            logger.info( "Starting batch retrieval of %d artifacts.", entries.size() );
            TransferBatch batch = new TransferBatch( entries );
            batch = transferManager.batchRetrieve( batch );

            logger.info( "Retrieved %d artifacts. Creating zip.", batch.getTransfers()
                                                                       .size() );

            // FIXME: Stream to a temp file, then pass that to the Response.ok() handler...
            final OutputStream os = resp.getOutputStream();
            stream = new ZipOutputStream( os );

            final List<Transfer> items = new ArrayList<Transfer>( batch.getTransfers()
                                                                       .values() );
            Collections.sort( items, new Comparator<Transfer>()
            {
                @Override
                public int compare( final Transfer f, final Transfer s )
                {
                    return f.getPath()
                            .compareTo( s.getPath() );
                }
            } );

            for ( final Transfer item : items )
            {
                //                    logger.info( "Adding: %s", item );
                final String path = item.getPath();
                if ( item != null )
                {
                    final ZipEntry ze = new ZipEntry( path );
                    stream.putNextEntry( ze );

                    InputStream itemStream = null;
                    try
                    {
                        itemStream = item.openInputStream();
                        copy( itemStream, stream );
                    }
                    finally
                    {
                        closeQuietly( itemStream );
                    }
                }
            }
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to generate runtime repository. Reason: %s", e, e.getMessage() );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( "Failed to generate runtime repository. Reason: %s", e, e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }

        response = Response.ok()
                           .type( "application/zip" )
                           .build();

        return response;
    }

    private String formatDownlogEntry( final ConcreteResource item, final UriInfo info, final boolean localUrls )
        throws MalformedURLException
    {
        final KeyedLocation kl = (KeyedLocation) item.getLocation();
        final StoreKey key = kl.getKey();

        if ( localUrls || kl instanceof CacheOnlyLocation )
        {
            final URI uri = info.getBaseUriBuilder()
                                .path( key.getType()
                                          .singularEndpointName() )
                                .path( key.getName() )
                                .path( item.getPath() )
                                .build();

            return String.format( "Downloading: %s", uri.toURL()
                                                        .toExternalForm() );
        }
        else
        {
            return "Downloading: " + buildUrl( item.getLocation()
                                                   .getUri(), item.getPath() );
        }
    }

    private String formatUrlMapRepositoryUrl( final KeyedLocation kl, final UriInfo info, final boolean localUrls )
        throws MalformedURLException
    {
        if ( localUrls || kl instanceof CacheOnlyLocation )
        {
            final StoreKey key = kl.getKey();
            final URI uri = info.getBaseUriBuilder()
                                .path( key.getType()
                                          .singularEndpointName() )
                                .path( key.getName() )
                                .build();

            return uri.toURL()
                      .toExternalForm();
        }
        else
        {
            return kl.getUri();
        }
    }

    private Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> resolveContents( final WebOperationConfigDTO dto, final HttpServletRequest req )
        throws AproxWorkflowException
    {
        if ( dto == null )
        {
            logger.warn( "Repository archive configuration is missing." );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "JSON configuration not supplied" );
        }

        dto.resolveFilters( presets, config.getDefaultWebFilterPreset() );

        if ( !dto.isValid() )
        {
            logger.warn( "Repository archive configuration is invalid: %s", dto );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Invalid configuration: %s", dto );
        }

        Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents;
        try
        {
            contents = ops.resolveRepositoryContents( dto );
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to resolve repository contents for: %s. Reason: %s", e, dto, e.getMessage() );
            throw new AproxWorkflowException( "Failed to resolve repository contents for: %s. Reason: %s", e, dto, e.getMessage() );
        }

        return contents;
    }

    private WebOperationConfigDTO readDTO( final HttpServletRequest req )
        throws AproxWorkflowException
    {
        String json;
        try
        {
            json = IOUtils.toString( req.getInputStream() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read configuration JSON from request body. Reason: %s", e, e.getMessage() );
        }

        logger.info( "Got configuration JSON:\n\n%s\n\n", json );
        final WebOperationConfigDTO dto = serializer.fromString( json, WebOperationConfigDTO.class );

        try
        {
            dto.calculateLocations( locationExpander );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "One or more sources/excluded sources is invalid: %s", e, e.getMessage() );
        }

        return dto;
    }

}