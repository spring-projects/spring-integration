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

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

/**
 * Base {@link JsonInboundMessageMapper.JsonMessageParser} implementation for Jackson processors.
 *
 * @author Artem Bilan
 * @since 3.0
 *
 * @see Jackson2JsonMessageParser
 * @see JacksonJsonMessageParser
 */
abstract class AbstractJacksonJsonMessageParser<P> implements JsonInboundMessageMapper.JsonMessageParser<P> {

	protected final JsonObjectMapper<P> objectMapper;

	protected volatile JsonInboundMessageMapper messageMapper;

	protected AbstractJacksonJsonMessageParser(JsonObjectMapper<P> objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Message<?> doInParser(JsonInboundMessageMapper messageMapper, String jsonMessage) throws Exception {
		if (this.messageMapper == null) {
			this.messageMapper = messageMapper;
		}
		P parser = this.createJsonParser(jsonMessage);
		if (messageMapper.isMapToPayload()) {
			try {
				return MessageBuilder.withPayload(this.readPayload(parser)).build();
			}
			catch (Exception ex) {
					throw new IllegalArgumentException("Mapping of JSON message " + jsonMessage +
							" directly to payload of type " + messageMapper.getPayloadType() + " failed.", ex);
			}
		}
		else {
			return this.parseWithHeaders(parser, jsonMessage);
		}
	}

	protected Object readPayload(P parser) throws Exception {
		return objectMapper.fromJson(parser, this.messageMapper.getPayloadType());
	}

	protected abstract Message<?> parseWithHeaders(P parser, String jsonMessage) throws Exception;

	protected abstract P createJsonParser(String jsonMessage) throws Exception;

}
