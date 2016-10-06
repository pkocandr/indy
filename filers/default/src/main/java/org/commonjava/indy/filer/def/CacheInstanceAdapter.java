package org.commonjava.indy.filer.def;

import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.galley.cache.infinispan.CacheInstance;
import org.commonjava.maven.galley.cache.infinispan.SimpleCacheInstance;
import org.infinispan.Cache;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import java.util.function.Function;

/**
 * Created by jdcasey on 10/6/16.
 */
public class CacheInstanceAdapter
        implements CacheInstance<String, String>
{
    private CacheHandle<String, String> cacheHandle;

    public CacheInstanceAdapter( CacheHandle<String, String> cacheHandle )
    {
        this.cacheHandle = cacheHandle;
    }

    @Override
    public String getName()
    {
        return cacheHandle.getName();
    }

    @Override
    public <R> R execute( Function<Cache<String, String>, R> operation )
    {
        return cacheHandle.execute( operation );
    }

    @Override
    public void stop()
    {
        cacheHandle.stop();
    }

    @Override
    public boolean containsKey( String key )
    {
        return cacheHandle.containsKey( key );
    }

    @Override
    public String put( String key, String value )
    {
        return cacheHandle.put( key, value );
    }

    @Override
    public String putIfAbsent( String key, String value )
    {
        return cacheHandle.putIfAbsent( key, value );
    }

    @Override
    public String remove( String key )
    {
        return cacheHandle.remove( key );
    }

    @Override
    public String get( String key )
    {
        return cacheHandle.get( key );
    }

    @Override
    public void beginTransaction()
            throws NotSupportedException, SystemException
    {
        cacheHandle.beginTransaction();
    }

    @Override
    public void rollback()
            throws SystemException
    {
        cacheHandle.rollback();
    }

    @Override
    public void commit()
            throws SystemException, HeuristicMixedException, HeuristicRollbackException, RollbackException
    {
        cacheHandle.commit();
    }

    @Override
    public int getTransactionStatus()
            throws SystemException
    {
        return cacheHandle.getTransactionStatus();
    }

    @Override
    public Object getLockOwner( String key )
    {
        return cacheHandle.getLockOwner( key );
    }

    @Override
    public boolean isLocked( String key )
    {
        return cacheHandle.isLocked( key );
    }

    @Override
    public void lock( String... keys )
    {
        cacheHandle.lock( keys );
    }
}
