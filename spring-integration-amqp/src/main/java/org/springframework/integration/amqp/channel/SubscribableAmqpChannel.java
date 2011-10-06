/*
 * Copyright 2002-2011 the original author or authors.
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
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
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
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class SubscribableAmqpChannel extends AbstractAmqpChannel implements SubscribableChannel, SmartLifecycle, DisposableBean {

	private final String channelName;

	private final SimpleMessageListenerContainer container;

	private final boolean isPubSub;

	private volatile MessageDispatcher dispatcher;


	public SubscribableAmqpChannel(String channelName, SimpleMessageListenerContainer container, RabbitTemplate rabbitTemplate, boolean isPubSub) {
		super(rabbitTemplate);
		Assert.notNull(container, "container must not be null");
		Assert.hasText(channelName, "channel name must not be empty");
		this.channelName = channelName;
		this.container = container;
		this.isPubSub = isPubSub;
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
		this.configureDispatcher();
		AmqpTemplate amqpTemplate = this.getAmqpTemplate();
		Assert.isInstanceOf(RabbitTemplate.class, amqpTemplate);
		RabbitTemplate rabbitTemplate = (RabbitTemplate) amqpTemplate;
		RabbitAdmin admin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
		if (this.isPubSub) {
			FanoutExchange exchange = new FanoutExchange("si.fanout." + this.channelName);
			admin.declareExchange(exchange);
			Queue queue = admin.declareQueue();
			Binding binding = BindingBuilder.bind(queue).to(exchange);
			admin.declareBinding(binding);
			this.container.setQueues(queue);
			rabbitTemplate.setExchange(exchange.getName());
		}
		else {
			String queueName = "si." + this.channelName;
			Queue queue = new Queue(queueName);
			admin.declareQueue(queue);
			this.container.setQueues(queue);
			rabbitTemplate.setRoutingKey(queueName);
		}
		MessageListener listener = new DispatchingMessageListener((RabbitTemplate) amqpTemplate, this.dispatcher);
		this.container.setMessageListener(listener);
		if (!this.container.isActive()) {
			this.container.afterPropertiesSet();
		}
		rabbitTemplate.afterPropertiesSet();
	}

	private void configureDispatcher() {
		if (this.isPubSub) {
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

		private final RabbitTemplate rabbitTemplate;

		private final MessageDispatcher dispatcher;


		private DispatchingMessageListener(RabbitTemplate rabbitTemplate, MessageDispatcher dispatcher) {
			this.rabbitTemplate = rabbitTemplate;
			this.dispatcher = dispatcher;
		}


		public void onMessage(org.springframework.amqp.core.Message message) {
			try {
				Object converted = this.rabbitTemplate.getMessageConverter().fromMessage(message);
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
				throw new MessagingException("failed to handle incoming AMQP Message", e);
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
