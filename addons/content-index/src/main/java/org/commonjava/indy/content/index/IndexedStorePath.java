/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.content.index;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by jdcasey on 3/15/16.
 */
@Indexed(index = "indexedStorePath")
public class IndexedStorePath
        implements Externalizable
{

    @Field( name = "storeType", store = Store.YES, analyze = Analyze.NO )
    private StoreType storeType;

    @Field( name = "storeName", store = Store.YES, analyze = Analyze.NO )
    private String storeName;

    @Field( name = "originStoreType", store = Store.YES, analyze = Analyze.NO )
    private StoreType originStoreType;

    @Field( name = "originStoreName", store = Store.YES, analyze = Analyze.NO )
    private String originStoreName;

    @Field( name = "path", store = Store.YES, analyze = Analyze.NO )
    private String path;

    public IndexedStorePath( StoreKey storeKey, StoreKey origin, String path )
    {
        this.storeType = storeKey.getType();
        this.storeName = storeKey.getName();
        this.originStoreType = origin.getType();
        this.originStoreName = origin.getName();

        this.path = path;
    }

    public StoreKey getStoreKey()
    {
        return new StoreKey( storeType, storeName );
    }

    public StoreKey getOriginStoreKey()
    {
        return new StoreKey( originStoreType, originStoreName );
    }

    public StoreType getStoreType()
    {
        return storeType;
    }

    public String getStoreName()
    {
        return storeName;
    }

    public String getPath()
    {
        return path;
    }

    @Override
    public String toString()
    {
        return "IndexedStorePath{" +
                "storeType=" + storeType +
                ", storeName='" + storeName + '\'' +
                ", originStoreType=" + originStoreType +
                ", originStoreName='" + originStoreName + '\'' +
                ", path='" + path + '\'' +
                '}';
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof IndexedStorePath ) )
        {
            return false;
        }

        IndexedStorePath that = (IndexedStorePath) o;

        if ( getStoreType() != that.getStoreType() )
        {
            return false;
        }
        if ( !getStoreName().equals( that.getStoreName() ) )
        {
            return false;
        }
        if ( originStoreType != that.originStoreType )
        {
            return false;
        }
        if ( !originStoreName.equals( that.originStoreName ) )
        {
            return false;
        }
        return getPath().equals( that.getPath() );

    }

    @Override
    public int hashCode()
    {
        int result = getStoreType().hashCode();
        result = 31 * result + getStoreName().hashCode();
        result = 31 * result + originStoreType.hashCode();
        result = 31 * result + originStoreName.hashCode();
        result = 31 * result + getPath().hashCode();
        return result;
    }

    @Override
    public void writeExternal( ObjectOutput out )
            throws IOException
    {
        out.writeObject( storeType.name() );
        out.writeObject( storeName );
        out.writeObject( originStoreType.name() );
        out.writeObject( originStoreName );
        out.writeObject( path );
    }

    @Override
    public void readExternal( ObjectInput in )
            throws IOException, ClassNotFoundException
    {
        storeType = StoreType.get( (String) in.readObject() );
        storeName = (String) in.readObject();
        originStoreType = StoreType.get( (String) in.readObject() );
        originStoreName = (String) in.readObject();
        path = (String) in.readObject();
    }
}
