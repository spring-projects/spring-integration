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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.dispatcher.AbstractDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 */
abstract class AbstractSubscribableAmqpChannel extends AbstractAmqpChannel
		implements SubscribableChannel, SmartLifecycle, DisposableBean {

	private final String channelName;

	private final SimpleMessageListenerContainer container;

	private volatile AbstractDispatcher dispatcher;

	private final boolean isPubSub;

	private volatile Integer maxSubscribers;

	private final AmqpAdmin admin;

	private final ConnectionFactory connectionFactory;

	/**
	 * Construct an instance with the supplied name, container and template; default header
	 * mappers will be used if the message is mapped.
	 * @param channelName the channel name.
	 * @param container the container.
	 * @param amqpTemplate the template.
	 * @see #setExtractPayload(boolean)
	 */
	protected AbstractSubscribableAmqpChannel(String channelName, SimpleMessageListenerContainer container,
			AmqpTemplate amqpTemplate) {
		this(channelName, container, amqpTemplate, false);
	}

	/**
	 * Construct an instance with the supplied name, container and template; default header
	 * mappers will be used if the message is mapped.
	 * @param channelName the channel name.
	 * @param container the container.
	 * @param amqpTemplate the template.
	 * @param outboundMapper the outbound mapper.
	 * @param inboundMapper the inbound mapper.
	 * @see #setExtractPayload(boolean)
	 * @since 4.3
	 */
	protected AbstractSubscribableAmqpChannel(String channelName, SimpleMessageListenerContainer container,
			AmqpTemplate amqpTemplate, AmqpHeaderMapper outboundMapper, AmqpHeaderMapper inboundMapper) {
		this(channelName, container, amqpTemplate, false, outboundMapper, inboundMapper);
	}

	/**
	 * Construct an instance with the supplied name, container and template; default header
	 * mappers will be used if the message is mapped.
	 * @param channelName the channel name.
	 * @param container the container.
	 * @param amqpTemplate the template.
	 * @param isPubSub true for a pub/sub channel.
	 * @see #setExtractPayload(boolean)
	 */
	protected AbstractSubscribableAmqpChannel(String channelName,
			SimpleMessageListenerContainer container,
			AmqpTemplate amqpTemplate, boolean isPubSub) {
		super(amqpTemplate);
		Assert.notNull(container, "container must not be null");
		Assert.hasText(channelName, "channel name must not be empty");
		this.channelName = channelName;
		this.container = container;
		this.isPubSub = isPubSub;
		this.connectionFactory = container.getConnectionFactory();
		this.admin = new RabbitAdmin(this.connectionFactory);
	}

	/**
	 * Construct an instance with the supplied name, container and template; default header
	 * mappers will be used if the message is mapped.
	 * @param channelName the channel name.
	 * @param container the container.
	 * @param amqpTemplate the template.
	 * @param isPubSub true for a pub/sub channel.
	 * @param outboundMapper the outbound mapper.
	 * @param inboundMapper the inbound mapper.
	 * @see #setExtractPayload(boolean)
	 * @since 4.3
	 */
	protected AbstractSubscribableAmqpChannel(String channelName,
			SimpleMessageListenerContainer container,
			AmqpTemplate amqpTemplate, boolean isPubSub,
			AmqpHeaderMapper outboundMapper, AmqpHeaderMapper inboundMapper) {
		super(amqpTemplate, outboundMapper, inboundMapper);
		Assert.notNull(container, "container must not be null");
		Assert.hasText(channelName, "channel name must not be empty");
		this.channelName = channelName;
		this.container = container;
		this.isPubSub = isPubSub;
		this.connectionFactory = container.getConnectionFactory();
		this.admin = new RabbitAdmin(this.connectionFactory);
	}

	/**
	 * Specify the maximum number of subscribers supported by the
	 * channel's dispatcher (if it is an {@link AbstractDispatcher}).
	 * @param maxSubscribers The maximum number of subscribers allowed.
	 */
	public void setMaxSubscribers(int maxSubscribers) {
		this.maxSubscribers = maxSubscribers;
		if (this.dispatcher != null) {
			this.dispatcher.setMaxSubscribers(this.maxSubscribers);
		}
	}

	protected AmqpAdmin getAdmin() {
		return this.admin;
	}

	protected ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		return this.dispatcher.addHandler(handler);
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		return this.dispatcher.removeHandler(handler);
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();
		this.dispatcher = this.createDispatcher();
		if (this.maxSubscribers == null) {
			this.maxSubscribers = this.getIntegrationProperty(this.isPubSub ?
					IntegrationProperties.CHANNELS_MAX_BROADCAST_SUBSCRIBERS :
					IntegrationProperties.CHANNELS_MAX_UNICAST_SUBSCRIBERS,
					Integer.class);
		}
		this.setMaxSubscribers(this.maxSubscribers);
		String queue = this.obtainQueueName(this.admin, this.channelName);
		this.container.setQueueNames(queue);
		MessageConverter converter = (this.getAmqpTemplate() instanceof RabbitTemplate)
				? ((RabbitTemplate) this.getAmqpTemplate()).getMessageConverter()
				: new SimpleMessageConverter();
		MessageListener listener = new DispatchingMessageListener(converter,
				this.dispatcher, this, this.isPubSub,
				getMessageBuilderFactory(), getInboundHeaderMapper());
		this.container.setMessageListener(listener);
		if (!this.container.isActive()) {
			this.container.afterPropertiesSet();
		}
	}

	/*
	 * SmartLifecycle implementation (delegates to the MessageListener container)
	 */

	@Override
	public boolean isAutoStartup() {
		return (this.container != null) && this.container.isAutoStartup();
	}

	@Override
	public int getPhase() {
		return (this.container != null) ? this.container.getPhase() : 0;
	}

	@Override
	public boolean isRunning() {
		return (this.container != null) && this.container.isRunning();
	}

	@Override
	public void start() {
		if (this.container != null) {
			this.container.start();
		}
	}

	@Override
	public void stop() {
		if (this.container != null) {
			this.container.stop();
		}
	}

	@Override
	public void stop(Runnable callback) {
		if (this.container != null) {
			this.container.stop(callback);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (this.container != null) {
			this.container.destroy();
		}
	}

	protected abstract AbstractDispatcher createDispatcher();

	protected abstract String obtainQueueName(AmqpAdmin admin, String channelName);


	private static final class DispatchingMessageListener implements MessageListener {

		private final Log logger = LogFactory.getLog(this.getClass());

		private final MessageDispatcher dispatcher;

		private final MessageConverter converter;

		private final AbstractSubscribableAmqpChannel channel;

		private final boolean isPubSub;

		private final MessageBuilderFactory messageBuilderFactory;

		private final AmqpHeaderMapper inboundHeaderMapper;

		private DispatchingMessageListener(MessageConverter converter,
				MessageDispatcher dispatcher, AbstractSubscribableAmqpChannel channel, boolean isPubSub,
				MessageBuilderFactory messageBuilderFactory, AmqpHeaderMapper inboundHeaderMapper) {
			Assert.notNull(converter, "MessageConverter must not be null");
			Assert.notNull(dispatcher, "MessageDispatcher must not be null");
			this.converter = converter;
			this.dispatcher = dispatcher;
			this.channel = channel;
			this.isPubSub = isPubSub;
			this.messageBuilderFactory = messageBuilderFactory;
			this.inboundHeaderMapper = inboundHeaderMapper;
		}


		@Override
		public void onMessage(org.springframework.amqp.core.Message message) {
			Message<?> messageToSend = null;
			try {
				Object converted = this.converter.fromMessage(message);
				if (converted != null) {
					messageToSend = (converted instanceof Message<?>) ? (Message<?>) converted
							: buildMessage(message, converted);
					this.dispatcher.dispatch(messageToSend);
				}
				else if (this.logger.isWarnEnabled()) {
					this.logger.warn("MessageConverter returned null, no Message to dispatch");
				}
			}
			catch (MessageDispatchingException e) {
				String exceptionMessage = e.getMessage() + " for amqp-channel '"
						+ this.channel.getFullChannelName() + "'.";
				if (this.isPubSub) {
					// log only for backwards compatibility with pub/sub
					if (this.logger.isWarnEnabled()) {
						this.logger.warn(exceptionMessage, e);
					}
				}
				else {
					throw new MessageDeliveryException(
							messageToSend, exceptionMessage, e);
				}
			}
			catch (Exception e) {
				throw new MessagingException("Failure occured in AMQP listener " +
						"while attempting to convert and dispatch Message.", e);
			}
		}

		protected Message<Object> buildMessage(org.springframework.amqp.core.Message message, Object converted) {
			AbstractIntegrationMessageBuilder<Object> messageBuilder =
					this.messageBuilderFactory.withPayload(converted);
			if (this.channel.isExtractPayload()) {
				Map<String, Object> headers =
						this.inboundHeaderMapper.toHeadersFromRequest(message.getMessageProperties());
				messageBuilder.copyHeaders(headers);
			}
			return messageBuilder.build();
		}

	}

}
