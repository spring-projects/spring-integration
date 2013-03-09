/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.support.converter;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Converts to/from a Map with 2 keys ('headers' and 'payload').
 * @author Gary Russell
 * @since 3.0
 *
 */
public class MapMessageConverter implements MessageConverter {

	private volatile String[] headerNames;

	private volatile boolean filterHeadersInToMessage;

	/**
	 * Headers to be converted in {@link #fromMessage(Message)}.
	 * {@link #toMessage(Object)} will populate all headers found in
	 * the map, unless {@link #filterHeadersInToMessage} is true.
	 * @param headerNames
	 */
	public void setHeaderNames(String... headerNames) {
		this.headerNames = headerNames;
	}

	/**
	 * By default all headers on Map passed to {@link #toMessage(Object)}
	 * will be mapped. Set this property
	 * to 'true' if you wish to limit the inbound headers to those in
	 * the #headerNames.
	 * @param filterHeadersInToMessage
	 */
	public void setFilterHeadersInToMessage(boolean filterHeadersInToMessage) {
		this.filterHeadersInToMessage = filterHeadersInToMessage;
	}

	public <P> Message<P> toMessage(Object object) {
		Assert.isInstanceOf(Map.class, object, "This converter expects a Map");
		@SuppressWarnings("unchecked")
		Map<String, ?> map = (Map<String, ?>) object;
		Object payload = map.get("payload");
		Assert.notNull(payload, "'payload' entry cannot be null");
		MessageBuilder<?> messageBuilder = MessageBuilder.withPayload(payload);
		@SuppressWarnings("unchecked")
		Map<String, ?> headers = (Map<String, ?>) map.get("headers");
		if (headers != null) {
			if (this.filterHeadersInToMessage) {
				headers.keySet().retainAll(Arrays.asList(this.headerNames));
			}
			messageBuilder.copyHeaders(headers);
			/*for (Entry<String, ?> entry : headers.entrySet()) {
				if (this.filterHeadersInToMessage ? this.headerNames.contains(entry.getKey()) : true) {
					messageBuilder.setHeader(entry.getKey(), entry.getValue());
				}
			}*/
		}
		@SuppressWarnings("unchecked")
		Message<P> convertedMessage = (Message<P>) messageBuilder.build();
		return convertedMessage;
	}

	public <P> Object fromMessage(Message<P> message) {
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("payload", message.getPayload());
		Map<String, Object> headers = new HashMap<String, Object>();
		for (String headerName : headerNames) {
			Object header = message.getHeaders().get(headerName);
			if (header != null) {
				headers.put(headerName, header);
			}
		}
		map.put("headers", headers);
		return map;
	}

}
