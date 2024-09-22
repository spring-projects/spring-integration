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

package org.springframework.integration.amqp.channel;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AllowedListDeserializingMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.amqp.support.MappingUtils;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * The base {@link AbstractMessageChannel} implementation for AMQP.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @author Ngoc Nhan
 *
 * @since 2.1
 */
public abstract class AbstractAmqpChannel extends AbstractMessageChannel implements ConnectionListener {

	private final AmqpTemplate amqpTemplate;

	private final RabbitTemplate rabbitTemplate;

	private final AmqpHeaderMapper outboundHeaderMapper;

	private final AmqpHeaderMapper inboundHeaderMapper;

	private AmqpAdmin admin;

	private ConnectionFactory connectionFactory;

	private boolean extractPayload;

	private boolean loggingEnabled = true;

	private MessageDeliveryMode defaultDeliveryMode;

	private boolean headersMappedLast;

	private volatile boolean initialized;

	/**
	 * Construct an instance with the supplied template and default header mappers
	 * used if the template is a {@link RabbitTemplate} and the message is mapped.
	 * @param amqpTemplate the template.
	 * @see #setExtractPayload(boolean)
	 */
	AbstractAmqpChannel(AmqpTemplate amqpTemplate) {
		this(amqpTemplate, DefaultAmqpHeaderMapper.outboundMapper(), DefaultAmqpHeaderMapper.inboundMapper());
	}

	/**
	 * Construct an instance with the supplied template and header mappers, used
	 * when the message is mapped.
	 * @param amqpTemplate the template.
	 * @param outboundMapper the outbound mapper.
	 * @param inboundMapper the inbound mapper.
	 * @since 4.3
	 * @see #setExtractPayload(boolean)
	 */
	AbstractAmqpChannel(AmqpTemplate amqpTemplate, AmqpHeaderMapper outboundMapper, AmqpHeaderMapper inboundMapper) {
		Assert.notNull(amqpTemplate, "amqpTemplate must not be null");
		this.amqpTemplate = amqpTemplate;
		if (amqpTemplate instanceof RabbitTemplate castRabbitTemplate) {
			this.rabbitTemplate = castRabbitTemplate;
			MessageConverter converter = this.rabbitTemplate.getMessageConverter();
			if (converter instanceof AllowedListDeserializingMessageConverter allowedListMessageConverter) {
				allowedListMessageConverter.addAllowedListPatterns(
						"java.util*",
						"java.lang*",
						GenericMessage.class.getName(),
						ErrorMessage.class.getName(),
						AdviceMessage.class.getName(),
						MutableMessage.class.getName(),
						MessageHeaders.class.getName(),
						MutableMessageHeaders.class.getName(),
						MessageHistory.class.getName());
			}
		}
		else {
			this.rabbitTemplate = null;
		}
		this.outboundHeaderMapper = outboundMapper;
		this.inboundHeaderMapper = inboundMapper;
	}

	@Override
	public boolean isLoggingEnabled() {
		return this.loggingEnabled;
	}

	@Override
	public void setLoggingEnabled(boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
	}

	/**
	 * Set the delivery mode to use if the message has no
	 * {@value org.springframework.amqp.support.AmqpHeaders#DELIVERY_MODE}
	 * header and the message property was not set by the
	 * {@code MessagePropertiesConverter}.
	 * @param defaultDeliveryMode the default delivery mode.
	 * @since 4.3
	 */
	public void setDefaultDeliveryMode(MessageDeliveryMode defaultDeliveryMode) {
		this.defaultDeliveryMode = defaultDeliveryMode;
	}

	/**
	 * Set to true to extract the payload and map the headers; otherwise
	 * the entire message is converted and sent. Default false.
	 * @param extractPayload true to extract and map.
	 * @since 4.3
	 */
	public void setExtractPayload(boolean extractPayload) {
		if (extractPayload) {
			Assert.isTrue(this.rabbitTemplate != null, "amqpTemplate must be a RabbitTemplate for 'extractPayload'");
			Assert.state(this.outboundHeaderMapper != null && this.inboundHeaderMapper != null,
					"'extractPayload' requires both inbound and outbound header mappers");
		}
		this.extractPayload = extractPayload;
	}

	/**
	 * @return the extract payload.
	 * @since 4.3
	 * @see #setExtractPayload(boolean)
	 */
	protected boolean isExtractPayload() {
		return this.extractPayload;
	}

	/**
	 * When mapping headers for the outbound message, determine whether the headers are
	 * mapped before the message is converted, or afterward. This only affects headers
	 * that might be added by the message converter. When false, the converter's headers
	 * win; when true, any headers added by the converter will be overridden (if the
	 * source message has a header that maps to those headers). You might wish to set this
	 * to true, for example, when using a
	 * {@link org.springframework.amqp.support.converter.SimpleMessageConverter} with a
	 * String payload that contains json; the converter will set the content type to
	 * {@code text/plain} which can be overridden to {@code application/json} by setting
	 * the {@link org.springframework.amqp.support.AmqpHeaders#CONTENT_TYPE} message header.
	 * Only applies when {@link #setExtractPayload(boolean) extractPayload} is true.
	 * Default: false.
	 * @param headersMappedLast true if headers are mapped after conversion.
	 * @since 5.0
	 */
	public void setHeadersMappedLast(boolean headersMappedLast) {
		this.headersMappedLast = headersMappedLast;
	}

	/**
	 * Subclasses may override this method to return an Exchange name.
	 * By default, Messages will be sent to the no-name Direct Exchange.
	 * @return The exchange name.
	 */
	protected String getExchangeName() {
		return "";
	}

	/**
	 * Subclasses may override this method to return a routing key.
	 * By default, there will be no routing key (empty string).
	 * @return The routing key.
	 */
	protected String getRoutingKey() {
		return "";
	}

	protected AmqpHeaderMapper getInboundHeaderMapper() {
		return this.inboundHeaderMapper;
	}

	protected AmqpTemplate getAmqpTemplate() {
		return this.amqpTemplate;
	}

	protected RabbitTemplate getRabbitTemplate() {
		return this.rabbitTemplate;
	}

	protected final void setAdmin(AmqpAdmin admin) {
		this.admin = admin;
	}

	protected final void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	protected AmqpAdmin getAdmin() {
		return this.admin;
	}

	protected ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (!this.initialized && this.rabbitTemplate != null && this.connectionFactory != null) {
			this.connectionFactory.addConnectionListener(this);
		}
		this.initialized = true;
	}

	@Override
	public void destroy() {
		if (this.connectionFactory != null) {
			this.connectionFactory.removeConnectionListener(this);
			this.initialized = false;
		}
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		if (this.extractPayload) {
			this.amqpTemplate.send(getExchangeName(), getRoutingKey(), MappingUtils.mapMessage(message,
					this.rabbitTemplate.getMessageConverter(), this.outboundHeaderMapper, this.defaultDeliveryMode,
					this.headersMappedLast));
		}
		else {
			this.amqpTemplate.convertAndSend(getExchangeName(), getRoutingKey(), message);
		}
		return true;
	}

	@Override
	public void onCreate(Connection connection) {
		doDeclares();
	}

	protected abstract void doDeclares();

}
