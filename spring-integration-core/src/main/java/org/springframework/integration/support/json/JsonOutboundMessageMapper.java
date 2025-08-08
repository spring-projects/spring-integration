/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.support.json;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link OutboundMessageMapper} implementation the converts a {@link Message} to a JSON
 * string representation.
 * <p>
 * Consider using the {@link EmbeddedJsonHeadersMessageMapper} instead; it provides more
 * flexibility for determining which headers are included.
 *
 * @author Jeremy Grelle
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.0
 */
public class JsonOutboundMessageMapper implements OutboundMessageMapper<String> {

	private volatile boolean shouldExtractPayload = false;

	private volatile JsonObjectMapper<?, ?> jsonObjectMapper;

	public JsonOutboundMessageMapper() {
		this(JsonObjectMapperProvider.newInstance());
	}

	public JsonOutboundMessageMapper(JsonObjectMapper<?, ?> jsonObjectMapper) {
		Assert.notNull(jsonObjectMapper, "jsonObjectMapper must not be null");
		this.jsonObjectMapper = jsonObjectMapper;
	}

	public void setShouldExtractPayload(boolean shouldExtractPayload) {
		this.shouldExtractPayload = shouldExtractPayload;
	}

	@Override
	public String fromMessage(Message<?> message) {
		try {
			return this.jsonObjectMapper.toJson(this.shouldExtractPayload ? message.getPayload() : message);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
