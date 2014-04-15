/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;

/**
 * Base {@link JsonInboundMessageMapper.JsonMessageParser} implementation for Jackson processors.
 *
 * @author Artem Bilan
 * @since 3.0
 *
 */
abstract class AbstractJacksonJsonMessageParser<P> implements JsonInboundMessageMapper.JsonMessageParser<P>,
		BeanFactoryAware {

	private final JsonObjectMapper<?, P> objectMapper;

	private volatile JsonInboundMessageMapper messageMapper;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	protected AbstractJacksonJsonMessageParser(JsonObjectMapper<?, P> objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(beanFactory);
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		return messageBuilderFactory;
	}

	@Override
	public Message<?> doInParser(JsonInboundMessageMapper messageMapper, String jsonMessage) throws Exception {
		if (this.messageMapper == null) {
			this.messageMapper = messageMapper;
		}
		P parser = this.createJsonParser(jsonMessage);

		if (messageMapper.isMapToPayload()) {
			Object payload = this.readPayload(parser, jsonMessage);
			return this.messageBuilderFactory.withPayload(payload).build();
		}
		else {
			return this.parseWithHeaders(parser, jsonMessage);
		}
	}

	protected Object readPayload(P parser, String jsonMessage) throws Exception {
		try {
			return objectMapper.fromJson(parser, this.messageMapper.getPayloadType());
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Mapping of JSON message '" + jsonMessage +
					"' to payload type '" + messageMapper.getPayloadType() + "' failed.", e);
		}
	}

	protected Object readHeader(P parser, String headerName, String jsonMessage) throws Exception {
		Class<?> headerType = this.messageMapper.getHeaderTypes().containsKey(headerName) ?
				this.messageMapper.getHeaderTypes().get(headerName) : Object.class;
		try {
			return this.objectMapper.fromJson(parser, (Type) headerType);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Mapping header '" + headerName + "' of JSON message '" +
					jsonMessage + "' to header type '" + headerType + "' failed.", e);
		}
	}

	protected abstract Message<?> parseWithHeaders(P parser, String jsonMessage) throws Exception;

	protected abstract P createJsonParser(String jsonMessage) throws Exception;

}
