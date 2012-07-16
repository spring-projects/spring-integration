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

package org.springframework.integration.jms;

import javax.jms.MessageListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.dispatcher.AbstractDispatcher;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class SubscribableJmsChannel extends AbstractJmsChannel implements SubscribableChannel, SmartLifecycle, DisposableBean {

	private final AbstractMessageListenerContainer container;

	private volatile AbstractDispatcher dispatcher;

	private volatile boolean initialized;

	private volatile int maxSubscribers = Integer.MAX_VALUE;

	public SubscribableJmsChannel(AbstractMessageListenerContainer container, JmsTemplate jmsTemplate) {
		super(jmsTemplate);
		Assert.notNull(container, "container must not be null");
		this.container = container;
	}

	/**
	 * Specify the maximum number of subscribers supported by the
	 * channel's dispatcher.
	 * @param maxSubscribers
	 */
	public void setMaxSubscribers(int maxSubscribers) {
		this.maxSubscribers = maxSubscribers;
	}

	public boolean subscribe(MessageHandler handler) {
		Assert.state(this.dispatcher != null, "'MessageDispatcher' must not be null. This channel might not have been initialized");
		return this.dispatcher.addHandler(handler);
	}

	public boolean unsubscribe(MessageHandler handler) {
		Assert.state(this.dispatcher != null, "'MessageDispatcher' must not be null. This channel might not have been initialized");
		return this.dispatcher.removeHandler(handler);
	}

	@Override
	public void onInit() throws Exception {
		if (this.initialized){
			return;
		}
		super.onInit();
		boolean isPubSub = this.container.isPubSubDomain();
		this.configureDispatcher(isPubSub);
		MessageListener listener = new DispatchingMessageListener(
				this.getJmsTemplate(), this.dispatcher,
				this.getComponentName(), isPubSub);
		this.container.setMessageListener(listener);
		if (!this.container.isActive()) {
			this.container.afterPropertiesSet();
		}
		this.initialized = true;
	}

	private void configureDispatcher(boolean isPubSub) {
		if (isPubSub) {
			this.dispatcher = new BroadcastingDispatcher(true);
		}
		else {
			UnicastingDispatcher unicastingDispatcher = new UnicastingDispatcher();
			unicastingDispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
			this.dispatcher = unicastingDispatcher;
		}
		this.dispatcher.setMaxSubscribers(this.maxSubscribers);
	}


	private static class DispatchingMessageListener implements MessageListener {

		private final Log logger = LogFactory.getLog(this.getClass());

		private final JmsTemplate jmsTemplate;

		private final MessageDispatcher dispatcher;

		private final String channelName;

		private final boolean isPubSub;


		private DispatchingMessageListener(JmsTemplate jmsTemplate,
				MessageDispatcher dispatcher, String channelName, boolean isPubSub) {
			this.jmsTemplate = jmsTemplate;
			this.dispatcher = dispatcher;
			this.channelName = channelName;
			this.isPubSub = isPubSub;
		}


		public void onMessage(javax.jms.Message message) {
			Message<?> messageToSend = null;
			try {
				Object converted = this.jmsTemplate.getMessageConverter().fromMessage(message);
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
				String exceptionMessage = e.getMessage() + " for jms-channel "
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
				throw new MessagingException("failed to handle incoming JMS Message", e);
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
