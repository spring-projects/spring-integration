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

package org.springframework.integration.support.converter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Converts to/from a Map with 2 keys ('headers' and 'payload').
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class MapMessageConverter implements MessageConverter, BeanFactoryAware {

	private String[] headerNames = {};

	private boolean filterHeadersInToMessage;

	private BeanFactory beanFactory;

	private MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private boolean messageBuilderFactorySet;

	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
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

	/**
	 * Headers to be converted in {@link #fromMessage(Message, Class)}.
	 * {@link #toMessage(Object, MessageHeaders)} will populate all headers found in
	 * the map, unless {@link #filterHeadersInToMessage} is true.
	 *
	 * @param headerNames The header names.
	 */
	public void setHeaderNames(String... headerNames) {
		if (ObjectUtils.isEmpty(headerNames)) {
			this.headerNames = new String[] {};
		}
		else {
			this.headerNames = Arrays.copyOf(headerNames, headerNames.length);
		}
	}

	/**
	 * By default, all headers on Map passed to {@link #toMessage(Object, MessageHeaders)}
	 * will be mapped. Set this property
	 * to 'true' if you wish to limit the inbound headers to those
	 * in the {@link #headerNames}.
	 * @param filterHeadersInToMessage true if the headers should be filtered.
	 */
	public void setFilterHeadersInToMessage(boolean filterHeadersInToMessage) {
		this.filterHeadersInToMessage = filterHeadersInToMessage;
	}

	@Nullable
	@Override
	public Message<?> toMessage(Object object, @Nullable MessageHeaders messageHeaders) {
		Assert.isInstanceOf(Map.class, object, "This converter expects a Map");
		@SuppressWarnings("unchecked")
		Map<String, ?> map = (Map<String, ?>) object;
		Object payload = map.get("payload");
		Assert.notNull(payload, "'payload' entry cannot be null");
		AbstractIntegrationMessageBuilder<?> messageBuilder = getMessageBuilderFactory().withPayload(payload);
		@SuppressWarnings("unchecked")
		Map<String, ?> headers = (Map<String, ?>) map.get("headers");
		if (headers != null) {
			if (this.filterHeadersInToMessage) {
				headers.keySet().retainAll(Arrays.asList(this.headerNames));
			}
			messageBuilder.copyHeaders(headers);
		}
		return messageBuilder
				.copyHeadersIfAbsent(messageHeaders)
				.build();
	}

	@Nullable
	@Override
	public Object fromMessage(Message<?> message, Class<?> clazz) {
		Map<String, Object> map = new HashMap<>();
		map.put("payload", message.getPayload());
		Map<String, Object> headers = new HashMap<>();
		for (String headerName : this.headerNames) {
			Object header = message.getHeaders().get(headerName);
			if (header != null) {
				headers.put(headerName, header);
			}
		}
		map.put("headers", headers);
		return map;
	}

}
