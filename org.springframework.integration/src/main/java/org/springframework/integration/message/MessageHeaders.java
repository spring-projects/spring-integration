/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.message;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The headers for a {@link Message}.
 * 
 * @author Arjen Poutsma
 * @author Mark Fisher
 */
public final class MessageHeaders implements Map<String, Object>, Serializable {

	public static final String ID = "internal.header.id";

	public static final String TIMESTAMP = "internal.header.timestamp";

	public static final String CORRELATION_ID = "internal.header.correlationId";

	public static final String RETURN_ADDRESS = "internal.header.returnAddress";

	public static final String EXPIRATION_DATE = "internal.header.exprirationDate";

	public static final String PRIORITY = "internal.header.priority";

	public static final String SEQUENCE_NUMBER = "internal.header.sequenceNumber";

	public static final String SEQUENCE_SIZE = "internal.header.sequenceSize";


	private final Map<String, Object> headers;


	public MessageHeaders(Map<String, Object> headers) {
		this.headers = (headers != null ? headers
				: new HashMap<String, Object>());
		this.headers.put(ID, UUID.randomUUID());
		this.headers.put(TIMESTAMP, new Long(System.currentTimeMillis()));
	}


	public Object getId() {
		return this.get(ID);
	}

	public Long getTimestamp() {
		return this.get(TIMESTAMP, Long.class);
	}

	public Long getExpirationDate() {
		return this.get(EXPIRATION_DATE, Long.class);
	}

	public Object getCorrelationId() {
		return this.get(CORRELATION_ID);
	}

	public Object getReturnAddress() {
		return this.get(RETURN_ADDRESS);
	}

	public Integer getSequenceNumber() {
		Integer sequenceNumber = this.get(SEQUENCE_NUMBER, Integer.class);
		return (sequenceNumber != null ? sequenceNumber : 0);
	}

	public Integer getSequenceSize() {
		Integer sequenceSize = this.get(SEQUENCE_SIZE, Integer.class);
		return (sequenceSize != null ? sequenceSize : 0);
	}

	public MessagePriority getPriority() {
		return this.get(PRIORITY, MessagePriority.class);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Object key, Class<T> type) {
		Object value = this.headers.get(key);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			throw new IllegalArgumentException(
					"Incorrect type specified for header '" + key
							+ "'. Expected [" + type + "] but actual type is ["
							+ value.getClass() + "]");
		}
		return (T) value;
	}

	public int hashCode() {
		return headers.hashCode();
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof MessageHeaders) {
			MessageHeaders other = (MessageHeaders) obj;
			return this.headers.equals(other.headers);
		}
		return false;
	}

	public String toString() {
		return this.headers.toString();
	}

	/*
	 * Map implementation
	 */

	public void clear() {
		this.headers.clear();
	}

	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	public Set<Map.Entry<String, Object>> entrySet() {
		return Collections.unmodifiableSet(this.headers.entrySet());
	}

	public Object get(Object key) {
		return this.headers.get(key);
	}

	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	public Set<String> keySet() {
		return Collections.unmodifiableSet(this.headers.keySet());
	}

	public int size() {
		return this.headers.size();
	}

	public Collection<Object> values() {
		return Collections.unmodifiableCollection(this.headers.values());
	}

	/*
	 * Unsupported operations
	 */

	public Object put(String key, Object value) {
		throw new UnsupportedOperationException("MessageHeaders is immutable.");
	}

	public void putAll(Map<? extends String, ? extends Object> t) {
		throw new UnsupportedOperationException("MessageHeaders is immutable.");
	}

	public Object remove(Object key) {
		throw new UnsupportedOperationException("MessageHeaders is immutable.");
	}

}
