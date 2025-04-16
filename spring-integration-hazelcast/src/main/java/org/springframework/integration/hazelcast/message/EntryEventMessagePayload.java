/*
 * Copyright 2015-2022 the original author or authors.
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

import org.springframework.util.Assert;

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
 */
public class EntryEventMessagePayload<K, V> {

	/**
	 * The entry key.
	 */
	public final K key;

	/**
	 * The entry value.
	 */
	public final V value;

	/**
	 * The entry old value if any.
	 */
	public final V oldValue;

	public EntryEventMessagePayload(final K key, final V value, final V oldValue) {
		Assert.notNull(key, "'key' must not be null");
		this.key = key;
		this.value = value;
		this.oldValue = oldValue;
	}

	@Override
	public String toString() {
		return "EntryEventMessagePayload [key=" + this.key + ", value=" + this.value + ", oldValue=" + this.oldValue + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		EntryEventMessagePayload<?, ?> that = (EntryEventMessagePayload<?, ?>) o;

		return this.key.equals(that.key) && !(this.value != null ? !this.value.equals(that.value)
				: that.value != null) && !(this.oldValue != null
				? !this.oldValue.equals(that.oldValue) : that.oldValue != null);

	}

	@Override
	public int hashCode() {
		int result = this.key.hashCode();
		result = 31 * result + (this.value != null ? this.value.hashCode() : 0);
		result = 31 * result + (this.oldValue != null ? this.oldValue.hashCode() : 0);
		return result;
	}

}
