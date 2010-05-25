/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.jms;

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;

/**
 * A {@link MessageConverter} implementation that is capable of delegating to
 * an existing converter instance and an existing {@link JmsHeaderMapper}. The
 * default MessageConverter implementation is {@link SimpleMessageConverter},
 * and the default header mapper implementation is {@link DefaultJmsHeaderMapper}.
 * 
 * <p>If 'extractJmsMessageBody' is <code>true</code> (the default), the body
 * of each received JMS Message will become the payload of a Spring Integration
 * Message. Otherwise, the JMS Message itself will be the payload of the Spring
 * Integration Message.
 * 
 * <p>If 'extractIntegrationMessagePayload' is <code>true</code> (the default),
 * the payload of each outbound Spring Integration Message will be passed to
 * the MessageConverter to produce the body of the JMS Message. Otherwise, the
 * Spring Integration Message itself will become the body of the JMS Message.
 * 
 * <p>The {@link JmsHeaderMapper} will be applied regardless of the values
 * specified for Message extraction.
 * 
 * @author Mark Fisher
 */
public class HeaderMappingMessageConverter implements MessageConverter {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final MessageConverter converter;

	private final JmsHeaderMapper headerMapper;

	private volatile boolean extractJmsMessageBody = true;

	private volatile boolean extractIntegrationMessagePayload = true;


	/**
	 * Create a HeaderMappingMessageConverter instance that will rely on the
	 * default {@link SimpleMessageConverter} and {@link DefaultJmsHeaderMapper}.
	 */
	public HeaderMappingMessageConverter() {
		this(null, null);
	}

	/**
	 * Create a HeaderMappingMessageConverter instance that will delegate to
	 * the provided {@link MessageConverter} instance and will use the default
	 * implementation of the {@link JmsHeaderMapper} strategy.
	 */
	public HeaderMappingMessageConverter(MessageConverter converter) {
		this(converter, null);
	}

	/**
	 * Create a HeaderMappingMessageConverter instance that will delegate to
	 * the provided {@link JmsHeaderMapper} instance and will use the default
	 * {@link SimpleMessageConverter} implementation.
	 */
	public HeaderMappingMessageConverter(JmsHeaderMapper headerMapper) {
		this(null, headerMapper);
	}

	/**
	 * Create a HeaderMappingMessageConverter instance that will delegate to
	 * the provided {@link MessageConverter} and {@link JmsHeaderMapper}.
	 */
	public HeaderMappingMessageConverter(MessageConverter converter, JmsHeaderMapper headerMapper) {
		this.converter = (converter != null ?  converter : new SimpleMessageConverter());
		this.headerMapper = (headerMapper != null ? headerMapper : new DefaultJmsHeaderMapper());
	}

	/**
	 * Specify whether the inbound JMS Message's body should be extracted
	 * during the conversion process. Otherwise, the raw JMS Message itself
	 * will be the payload of the created Spring Integration Message. The
	 * HeaderMapper will be applied to the Message regardless of this value.
	 * 
	 * <p>The default value is <code>true</code>.
	 */
	public void setExtractJmsMessageBody(boolean extractJmsMessageBody) {
		this.extractJmsMessageBody = extractJmsMessageBody;
	}

	/**
	 * Specify whether the outbound integration Message's payload should be
	 * extracted prior to conversion into a JMS Message. Otherwise, the
	 * integration Message itself will be passed to the converter.
	 * 
	 * <p>Typically, this setting should be determined by the expectations of
	 * the target system. If the target system is not capable of understanding
	 * a Spring Integration Message, then set this to <code>true</code>.
	 * On the other hand, if the system is not only capable of understanding a
	 * Spring Integration Message but actually expected to rely upon Spring
	 * Integration Message Header values, then this must be set to
	 * <code>false</code> to ensure that the actual Message will be passed
	 * along with its Serializable headers.
	 * 
	 * <p>The default value is <code>true</code>.
	 */
	public void setExtractIntegrationMessagePayload(boolean extractIntegrationMessagePayload) {
		this.extractIntegrationMessagePayload = extractIntegrationMessagePayload;
	}

	/**
	 * Converts from a JMS {@link javax.jms.Message} to an integration Message.
	 */
	public Object fromMessage(javax.jms.Message jmsMessage) throws JMSException, MessageConversionException {
		MessageBuilder<?> builder = null;
		if (this.extractJmsMessageBody) {
			Object conversionResult = this.converter.fromMessage(jmsMessage);
			if (conversionResult == null) {
				return null;
			}
			if (conversionResult instanceof Message<?>) {
				builder = MessageBuilder.fromMessage((Message<?>) conversionResult);
			}
			else {
				builder = MessageBuilder.withPayload(conversionResult);
			}
		}
		else {
			builder = MessageBuilder.withPayload(jmsMessage);
		}
		Map<String, Object> headers = this.headerMapper.toHeaders(jmsMessage);
		Message<?> message = builder.copyHeadersIfAbsent(headers).build();
		if (logger.isDebugEnabled()) {
			logger.debug("converted JMS Message [" + jmsMessage + "] to integration Message [" + message + "]");
		}
		return message;
	}

	/**
	 * Converts from an integration Message to a JMS {@link javax.jms.Message}.
	 */
	public javax.jms.Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		MessageHeaders headers = null;
		javax.jms.Message jmsMessage = null;
		if (object instanceof Message<?>) {
			headers = ((Message<?>) object).getHeaders();
			if (this.extractIntegrationMessagePayload) {
				object = ((Message<?>) object).getPayload();
			}
		}
		jmsMessage = this.converter.toMessage(object, session);
		if (headers != null) {
			this.headerMapper.fromHeaders(headers, jmsMessage);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("converted [" + object + "] to JMS Message [" + jmsMessage + "]");
		}
		return jmsMessage;
	}

}
