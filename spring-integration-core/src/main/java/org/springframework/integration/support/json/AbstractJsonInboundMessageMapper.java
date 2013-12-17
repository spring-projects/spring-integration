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

package org.springframework.integration.support.json;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.util.Assert;

/**
 * Abstract {@link InboundMessageMapper} implementation that maps incoming JSON messages
 * to a {@link org.springframework.messaging.Message} with the specified payload type.
 *
 * @author Artem Bilan
 * @since 3.0
 *
 * @see JsonInboundMessageMapper
 */
public abstract class AbstractJsonInboundMessageMapper<P> implements InboundMessageMapper<String> {

	protected static final String MESSAGE_FORMAT_ERROR = "JSON message is invalid.  Expected a message in the format of either " +
			"{\"headers\":{...},\"payload\":{...}} or {\"payload\":{...}.\"headers\":{...}} but was ";

	protected static final Map<String, Class<?>> DEFAULT_HEADER_TYPES = new HashMap<String, Class<?>>();

	static {
		DEFAULT_HEADER_TYPES.put(IntegrationMessageHeaderAccessor.PRIORITY, Integer.class);
		DEFAULT_HEADER_TYPES.put(IntegrationMessageHeaderAccessor.EXPIRATION_DATE, Long.class);
		DEFAULT_HEADER_TYPES.put(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, Integer.class);
		DEFAULT_HEADER_TYPES.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Integer.class);
	}

	protected final Type payloadType;

	protected final Map<String, Class<?>> headerTypes = DEFAULT_HEADER_TYPES;

	protected volatile boolean mapToPayload = false;

	public AbstractJsonInboundMessageMapper(Type payloadType) {
		Assert.notNull(payloadType, "payloadType must not be null");
		this.payloadType = payloadType;
	}

	public void setHeaderTypes(Map<String, Class<?>> headerTypes) {
		this.headerTypes.putAll(headerTypes);
	}

	public void setMapToPayload(boolean mapToPayload) {
		this.mapToPayload = mapToPayload;
	}

	protected abstract Object readPayload(P parser, String jsonMessage) throws Exception;

	protected abstract Map<String, Object> readHeaders(P parser, String jsonMessage) throws Exception;

}
