/*
 * Copyright 2014 the original author or authors.
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

import java.util.Map;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.ObjectUtils;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
public class MutableMessage<T> implements Message<T> {

	private T payload;

	private final MessageHeaders headers;

	public MutableMessage(T payload) {
		this.payload = payload;
		this.headers = new MessageHeaders(null);
	}

	@Override
	public MessageHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public T getPayload() {
		return this.payload;
	}

	public void setPayload(T payload) {
		this.payload = payload;
	}

	public Map<String, Object> getRawHeaders() {
		@SuppressWarnings("unchecked")
		Map<String, Object> headers = (Map<String, Object>) new DirectFieldAccessor(this.headers)
				.getPropertyValue("headers");
		return headers;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.payload instanceof byte[]) {
			sb.append("[Payload byte[").append(((byte[]) this.payload).length).append("]]");
		}
		else {
			sb.append("[Payload ").append(this.payload.getClass().getSimpleName());
			sb.append(" content=").append(this.payload).append("]");
		}
		sb.append("[Headers=").append(this.headers).append("]");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode() * 23 + ObjectUtils.nullSafeHashCode(this.payload);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof MutableMessage<?>) {
			MutableMessage<?> other = (MutableMessage<?>) obj;
			return (this.headers.getId().equals(other.headers.getId()) &&
					this.headers.equals(other.headers) && this.payload.equals(other.payload));
		}
		return false;
	}

}
