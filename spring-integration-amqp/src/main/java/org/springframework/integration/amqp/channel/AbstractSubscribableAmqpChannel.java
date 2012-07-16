/*
 * Copyright 2002-2012 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.dispatcher.AbstractDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
abstract class AbstractSubscribableAmqpChannel extends AbstractAmqpChannel implements SubscribableChannel, SmartLifecycle, DisposableBean {

	private final String channelName;

	private final SimpleMessageListenerContainer container;

	private volatile MessageDispatcher dispatcher;

	private final boolean isPubSub;

	private volatile int maxSubscribers = Integer.MAX_VALUE;

	public AbstractSubscribableAmqpChannel(String channelName, SimpleMessageListenerContainer container, AmqpTemplate amqpTemplate) {
		this(channelName, container, amqpTemplate, false);
	}

	public AbstractSubscribableAmqpChannel(String channelName,
			SimpleMessageListenerContainer container,
			AmqpTemplate amqpTemplate, boolean isPubSub) {
		super(amqpTemplate);
		Assert.notNull(container, "container must not be null");
		Assert.hasText(channelName, "channel name must not be empty");
		this.channelName = channelName;
		this.container = container;
		this.isPubSub = isPubSub;
	}

	/**
	 * Specify the maximum number of subscribers supported by the
	 * channel's dispatcher (if it is an {@link AbstractDispatcher}).
	 * @param maxSubscribers
	 */
	public void setMaxSubscribers(int maxSubscribers) {
		this.maxSubscribers = maxSubscribers;
	}

	public boolean subscribe(MessageHandler handler) {
		return this.dispatcher.addHandler(handler);
	}

	public boolean unsubscribe(MessageHandler handler) {
		return this.dispatcher.removeHandler(handler);
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();
		this.dispatcher = this.createDispatcher();
		if (this.dispatcher instanceof AbstractDispatcher) {
			((AbstractDispatcher) this.dispatcher).setMaxSubscribers(this.maxSubscribers);
		}
		AmqpAdmin admin = new RabbitAdmin(this.container.getConnectionFactory());
		Queue queue = this.initializeQueue(admin, this.channelName);
		this.container.setQueues(queue);
		MessageConverter converter = (this.getAmqpTemplate() instanceof RabbitTemplate)
				? ((RabbitTemplate) this.getAmqpTemplate()).getMessageConverter()
				: new SimpleMessageConverter();
		MessageListener listener = new DispatchingMessageListener(converter,
				this.dispatcher, this.channelName, this.isPubSub);
		this.container.setMessageListener(listener);
		if (!this.container.isActive()) {
			this.container.afterPropertiesSet();
		}
	}

	protected abstract MessageDispatcher createDispatcher();

	protected abstract Queue initializeQueue(AmqpAdmin admin, String channelName);


	private static class DispatchingMessageListener implements MessageListener {

		private final Log logger = LogFactory.getLog(this.getClass());

		private final MessageDispatcher dispatcher;

		private final MessageConverter converter;

		private final String channelName;

		private final boolean isPubSub;

		private DispatchingMessageListener(MessageConverter converter,
				MessageDispatcher dispatcher, String channelName, boolean isPubSub) {
			Assert.notNull(converter, "MessageConverter must not be null");
			Assert.notNull(dispatcher, "MessageDispatcher must not be null");
			this.converter = converter;
			this.dispatcher = dispatcher;
			this.channelName = channelName;
			this.isPubSub = isPubSub;
		}


		public void onMessage(org.springframework.amqp.core.Message message) {
			Message<?> messageToSend = null;
			try {
				Object converted = this.converter.fromMessage(message);
				if (converted != null) {
					messageToSend = (converted instanceof Message<?>) ? (Message<?>) converted
							: MessageBuilder.withPayload(converted).build();
					this.dispatcher.dispatch(messageToSend);
				}
				else if (this.logger.isWarnEnabled()) {
					logger.warn("MessageConverter returned null, no Message to dispatch");
				}
			}
			catch (MessageDispatchingException e) {
				String channelName = StringUtils.hasText(this.channelName) ? this.channelName : "unknown";
				String exceptionMessage = e.getMessage() + " for amqp-channel "
						+ channelName + ".";
				if (this.isPubSub) {
					// log only for backwards compatibility with pub/sub
					if (logger.isWarnEnabled()) {
						logger.warn(exceptionMessage, e);
					}
				}
				else {
					throw new MessageDeliveryException(
							messageToSend, exceptionMessage, e);
				}
			}
			catch (Exception e) {
				throw new MessagingException("Failure occured in AMQP listener while attempting to convert and dispatch Message.", e);
			}
		}
	}


	/*
	 * SmartLifecycle implementation (delegates to the MessageListener container)
	 */

	public boolean isAutoStartup() {
		return (this.container != null) ? this.container.isAutoStartup() : false;
	}

	public int getPhase() {
		return (this.container != null) ? this.container.getPhase() : 0;
	}

	public boolean isRunning() {
		return (this.container != null) ? this.container.isRunning() : false;
	}

	public void start() {
		if (this.container != null) {
			this.container.start();
		}
	}

	public void stop() {
		if (this.container != null) {
			this.container.stop();
		}
	}

	public void stop(Runnable callback) {
		if (this.container != null) {
			this.container.stop(callback);
		}
	}

	public void destroy() throws Exception {
		if (this.container != null) {
			this.container.destroy();
		}
	}

}
