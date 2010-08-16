package org.springframework.integration.endpoint.metadata;

import org.springframework.util.Assert;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory implementation of teh {@link org.springframework.integration.endpoint.metadata.MetadataPersister}
 * interface suitable for the use cases where it's assured that component only needs ephemeral metadata.
 *
 *
 * @author Josh Long
 * @param <T>        the type of objects to be stored as values. Keys will always be {@link String}
 */
public class MapBasedMetadataPersister <T>  implements MetadataPersister<T> {

    private ConcurrentHashMap<String,T>  metadataMap = new ConcurrentHashMap<String,T>() ;

    public void write(String key, T value) {
        Assert.notNull( key != null , "key can't be null");
        Assert.notNull( value != null , "value can't be null");
        this.metadataMap.put( key, value);
    }

    public T read(String key) {
        return  this.metadataMap.get(key);
    }
}
