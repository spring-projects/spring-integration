/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.amqp.inbound;

import java.util.Map;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;

/**
 * Adapter that receives Messages from an AMQP Queue, converts them into
 * Spring Integration Messages, and sends the results to a Message Channel.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 */
public class AmqpInboundChannelAdapter extends MessageProducerSupport implements
		OrderlyShutdownCapable {

	private final AbstractMessageListenerContainer messageListenerContainer;

	private volatile MessageConverter messageConverter = new SimpleMessageConverter();

	private volatile AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();


	public AmqpInboundChannelAdapter(AbstractMessageListenerContainer listenerContainer) {
		Assert.notNull(listenerContainer, "listenerContainer must not be null");
		Assert.isNull(listenerContainer.getMessageListener(),
				"The listenerContainer provided to an AMQP inbound Channel Adapter " +
						"must not have a MessageListener configured since the adapter " +
						"configure its own listener implementation.");
		this.messageListenerContainer = listenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
	}


	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "messageConverter must not be null");
		this.messageConverter = messageConverter;
	}

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	@Override
	public String getComponentType() {
		return "amqp:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		this.messageListenerContainer.setMessageListener((ChannelAwareMessageListener) (message, channel) -> {
			boolean error = false;
			Map<String, Object> headers = null;
			Object payload = null;
			try {
				payload = this.messageConverter.fromMessage(message);
				headers = this.headerMapper.toHeadersFromRequest(message.getMessageProperties());
				if (this.messageListenerContainer.getAcknowledgeMode()
						== AcknowledgeMode.MANUAL) {
					headers.put(AmqpHeaders.DELIVERY_TAG, message.getMessageProperties().getDeliveryTag());
					headers.put(AmqpHeaders.CHANNEL, channel);
				}
			}
			catch (RuntimeException e) {
				if (getErrorChannel() != null) {
					getMessagingTemplate().send(getErrorChannel(), new ErrorMessage(
							new ListenerExecutionFailedException("Message conversion failed", e, message)));
				}
				else {
					throw e;
				}
				error = true;
			}
			if (!error) {
				sendMessage(getMessageBuilderFactory().withPayload(payload).copyHeaders(headers).build());
			}
		});
		this.messageListenerContainer.afterPropertiesSet();
		super.onInit();
	}

	@Override
	protected void doStart() {
		this.messageListenerContainer.start();
	}

	@Override
	protected void doStop() {
		this.messageListenerContainer.stop();
	}


	/**
	 * {@inheritDoc}
	 * <p>
	 * Shuts down the listener container.
	 */
	@Override
	public int beforeShutdown() {
		this.stop();
		return 0;
	}


	/**
	 * {@inheritDoc}
	 * <p>No-op
	 */
	@Override
	public int afterShutdown() {
		return 0;
	}

}
