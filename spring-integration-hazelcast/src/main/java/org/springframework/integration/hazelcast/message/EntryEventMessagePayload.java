/*
 * Copyright 2015-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
