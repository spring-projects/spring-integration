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
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.integration.dispatcher.AbstractDispatcher;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.1
 */
public class PointToPointSubscribableAmqpChannel extends AbstractSubscribableAmqpChannel {

	private volatile String queueName;


	public PointToPointSubscribableAmqpChannel(String channelName, SimpleMessageListenerContainer container,
			AmqpTemplate amqpTemplate) {
		super(channelName, container, amqpTemplate);
	}


	/**
	 * Provide a Queue name to be used. If this is not provided,
	 * the Queue's name will be the same as the channel name.
	 * @param queueName The queue name.
	 */
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	@Override
	protected String obtainQueueName(AmqpAdmin admin, String channelName) {
		if (this.queueName == null) {
			this.queueName = channelName;
		}
		if (admin.getQueueProperties(this.queueName) == null) {
			admin.declareQueue(new Queue(this.queueName));
		}
		return this.queueName;
	}

	@Override
	protected AbstractDispatcher createDispatcher() {
		UnicastingDispatcher unicastingDispatcher = new UnicastingDispatcher();
		unicastingDispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
		return unicastingDispatcher;
	}

	@Override
	protected String getRoutingKey() {
		return this.queueName;
	}

}
