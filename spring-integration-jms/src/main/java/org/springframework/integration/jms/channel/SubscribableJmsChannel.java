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

import jakarta.jms.MessageListener;

import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.channel.BroadcastCapableChannel;
import org.springframework.integration.dispatcher.AbstractDispatcher;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.support.management.ManageableSmartLifecycle;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
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
 * @since 7.0
 */
public class SubscribableJmsChannel extends AbstractJmsChannel
		implements BroadcastCapableChannel, ManageableSmartLifecycle {

	private final AbstractMessageListenerContainer container;

	@SuppressWarnings("NullAway.Init")
	private volatile AbstractDispatcher dispatcher;

	private volatile boolean initialized;

	@SuppressWarnings("NullAway.Init")
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
		return this.dispatcher.addHandler(handler);
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
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
		configureDispatcher(isBroadcast());
		this.container.setMessageListener((MessageListener) this::receiveAndDispatch);
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
			this.maxSubscribers =
					isPubSub
							? getIntegrationProperties().getChannelsMaxBroadcastSubscribers()
							: getIntegrationProperties().getChannelsMaxUnicastSubscribers();
		}
		this.dispatcher.setMaxSubscribers(this.maxSubscribers);
	}

	/*
	 * SmartLifecycle implementation (delegates to the MessageListener container)
	 */

	@Override
	public boolean isAutoStartup() {
		return this.container.isAutoStartup();
	}

	@Override
	public int getPhase() {
		return this.container.getPhase();
	}

	@Override
	public boolean isRunning() {
		return this.container.isRunning();
	}

	@Override
	public void start() {
		this.container.start();
	}

	@Override
	public void stop() {
		this.container.stop();
	}

	@Override
	public void stop(Runnable callback) {
		this.container.stop(callback);
	}

	@Override
	public void destroy() {
		this.container.destroy();
	}

	private void receiveAndDispatch(jakarta.jms.Message message) {
		Message<?> messageToSend = fromJmsMessage(message);
		try {
			this.dispatcher.dispatch(messageToSend);
		}
		catch (MessageDispatchingException ex) {
			String exceptionMessage = ex.getMessage() + " for jms-channel '" + this.getFullChannelName() + "'.";
			if (isBroadcast()) {
				// log only for backwards compatibility with pub/sub
				this.logger.warn(ex, exceptionMessage);
			}
			else {
				throw new MessageDeliveryException(messageToSend, exceptionMessage, ex);
			}
		}
	}

}
