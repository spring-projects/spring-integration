/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
