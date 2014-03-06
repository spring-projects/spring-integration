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
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.util.Assert;

/**
 * A {@link PollableChannel} implementation that is backed by an AMQP Queue.
 * Messages will be sent to the default (no-name) exchange with that Queue's
 * name as the routing key.
 *
 * @author Mark Fisher
 * @since 2.1
 */
public class PollableAmqpChannel extends AbstractAmqpChannel implements PollableChannel {

	private final String channelName;

	private volatile String queueName;

	private volatile AmqpAdmin amqpAdmin;


	public PollableAmqpChannel(String channelName, AmqpTemplate amqpTemplate) {
		super(amqpTemplate);
		Assert.hasText(channelName, "channel name must not be empty");
		this.channelName = channelName;
	}


	/**
	 * Provide an explicitly configured queue name. If this is not provided, then a Queue will be created
	 * implicitly with the channelName as its name. The implicit creation will require that either an AmqpAdmin
	 * instance has been provided or that the configured AmqpTemplate is an instance of RabbitTemplate.
	 *
	 * @param queueName The queue name.
	 */
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	/**
	 * Provide an instance of AmqpAdmin for implicitly declaring Queues if the queueName is not provided.
	 * When providing a RabbitTemplate implementation, this is not strictly necessary since a RabbitAdmin
	 * instance can be created from the template's ConnectionFactory reference.
	 *
	 * @param amqpAdmin The amqp admin.
	 */
	public void setAmqpAdmin(AmqpAdmin amqpAdmin) {
		this.amqpAdmin = amqpAdmin;
	}

	@Override
	protected void onInit() throws Exception {
		AmqpTemplate amqpTemplate = this.getAmqpTemplate();
		if (this.queueName == null) {
			if (this.amqpAdmin == null && amqpTemplate instanceof RabbitTemplate) {
				this.amqpAdmin = new RabbitAdmin(((RabbitTemplate) amqpTemplate).getConnectionFactory());
			}
			Assert.notNull(this.amqpAdmin,
					"If no queueName is configured explicitly, an AmqpAdmin instance must be provided, " +
					"or the AmqpTemplate must be a RabbitTemplate since the Queue needs to be declared.");
			this.queueName = this.channelName;
			this.amqpAdmin.declareQueue(new Queue(this.queueName));
		}
	}

	@Override
	protected String getRoutingKey() {
		return this.queueName;
	}

	@Override
	public Message<?> receive() {
		if (!this.getInterceptors().preReceive(this)) {
 			return null;
 		}
		Object object = this.getAmqpTemplate().receiveAndConvert(this.queueName);
		if (object == null) {
			return null;
		}
		Message<?> replyMessage = null;
		if (object instanceof Message<?>) {
			replyMessage = (Message<?>) object;
		}
		else {
			replyMessage = this.getMessageBuilderFactory().withPayload(object).build();
		}
		return this.getInterceptors().postReceive(replyMessage, this) ;
	}

	@Override
	public Message<?> receive(long timeout) {
		if (logger.isInfoEnabled()) {
			logger.info("Calling receive with a timeout value on PollableAmqpChannel. " +
					"The timeout will be ignored since no receive timeout is supported.");
		}
		return this.receive();
	}

}
