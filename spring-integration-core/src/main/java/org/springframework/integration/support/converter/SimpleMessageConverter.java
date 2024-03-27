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

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class SimpleMessageConverter implements MessageConverter, BeanFactoryAware {

	private InboundMessageMapper inboundMessageMapper = new DefaultInboundMessageMapper();

	private OutboundMessageMapper outboundMessageMapper = new DefaultOutboundMessageMapper();

	private MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private boolean messageBuilderFactorySet;

	private BeanFactory beanFactory;

	public SimpleMessageConverter() {
	}

	public SimpleMessageConverter(InboundMessageMapper<?> inboundMessageMapper) {
		setInboundMessageMapper(inboundMessageMapper);
		if (inboundMessageMapper instanceof OutboundMessageMapper<?> messageMapper) {
			setOutboundMessageMapper(messageMapper);
		}
	}

	public SimpleMessageConverter(OutboundMessageMapper<?> outboundMessageMapper) {
		if (outboundMessageMapper instanceof InboundMessageMapper<?> messageMapper) {
			setInboundMessageMapper(messageMapper);
		}
		setOutboundMessageMapper(outboundMessageMapper);
	}

	public SimpleMessageConverter(InboundMessageMapper<?> inboundMessageMapper,
			OutboundMessageMapper<?> outboundMessageMapper) {

		setInboundMessageMapper(inboundMessageMapper);
		setOutboundMessageMapper(outboundMessageMapper);
	}

	public final void setInboundMessageMapper(@Nullable InboundMessageMapper<?> inboundMessageMapper) {
		if (inboundMessageMapper != null) {
			this.inboundMessageMapper = inboundMessageMapper;
		}
	}

	public final void setOutboundMessageMapper(@Nullable OutboundMessageMapper<?> outboundMessageMapper) {
		if (outboundMessageMapper != null) {
			this.outboundMessageMapper = outboundMessageMapper;
		}
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

	@Nullable
	@Override
	public Message<?> toMessage(Object object, @Nullable MessageHeaders headers) {
		try {
			return this.inboundMessageMapper.toMessage(object, headers);
		}
		catch (Exception ex) {
			throw new MessageConversionException("failed to convert object to Message", ex);
		}
	}

	@Nullable
	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		try {
			return this.outboundMessageMapper.fromMessage(message);
		}
		catch (Exception ex) {
			throw new MessageConversionException(message, "failed to convert Message to object", ex);
		}
	}

	private class DefaultInboundMessageMapper implements InboundMessageMapper<Object> {

		DefaultInboundMessageMapper() {
		}

		@Override
		public Message<?> toMessage(@Nullable Object object, @Nullable Map<String, Object> headers) {
			if (object == null) {
				return null;
			}
			if (object instanceof Message<?>) {
				return (Message<?>) object;
			}
			return getMessageBuilderFactory()
					.withPayload(object)
					.copyHeadersIfAbsent(headers)
					.build();
		}

	}

	private static class DefaultOutboundMessageMapper implements OutboundMessageMapper<Object> {

		DefaultOutboundMessageMapper() {
		}

		@Override
		public Object fromMessage(@Nullable Message<?> message) {
			return (message != null) ? message.getPayload() : null;
		}

	}

}
