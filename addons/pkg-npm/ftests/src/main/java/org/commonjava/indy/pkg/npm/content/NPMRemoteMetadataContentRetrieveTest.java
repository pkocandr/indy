/**
 * Copyright (C) 2017 Red Hat, Inc. (yma@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.pkg.npm.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class NPMRemoteMetadataContentRetrieveTest
                extends AbstractContentManagementTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    @Test
    public void test() throws Exception
    {
        final String packagecontent =
                        "{\"name\": \"jquery\",\n" + "\"description\": \"JavaScript library for DOM operations\",\n"
                                        + "\"license\": \"MIT\"}";
        final String versionContent =
                        "{\"name\": \"jquery\",\n" + "\"url\": \"jquery.com\",\n" + "\"version\": \"1.1.0\"}";

        final String packagePath = "jquery/package.json";
        final String versionPath = "jquery/1.1.0/package.json";

        server.expect( server.formatUrl( STORE, packagePath ), 200,
                       new ByteArrayInputStream( packagecontent.getBytes() ) );
        server.expect( server.formatUrl( STORE, versionPath ), 200,
                       new ByteArrayInputStream( versionContent.getBytes() ) );

        final RemoteRepository remoteRepository = new RemoteRepository( NPM_PKG_KEY, STORE, server.formatUrl( STORE ) );
        final StoreKey storeKey = remoteRepository.getKey();

        client.stores().create( remoteRepository, "adding npm remote repo", RemoteRepository.class );

        final InputStream packageStream = client.content().get( storeKey, "jquery" );
        final InputStream versionStream = client.content().get( storeKey, "jquery/1.1.0" );

        assertThat( packageStream, notNullValue() );
        assertThat( versionStream, notNullValue() );

        assertThat( IOUtils.toString( packageStream ), equalTo( packagecontent ) );
        assertThat( IOUtils.toString( versionStream ), equalTo( versionContent ) );

        packageStream.close();
        versionStream.close();
    }
}
