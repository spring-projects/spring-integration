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

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
public class PublishSubscribeAmqpChannel extends AbstractSubscribableAmqpChannel {

	private volatile FanoutExchange exchange;


	public PublishSubscribeAmqpChannel(String channelName, SimpleMessageListenerContainer container, AmqpTemplate amqpTemplate) {
		super(channelName, container, amqpTemplate, true);
	}


	/**
	 * Configure the FanoutExchange instance. If this is not provided, then a
	 * FanoutExchange will be declared implicitly, and its name will be the same
	 * as the channel name prefixed by "si.fanout.". In either case, an effectively
	 * anonymous Queue will be declared automatically.
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
		Queue queue = admin.declareQueue();
		Binding binding = BindingBuilder.bind(queue).to(this.exchange);
		admin.declareBinding(binding);
		return queue;
	}

	@Override
	protected MessageDispatcher createDispatcher() {
		return new BroadcastingDispatcher(true);
	}

	@Override
	protected String getExchangeName() {
		return (this.exchange != null) ? this.exchange.getName() : "";
	}

}
