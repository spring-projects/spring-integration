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
import java.util.Map;

import org.springframework.integration.support.json.JsonInboundMessageMapper.JsonMessageParser;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.integration.mapping.InboundMessageMapper} implementation that maps incoming JSON messages
 * to a {@link Message} with the specified payload type.
 *
 * @author Jeremy Grelle
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.0
 */
public class JsonInboundMessageMapper extends AbstractJsonInboundMessageMapper<JsonMessageParser<?>> {

	private volatile JsonMessageParser<?> messageParser;

	public JsonInboundMessageMapper(Class<?> payloadType, JsonMessageParser<?> messageParser) {
		this((Type) payloadType, messageParser);
	}

	public JsonInboundMessageMapper(Type payloadType, JsonMessageParser<?> messageParser) {
		super(payloadType);
		Assert.notNull(messageParser, "'messageParser' cannot be null");
		this.messageParser = messageParser;
	}

	public boolean isMapToPayload() {
		return mapToPayload;
	}

	public Type getPayloadType() {
		return payloadType;
	}

	public Map<String, Class<?>> getHeaderTypes() {
		return headerTypes;
	}

	public Message<?> toMessage(String jsonMessage) throws Exception {
		return this.messageParser.doInParser(this, jsonMessage);
	}

	@Override
	protected Map<String, Object> readHeaders(JsonMessageParser<?> parser, String jsonMessage) throws Exception {
		//No-op
		return null;
	}

	@Override
	protected Object readPayload(JsonMessageParser<?> parser, String jsonMessage) throws Exception {
		//No-op
		return null;
	}

	public static interface JsonMessageParser<P> {

		Message<?> doInParser(JsonInboundMessageMapper messageMapper, String jsonMessage) throws Exception;

	}

}
