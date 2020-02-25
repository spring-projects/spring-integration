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

package org.springframework.integration.amqp.channel;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.channel.BroadcastCapableChannel;
import org.springframework.integration.dispatcher.AbstractDispatcher;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;

/**
 * The {@link AbstractSubscribableAmqpChannel} extension for pub-sub semantics based on the {@link FanoutExchange}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class PublishSubscribeAmqpChannel extends AbstractSubscribableAmqpChannel implements BroadcastCapableChannel {

	private final Queue queue = new AnonymousQueue();

	private volatile FanoutExchange exchange;

	private volatile Binding binding;

	/**
	 * Construct an instance with the supplied name, container and template; default header
	 * mappers will be used if the message is mapped.
	 * @param channelName the channel name.
	 * @param container the container.
	 * @param amqpTemplate the template.
	 * @see #setExtractPayload(boolean)
	 */
	public PublishSubscribeAmqpChannel(String channelName, AbstractMessageListenerContainer container,
			AmqpTemplate amqpTemplate) {

		super(channelName, container, amqpTemplate, true);
	}

	/**
	 * Construct an instance with the supplied name, container and template; default header
	 * mappers will be used if the message is mapped.
	 * @param channelName the channel name.
	 * @param container the container.
	 * @param amqpTemplate the template
	 * @param outboundMapper the outbound mapper.
	 * @param inboundMapper the inbound mapper.
	 * @see #setExtractPayload(boolean)
	 * @since 4.3
	 */
	public PublishSubscribeAmqpChannel(String channelName, AbstractMessageListenerContainer container,
			AmqpTemplate amqpTemplate, AmqpHeaderMapper outboundMapper, AmqpHeaderMapper inboundMapper) {

		super(channelName, container, amqpTemplate, true, outboundMapper, inboundMapper);
	}


	/**
	 * Configure the FanoutExchange instance. If this is not provided, then a
	 * FanoutExchange will be declared implicitly, and its name will be the same
	 * as the channel name prefixed by "si.fanout.". In either case, an effectively
	 * anonymous Queue will be declared automatically.
	 * @param exchange The fanout exchange.
	 */
	public void setExchange(FanoutExchange exchange) {
		this.exchange = exchange;
	}

	@Override
	protected String getExchangeName() {
		return (this.exchange != null) ? this.exchange.getName() : "";
	}

	@Override
	protected String obtainQueueName(String channelName) {
		if (this.exchange == null) {
			String exchangeName = "si.fanout." + channelName;
			this.exchange = new FanoutExchange(exchangeName);
		}

		this.binding = BindingBuilder.bind(this.queue).to(this.exchange);

		return this.queue.getName();
	}

	@Override
	protected AbstractDispatcher createDispatcher() {
		BroadcastingDispatcher broadcastingDispatcher = new BroadcastingDispatcher(true);
		broadcastingDispatcher.setBeanFactory(getBeanFactory());
		return broadcastingDispatcher;
	}

	@Override
	protected void doDeclares() {
		AmqpAdmin admin = getAdmin();
		if (admin != null) {
			if (admin.getQueueProperties(this.queue.getName()) == null) {
				admin.declareQueue(this.queue);
			}
			if (this.exchange != null) {
				admin.declareExchange(this.exchange);
			}
			if (this.binding != null) {
				admin.declareBinding(this.binding);
			}
		}
	}

}
