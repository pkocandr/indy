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
package org.commonjava.aprox.depgraph.discover;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.event.RelationshipStorageEvent;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class RepoSourceMetadataListener
{

    private static final String FOUND_IN_METADATA = "found-in-repo";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager aprox;

    @Inject
    private CartoDataManager carto;

    public RepoSourceMetadataListener()
    {
    }

    public RepoSourceMetadataListener( final StoreDataManager aprox, final CartoDataManager carto )
    {
        this.aprox = aprox;
        this.carto = carto;
    }

    public void addRepoMetadata( @Observes final RelationshipStorageEvent event )
    {
        final Set<ProjectRelationship<?>> stored = event.getStored();
        if ( stored == null )
        {
            return;
        }

        final Map<URI, Repository> repos = new HashMap<URI, Repository>();
        final Set<URI> unmatchedSources = new HashSet<URI>();

        final Set<ProjectVersionRef> seen = new HashSet<ProjectVersionRef>();
        for ( final ProjectRelationship<?> rel : stored )
        {
            final ProjectVersionRef ref = rel.getDeclaring()
                                             .asProjectVersionRef();
            if ( seen.contains( ref ) )
            {
                continue;
            }

            seen.add( ref );

            final StringBuilder sb = new StringBuilder();
            final Set<URI> srcs = rel.getSources();
            if ( srcs == null )
            {
                continue;
            }

            for ( final URI src : srcs )
            {
                if ( unmatchedSources.contains( src ) )
                {
                    continue;
                }

                Repository repo = repos.get( src );
                final String scheme = src.getScheme();

                if ( repo != null || StoreType.get( scheme ) == StoreType.repository )
                {
                    if ( repo == null )
                    {
                        final String sub = src.getSchemeSpecificPart();
                        try
                        {
                            repo = aprox.getRepository( sub );
                        }
                        catch ( final ProxyDataException e )
                        {
                            logger.error( "Failed to retrieve repository with name: '%s' for %s metadata association in dependency graph. Reason: %s",
                                          e, sub, FOUND_IN_METADATA, e.getMessage() );
                        }
                    }

                    if ( repo != null )
                    {
                        repos.put( src, repo );

                        if ( sb.length() > 0 )
                        {
                            sb.append( ',' );
                        }

                        sb.append( repo.getKey() )
                          .append( '@' )
                          .append( repo.getUrl() );
                    }
                    else
                    {
                        unmatchedSources.add( src );
                    }
                }
                else
                {
                    unmatchedSources.add( src );
                }
            }

            if ( sb.length() > 0 )
            {
                carto.addMetadata( ref, FOUND_IN_METADATA, FOUND_IN_METADATA );
            }
        }
    }

}
