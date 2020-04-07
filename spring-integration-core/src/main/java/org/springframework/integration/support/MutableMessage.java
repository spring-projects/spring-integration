/*
 * Copyright 2014-2020 the original author or authors.
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

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * An implementation of {@link Message} with a generic payload. Unlike
 * {@link org.springframework.messaging.support.GenericMessage},
 * this message (or its headers) can be modified after creation.
 * Great care must be taken, when mutating messages, that some other element/thread is not
 * concurrently using the message. Also note that any in-memory stores (such as
 * {@link org.springframework.integration.store.SimpleMessageStore})
 * may have a reference to the message and changes will be
 * reflected there too.
 *
 * <p>
 * <b>IMPORTANT: Mutable messages may share state (such as message headers); such messages
 * should never be exposed to other components or undesirable side-effects may result.</b>
 * <p>
 * <b>It is generally recommended that messages transferred between components should
 * always be immutable unless great care is taken with their use.</b>
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Stuart Williams
 * @author David Turanski
 *
 * @since 4.0
 *
 */
public class MutableMessage<T> implements Message<T>, Serializable {

	private static final long serialVersionUID = -636635024258737500L;

	private final T payload;

	private final MutableMessageHeaders headers;

	public MutableMessage(T payload) {
		this(payload, (Map<String, Object>) null);
	}

	public MutableMessage(T payload, @Nullable Map<String, Object> headers) {
		this(payload, new MutableMessageHeaders(headers));
	}

	protected MutableMessage(T payload, MutableMessageHeaders headers) {
		Assert.notNull(payload, "payload must not be null");
		Assert.notNull(headers, "headers must not be null");
		this.payload = payload;
		this.headers = headers;
	}

	@Override
	public MutableMessageHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public T getPayload() {
		return this.payload;
	}

	Map<String, Object> getRawHeaders() {
		return this.headers.getRawHeaders();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(" [payload=");
		if (this.payload instanceof byte[]) {
			sb.append("byte[").append(((byte[]) this.payload).length).append("]");
		}
		else {
			sb.append(this.payload);
		}
		sb.append(", headers=").append(this.headers).append("]");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode() * 23 + ObjectUtils.nullSafeHashCode(this.payload); // NOSONAR
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof MutableMessage<?>) {
			MutableMessage<?> other = (MutableMessage<?>) obj;
			UUID thisId = this.headers.getId();
			UUID otherId = other.headers.getId();
			return (ObjectUtils.nullSafeEquals(thisId, otherId) &&
					this.headers.equals(other.headers) && this.payload.equals(other.payload));
		}
		return false;
	}

}
