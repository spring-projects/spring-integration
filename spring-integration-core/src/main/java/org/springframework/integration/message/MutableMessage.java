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

}
