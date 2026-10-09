/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.support.json;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link JsonInboundMessageMapper.JsonMessageParser} implementation that parses JSON messages
 * and builds a {@link Message} with the specified payload type from provided {@link JsonInboundMessageMapper}.
 * Uses <a href="https://github.com/FasterXML">Jackson JSON Processor</a>.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 */
public class JacksonJsonMessageParser extends AbstractJacksonJsonMessageParser<JsonParser> {

	private static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

	public JacksonJsonMessageParser() {
		this(new JacksonJsonObjectMapper());
	}

	public JacksonJsonMessageParser(JacksonJsonObjectMapper objectMapper) {
		super(objectMapper);
	}

	@Override
	protected JsonParser createJsonParser(String jsonMessage) {
		return JSON_FACTORY.createParser(ObjectReadContext.empty(), jsonMessage);
	}

	@Override
	protected Message<?> parseWithHeaders(JsonParser parser, String jsonMessage,
			@Nullable Map<String, Object> headersToAdd) {

		String error = AbstractJsonInboundMessageMapper.MESSAGE_FORMAT_ERROR + jsonMessage;
		Assert.isTrue(JsonToken.START_OBJECT == parser.nextToken(), error);
		Map<String, Object> headers = null;
		Object payload = null;
		while (JsonToken.END_OBJECT != parser.nextToken()) {
			Assert.isTrue(JsonToken.PROPERTY_NAME == parser.currentToken(), error);
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
		Assert.notNull(payload, "Payload must not be null");
		return getMessageBuilderFactory()
				.withPayload(payload)
				.copyHeaders(headers)
				.copyHeadersIfAbsent(headersToAdd)
				.build();
	}

	private Map<String, Object> readHeaders(JsonParser parser, String jsonMessage) throws JacksonException {
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
