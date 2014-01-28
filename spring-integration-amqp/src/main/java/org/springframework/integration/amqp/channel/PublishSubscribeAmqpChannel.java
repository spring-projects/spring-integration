/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.integration.dispatcher.AbstractDispatcher;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
public class PublishSubscribeAmqpChannel extends AbstractSubscribableAmqpChannel implements ConnectionListener {

	private volatile FanoutExchange exchange;

	private volatile AmqpAdmin admin;

	private volatile AnonymousQueue queue;

	private volatile Binding binding;

	private volatile boolean initialized;

	public PublishSubscribeAmqpChannel(String channelName, SimpleMessageListenerContainer container, AmqpTemplate amqpTemplate) {
		super(channelName, container, amqpTemplate, true);
	}


	/**
	 * Configure the FanoutExchange instance. If this is not provided, then a
	 * FanoutExchange will be declared implicitly, and its name will be the same
	 * as the channel name prefixed by "si.fanout.". In either case, an effectively
	 * anonymous Queue will be declared automatically.
	 *
	 * @param exchange The fanout exchange.
	 */
	public void setExchange(FanoutExchange exchange) {
		this.exchange = exchange;
	}

	@Override
	protected Queue initializeQueue(AmqpAdmin admin, String channelName) {
		if (this.exchange == null) {
			String exchangeName = "si.fanout." + channelName;
			this.exchange = new FanoutExchange(exchangeName);
		}
		admin.declareExchange(this.exchange);
		this.queue = new AnonymousQueue();
		admin.declareQueue(this.queue);
		this.binding = BindingBuilder.bind(this.queue).to(this.exchange);
		admin.declareBinding(this.binding);
		this.admin = admin;
		if (!this.initialized && this.getAmqpTemplate() instanceof RabbitTemplate) {
			((RabbitTemplate) this.getAmqpTemplate()).getConnectionFactory().addConnectionListener(this);
		}
		this.initialized = true;
		return this.queue;
	}

	protected void doDeclares() {
		if (this.isRunning()) {
			if (this.admin != null) {
				if (this.queue != null) {
					admin.declareQueue(this.queue);
				}
				if (this.binding != null) {
					admin.declareBinding(this.binding);
				}
			}
		}
	}

	@Override
	protected AbstractDispatcher createDispatcher() {
		return new BroadcastingDispatcher(true);
	}

	@Override
	protected String getExchangeName() {
		return (this.exchange != null) ? this.exchange.getName() : "";
	}

	@Override
	public void destroy() throws Exception {
		super.destroy();
		/*
		 * TODO: remove this from the CF listeners, when the connection factory supports it.
		 * Also reset this.initialized.
		 */
	}

	@Override
	public void start() {
		this.doDeclares(); // connection may have been lost while we were stopped
		super.start();
	}

	@Override
	public void onCreate(Connection connection) {
		doDeclares();
	}

	@Override
	public void onClose(Connection connection) {
	}

}
