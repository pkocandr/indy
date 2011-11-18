/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.sec.fixture;

import java.io.File;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.commonjava.aprox.core.conf.DefaultProxyConfiguration;
import org.commonjava.aprox.core.conf.ProxyConfiguration;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.auth.couch.conf.DefaultUserManagerConfig;
import org.commonjava.auth.couch.conf.UserManagerConfiguration;
import org.commonjava.auth.couch.inject.UserData;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.test.fixture.TestData;

@Singleton
public class ProxyConfigProvider
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    public static final String APROX_DATABASE_URL = "aprox.db.url";

    public static final String USER_DATABASE_URL = "user.db.url";

    private DefaultProxyConfiguration config;

    private UserManagerConfiguration umConfig;

    @Produces
    @TestData
    @UserData
    @Default
    public synchronized CouchDBConfiguration getCouchDBConfiguration()
    {
        return getUserManagerConfiguration().getDatabaseConfig();
    }

    @Produces
    @TestData
    @Default
    public synchronized UserManagerConfiguration getUserManagerConfiguration()
    {
        if ( umConfig == null )
        {
            umConfig =
                new DefaultUserManagerConfig( "admin@nowhere.com", "password", "Admin", "User",
                                              "http://localhost:5984/test-user-manager" );
        }

        return umConfig;
    }

    @Produces
    @TestData
    @Default
    public synchronized ProxyConfiguration getProxyConfiguration()
    {
        if ( config == null )
        {
            config = new DefaultProxyConfiguration( "http://localhost:5984/test-aprox" );

            config.setRepositoryRootDirectory( new File( System.getProperty( REPO_ROOT_DIR, "target/repo-downloads" ) ) );
        }

        return config;
    }

    @Produces
    @AproxData
    @TestData
    // @Default
    public CouchDBConfiguration getCouchConfiguration()
    {
        return getProxyConfiguration().getDatabaseConfig();
    }

    // @Produces
    // @TestData
    // @Default
    // public synchronized ProxyConfiguration getWeldProxyConfiguration()
    // {
    // return getProxyConfiguration();
    // }
    //
    // @Produces
    // @AproxData
    // @TestData
    // @Default
    // public CouchDBConfiguration getWeldCouchConfiguration()
    // {
    // return getProxyConfiguration().getDatabaseConfig();
    // }

}
