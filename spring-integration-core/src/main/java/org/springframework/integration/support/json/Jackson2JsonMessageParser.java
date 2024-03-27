/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.support.json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link JsonInboundMessageMapper.JsonMessageParser} implementation that parses JSON messages
 * and builds a {@link Message} with the specified payload type from provided {@link JsonInboundMessageMapper}.
 * Uses <a href="https://github.com/FasterXML">Jackson JSON Processor</a>.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
public class Jackson2JsonMessageParser extends AbstractJacksonJsonMessageParser<JsonParser> {

	public Jackson2JsonMessageParser() {
		this(new Jackson2JsonObjectMapper());
	}

	public Jackson2JsonMessageParser(Jackson2JsonObjectMapper objectMapper) {
		super(objectMapper);
	}

	@Override
	protected JsonParser createJsonParser(String jsonMessage) {
		try {
			return new JsonFactory().createParser(jsonMessage);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	protected Message<?> parseWithHeaders(JsonParser parser, String jsonMessage,
			@Nullable Map<String, Object> headersToAdd) {

		try {
			String error = AbstractJsonInboundMessageMapper.MESSAGE_FORMAT_ERROR + jsonMessage;
			Assert.isTrue(JsonToken.START_OBJECT == parser.nextToken(), error);
			Map<String, Object> headers = null;
			Object payload = null;
			while (JsonToken.END_OBJECT != parser.nextToken()) {
				Assert.isTrue(JsonToken.FIELD_NAME == parser.getCurrentToken(), error);
				String currentName = parser.currentName();
				boolean isHeadersToken = "headers".equals(currentName);
				boolean isPayloadToken = "payload".equals(currentName);
				Assert.isTrue(isHeadersToken || isPayloadToken, error);
				if (isHeadersToken) {
					Assert.isTrue(parser.nextToken() == JsonToken.START_OBJECT, error);
					headers = readHeaders(parser, jsonMessage);
				}
				else {
					parser.nextToken();
					payload = readPayload(parser, jsonMessage);
				}
			}
			Assert.notNull(headers, error);

			return getMessageBuilderFactory()
					.withPayload(payload)
					.copyHeaders(headers)
					.copyHeadersIfAbsent(headersToAdd)
					.build();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Map<String, Object> readHeaders(JsonParser parser, String jsonMessage) throws IOException {
		Map<String, Object> headers = new LinkedHashMap<>();
		while (JsonToken.END_OBJECT != parser.nextToken()) {
			String headerName = parser.currentName();
			parser.nextToken();
			Object headerValue = readHeader(parser, headerName, jsonMessage);
			headers.put(headerName, headerValue);
		}
		return headers;
	}

}
