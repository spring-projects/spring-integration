/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.message;

import org.jspecify.annotations.Nullable;

/**
 * Hazelcast Message Payload for Entry Events.
 *
 * @param <K> the entry key type
 * @param <V> the entry value type
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 *
 * @param key The entry key.
 * @param value The entry value.
 * @param oldValue The entry old value if any.
 */
public record EntryEventMessagePayload<K, V>(K key, @Nullable V value, @Nullable V oldValue) {

}
