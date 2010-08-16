package org.springframework.integration.endpoint.metadata;

import java.util.Properties;


/**
 * Envisioned as a strategy interface for persisting metadata from certain adapters / endpoints. Ideally,
 * there will be at least two options - one ephemeral persister (RAM-only) and one durable (<code>*.ini</code> based).
 * <p/>
 * This is used to give adapters / endpoints a place to store metadata to avoid duplicate delivery of messages, for example.
 *
 * @author Josh Long
 */
public interface MetadataPersister<V> {
    void write(String key, V value);
    V read(String key);
}
