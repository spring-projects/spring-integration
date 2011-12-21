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

import javax.jms.MessageListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class SubscribableJmsChannel extends AbstractJmsChannel implements SubscribableChannel, SmartLifecycle, DisposableBean {

	private final AbstractMessageListenerContainer container;

	private volatile MessageDispatcher dispatcher;
	
	private volatile boolean initialized;;
	
	public SubscribableJmsChannel(AbstractMessageListenerContainer container, JmsTemplate jmsTemplate) {
		super(jmsTemplate);
		Assert.notNull(container, "container must not be null");
		this.container = container;
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
		if (this.dispatcher == null){
			super.onInit();
			this.configureDispatcher(this.container.isPubSubDomain());
			MessageListener listener = new DispatchingMessageListener(this.getJmsTemplate(), this.dispatcher);
			this.container.setMessageListener(listener);
			if (!this.container.isActive()) {
				this.container.afterPropertiesSet();
			}
		}
	}

	private void configureDispatcher(boolean isPubSub) {
		if (isPubSub) {
			this.dispatcher = new BroadcastingDispatcher();
		}
		else {
			UnicastingDispatcher unicastingDispatcher = new UnicastingDispatcher();
			unicastingDispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
			this.dispatcher = unicastingDispatcher;
		}
	}


	private static class DispatchingMessageListener implements MessageListener {

		private final Log logger = LogFactory.getLog(this.getClass());

		private final JmsTemplate jmsTemplate;

		private final MessageDispatcher dispatcher;


		private DispatchingMessageListener(JmsTemplate jmsTemplate, MessageDispatcher dispatcher) {
			this.jmsTemplate = jmsTemplate;
			this.dispatcher = dispatcher;
		}


		public void onMessage(javax.jms.Message message) {
			try {
				Object converted = this.jmsTemplate.getMessageConverter().fromMessage(message);
				if (converted != null) {
					Message<?> messageToSend = (converted instanceof Message<?>) ? (Message<?>) converted
							: MessageBuilder.withPayload(converted).build();
					this.dispatcher.dispatch(messageToSend);
				}
				else if (this.logger.isWarnEnabled()) {
					logger.warn("MessageConverter returned null, no Message to dispatch");
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
