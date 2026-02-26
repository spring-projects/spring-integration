/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.jms.channel;

import jakarta.jms.JMSException;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.jms.DynamicJmsTemplateProperties;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessagingMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A base {@link AbstractMessageChannel} implementation for JMS-backed message channels.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 7.0
 *
 * @see PollableJmsChannel
 * @see SubscribableJmsChannel
 */
public abstract class AbstractJmsChannel extends AbstractMessageChannel {

	protected final DefaultJmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();

	protected final JmsTemplate jmsTemplate;

	public AbstractJmsChannel(JmsTemplate jmsTemplate) {
		Assert.notNull(jmsTemplate, "jmsTemplate must not be null");
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		try {
			DynamicJmsTemplateProperties.setPriority(new IntegrationMessageHeaderAccessor(message).getPriority());
			MessageConverter messageConverter = this.jmsTemplate.getMessageConverter();
			this.jmsTemplate.send((session) -> {
				jakarta.jms.Message jmsMessage = messageConverter.toMessage(message, session);
				if (!(messageConverter instanceof MessagingMessageConverter)) {
					MessageHeaders headers = message.getHeaders();
					this.headerMapper.fromHeaders(headers, jmsMessage);
				}
				return jmsMessage;
			});
		}
		finally {
			DynamicJmsTemplateProperties.clearPriority();
		}
		return true;
	}

	protected Message<?> fromJmsMessage(jakarta.jms.Message message) {
		MessageConverter converter = this.jmsTemplate.getMessageConverter();
		try {
			Object converted = converter.fromMessage(message);
			Message<?> messageToSend;
			if (converted instanceof Message<?> convertedMessage) {
				messageToSend = convertedMessage;
				if (!(converter instanceof MessagingMessageConverter)) {
					messageToSend = getMessageBuilderFactory()
							.fromMessage(messageToSend)
							.copyHeadersIfAbsent(this.headerMapper.toHeaders(message))
							.build();
				}
			}
			else {
				messageToSend = getMessageBuilderFactory()
						.withPayload(converted)
						.copyHeaders(this.headerMapper.toHeaders(message))
						.build();
			}

			return messageToSend;
		}
		catch (JMSException ex) {
			throw new MessagingException("failed to convert incoming JMS Message", ex);
		}

	}

}
