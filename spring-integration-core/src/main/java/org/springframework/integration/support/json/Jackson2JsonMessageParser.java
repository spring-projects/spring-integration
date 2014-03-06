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


import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * {@link JsonInboundMessageMapper.JsonMessageParser} implementation that parses JSON messages
 * and builds a {@link Message} with the specified payload type from provided {@link JsonInboundMessageMapper}.
 * Uses Jackson 2 JSON-processor (@link https://github.com/FasterXML).
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public class Jackson2JsonMessageParser extends AbstractJacksonJsonMessageParser<JsonParser> {

	public Jackson2JsonMessageParser() {
		super(new Jackson2JsonObjectMapper());
	}

	@Override
	protected JsonParser createJsonParser(String jsonMessage) throws Exception {
		return new JsonFactory().createParser(jsonMessage);
	}

	@Override
	protected Message<?> parseWithHeaders(JsonParser parser, String jsonMessage) throws Exception {
		String error = AbstractJsonInboundMessageMapper.MESSAGE_FORMAT_ERROR + jsonMessage;
		Assert.isTrue(JsonToken.START_OBJECT == parser.nextToken(), error);
		Map<String, Object> headers = null;
		Object payload = null;
		while (JsonToken.END_OBJECT != parser.nextToken()) {
			Assert.isTrue(JsonToken.FIELD_NAME == parser.getCurrentToken(), error);
			boolean isHeadersToken = "headers".equals(parser.getCurrentName());
			boolean isPayloadToken = "payload".equals(parser.getCurrentName());
			Assert.isTrue(isHeadersToken || isPayloadToken, error);
			if (isHeadersToken) {
				Assert.isTrue(parser.nextToken() == JsonToken.START_OBJECT, error);
				headers = readHeaders(parser, jsonMessage);
			}
			else if (isPayloadToken) {
				parser.nextToken();
				payload = this.readPayload(parser, jsonMessage);
			}
		}
		Assert.notNull(headers, error);
		return this.getMessageBuilderFactory().withPayload(payload).copyHeaders(headers).build();
	}

	private Map<String, Object> readHeaders(JsonParser parser, String jsonMessage) throws Exception {
		Map<String, Object> headers = new LinkedHashMap<String, Object>();
		while (JsonToken.END_OBJECT != parser.nextToken()) {
			String headerName = parser.getCurrentName();
			parser.nextToken();
			Object headerValue = this.readHeader(parser, headerName, jsonMessage);
			headers.put(headerName, headerValue);
		}
		return headers;
	}

}
