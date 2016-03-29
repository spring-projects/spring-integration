/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.amqp.channel;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.amqp.support.MappingUtils;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public abstract class AbstractAmqpChannel extends AbstractMessageChannel {

	private final AmqpTemplate amqpTemplate;

	private final RabbitTemplate rabbitTemplate;

	private final AmqpHeaderMapper outboundHeaderMapper;

	private final AmqpHeaderMapper inboundHeaderMapper;

	private volatile boolean extractPayload;

	private volatile boolean loggingEnabled = true;

	private MessageDeliveryMode defaultDeliveryMode;

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
	 * @see #setExtractPayload(boolean)
	 * @since 4.3
	 */
	AbstractAmqpChannel(AmqpTemplate amqpTemplate, AmqpHeaderMapper outboundMapper, AmqpHeaderMapper inboundMapper) {
		Assert.notNull(amqpTemplate, "amqpTemplate must not be null");
		this.amqpTemplate = amqpTemplate;
		if (amqpTemplate instanceof RabbitTemplate) {
			this.rabbitTemplate = (RabbitTemplate) amqpTemplate;
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
	 * header and the message property was not set by the {@code MessagePropertiesConverter}.
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
	 * @see #setExtractPayload(boolean)
	 * @since 4.3
	 */
	protected boolean isExtractPayload() {
		return this.extractPayload;
	}

	/**
	 * Subclasses may override this method to return an Exchange name.
	 * By default, Messages will be sent to the no-name Direct Exchange.
	 *
	 * @return The exchange name.
	 */
	protected String getExchangeName() {
		return "";
	}

	/**
	 * Subclasses may override this method to return a routing key.
	 * By default, there will be no routing key (empty string).
	 *
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

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		if (this.extractPayload) {
			this.amqpTemplate.send(getExchangeName(), getRoutingKey(), MappingUtils.mapMessage(message,
					this.rabbitTemplate.getMessageConverter(), this.outboundHeaderMapper, this.defaultDeliveryMode));
		}
		else {
			this.amqpTemplate.convertAndSend(getExchangeName(), getRoutingKey(), message);
		}
		return true;
	}

}
