/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.support.json;

import java.lang.reflect.Type;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * Base {@link JsonInboundMessageMapper.JsonMessageParser} implementation for Jackson processors.
 *
 * @param <P> the payload type.
 *
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
abstract class AbstractJacksonJsonMessageParser<P> implements JsonInboundMessageMapper.JsonMessageParser<P>,
		BeanFactoryAware {

	private final JsonObjectMapper<?, P> objectMapper;

	private volatile JsonInboundMessageMapper messageMapper;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private BeanFactory beanFactory;

	private volatile boolean messageBuilderFactorySet;

	protected AbstractJacksonJsonMessageParser(JsonObjectMapper<?, P> objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (!this.messageBuilderFactorySet) {
			if (this.beanFactory != null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.messageBuilderFactorySet = true;
		}
		return this.messageBuilderFactory;
	}

	@Override
	public Message<?> doInParser(JsonInboundMessageMapper messageMapperToUse, String jsonMessage,
			@Nullable Map<String, Object> headers) {

		if (this.messageMapper == null) {
			this.messageMapper = messageMapperToUse;
		}
		P parser = this.createJsonParser(jsonMessage);

		if (messageMapperToUse.isMapToPayload()) {
			Object payload = readPayload(parser, jsonMessage);
			return getMessageBuilderFactory()
					.withPayload(payload)
					.copyHeaders(headers)
					.build();
		}
		else {
			return parseWithHeaders(parser, jsonMessage, headers);
		}
	}

	protected Object readPayload(P parser, String jsonMessage) {
		try {
			return this.objectMapper.fromJson(parser, this.messageMapper.getPayloadType());
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Mapping of JSON message '" + jsonMessage +
					"' to payload type '" + this.messageMapper.getPayloadType() + "' failed.", e);
		}
	}

	protected Object readHeader(P parser, String headerName, String jsonMessage) {
		Class<?> headerType = this.messageMapper.getHeaderTypes().getOrDefault(headerName, Object.class);
		try {
			return this.objectMapper.fromJson(parser, (Type) headerType);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Mapping header '" + headerName + "' of JSON message '" +
					jsonMessage + "' to header type '" + headerType + "' failed.", e);
		}
	}

	protected abstract Message<?> parseWithHeaders(P parser, String jsonMessage,
			@Nullable Map<String, Object> headers);

	protected abstract P createJsonParser(String jsonMessage);

}
