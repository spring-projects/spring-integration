/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.jms;

import javax.jms.MessageListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.channel.BroadcastCapableChannel;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.dispatcher.AbstractDispatcher;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * An {@link AbstractJmsChannel} implementation for message-driven subscriptions.
 * Also implements a {@link BroadcastCapableChannel} to represent possible pub-sub semantics
 * when configured against JMS topic.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SubscribableJmsChannel extends AbstractJmsChannel
		implements BroadcastCapableChannel, SmartLifecycle {

	private final AbstractMessageListenerContainer container;

	private volatile AbstractDispatcher dispatcher;

	private volatile boolean initialized;

	private volatile Integer maxSubscribers;

	public SubscribableJmsChannel(AbstractMessageListenerContainer container, JmsTemplate jmsTemplate) {
		super(jmsTemplate);
		Assert.notNull(container, "container must not be null");
		this.container = container;
	}

	/**
	 * Specify the maximum number of subscribers supported by the
	 * channel's dispatcher.
	 * @param maxSubscribers The maximum number of subscribers allowed.
	 */
	public void setMaxSubscribers(int maxSubscribers) {
		this.maxSubscribers = maxSubscribers;
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		Assert.state(this.dispatcher != null,
				"'MessageDispatcher' must not be null. This channel might not have been initialized");
		return this.dispatcher.addHandler(handler);
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		Assert.state(this.dispatcher != null,
				"'MessageDispatcher' must not be null. This channel might not have been initialized");
		return this.dispatcher.removeHandler(handler);
	}

	@Override
	public boolean isBroadcast() {
		return this.container.isPubSubDomain();
	}

	@Override
	public void onInit() {
		if (this.initialized) {
			return;
		}
		super.onInit();
		boolean isPubSub = isBroadcast();
		configureDispatcher(isPubSub);
		MessageListener listener =
				new DispatchingMessageListener(getJmsTemplate(), this.dispatcher, this, isPubSub,
						getMessageBuilderFactory());
		this.container.setMessageListener(listener);
		if (!this.container.isActive()) {
			this.container.afterPropertiesSet();
		}
		this.initialized = true;
	}

	private void configureDispatcher(boolean isPubSub) {
		if (isPubSub) {
			BroadcastingDispatcher broadcastingDispatcher = new BroadcastingDispatcher(true);
			broadcastingDispatcher.setBeanFactory(getBeanFactory());
			this.dispatcher = broadcastingDispatcher;
		}
		else {
			UnicastingDispatcher unicastingDispatcher = new UnicastingDispatcher();
			unicastingDispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
			this.dispatcher = unicastingDispatcher;
		}
		if (this.maxSubscribers == null) {
			this.maxSubscribers = this.getIntegrationProperty(isPubSub ?
							IntegrationProperties.CHANNELS_MAX_BROADCAST_SUBSCRIBERS :
							IntegrationProperties.CHANNELS_MAX_UNICAST_SUBSCRIBERS,
					Integer.class);
		}
		this.dispatcher.setMaxSubscribers(this.maxSubscribers);
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
		else {
			callback.run();
		}
	}

	@Override
	public void destroy() {
		if (this.container != null) {
			this.container.destroy();
		}
	}


	private static final class DispatchingMessageListener implements MessageListener {

		private final Log logger = LogFactory.getLog(this.getClass());

		private final JmsTemplate jmsTemplate;

		private final MessageDispatcher dispatcher;

		private final SubscribableJmsChannel channel;

		private final boolean isPubSub;

		private final MessageBuilderFactory messageBuilderFactory;


		DispatchingMessageListener(JmsTemplate jmsTemplate,
				MessageDispatcher dispatcher, SubscribableJmsChannel channel, boolean isPubSub,
				MessageBuilderFactory messageBuilderFactory) {

			this.jmsTemplate = jmsTemplate;
			this.dispatcher = dispatcher;
			this.channel = channel;
			this.isPubSub = isPubSub;
			this.messageBuilderFactory = messageBuilderFactory;
		}


		@Override
		public void onMessage(javax.jms.Message message) {
			Message<?> messageToSend = null;
			try {
				MessageConverter converter = this.jmsTemplate.getMessageConverter();
				Object converted = null;
				if (converter != null) {
					converted = converter.fromMessage(message);
				}
				if (converted != null) {
					messageToSend =
							converted instanceof Message<?>
									? (Message<?>) converted
									: this.messageBuilderFactory.withPayload(converted).build();
					this.dispatcher.dispatch(messageToSend);
				}
				else if (this.logger.isWarnEnabled()) {
					this.logger.warn("No converter found, or converter returned null for: " + message
							+ ", no Message to dispatch");
				}
			}
			catch (MessageDispatchingException e) {
				String exceptionMessage = e.getMessage() + " for jms-channel '"
						+ this.channel.getFullChannelName() + "'.";
				if (this.isPubSub) {
					// log only for backwards compatibility with pub/sub
					if (this.logger.isWarnEnabled()) {
						this.logger.warn(exceptionMessage, e);
					}
				}
				else {
					throw new MessageDeliveryException(messageToSend, exceptionMessage, e);
				}
			}
			catch (Exception e) {
				throw new MessagingException("failed to handle incoming JMS Message", e);
			}
		}

	}

}
