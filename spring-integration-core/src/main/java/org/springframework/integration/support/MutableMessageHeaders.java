/*
 * Copyright 2015-2024 the original author or authors.
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

package org.springframework.integration.support;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;

/**
 * A MessageHeaders that permits direct access to and modification of the
 * header map.
 *
 * @author Stuart Williams
 * @author David Turanski
 * @author Artem Bilan
 * @author Nathan Kurtyka
 *
 * @since 4.2
 */
public class MutableMessageHeaders extends MessageHeaders {

	private static final long serialVersionUID = 3084692953798643018L;

	public MutableMessageHeaders(@Nullable Map<String, Object> headers) {
		super(headers, extractId(headers), extractTimestamp(headers));
	}

	protected MutableMessageHeaders(@Nullable Map<String, Object> headers, @Nullable UUID id,
			@Nullable Long timestamp) {

		super(headers, id, timestamp);
	}

	@Override
	protected Map<String, Object> getRawHeaders() { // NOSONAR - not useless; increases visibility
		return super.getRawHeaders();
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		super.getRawHeaders().putAll(map);
	}

	@Override
	public Object put(String key, Object value) {
		return super.getRawHeaders().put(key, value);
	}

	@Override
	public void clear() {
		super.getRawHeaders().clear();
	}

	@Override
	public Object remove(Object key) {
		return super.getRawHeaders().remove(key);
	}

	@Nullable
	private static UUID extractId(@Nullable Map<String, Object> headers) {
		if (headers != null && headers.containsKey(MessageHeaders.ID)) {
			Object id = headers.get(MessageHeaders.ID);
			if (id instanceof String) {
				return UUID.fromString((String) id);
			}
			else if (id instanceof byte[]) {
				ByteBuffer bb = ByteBuffer.wrap((byte[]) id);
				return new UUID(bb.getLong(), bb.getLong());
			}
			else {
				return (UUID) id;
			}
		}

		return null;
	}

	@Nullable
	private static Long extractTimestamp(@Nullable Map<String, Object> headers) {
		if (headers != null && headers.containsKey(MessageHeaders.TIMESTAMP)) {
			Object timestamp = headers.get(MessageHeaders.TIMESTAMP);
			return (timestamp instanceof String) ? Long.parseLong((String) timestamp) : (Long) timestamp;
		}

		return null;
	}

}
