/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
