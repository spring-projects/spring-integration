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

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.integration.Message;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class PollableAmqpChannel extends AbstractAmqpChannel implements PollableChannel {

	private final String channelName;


	public PollableAmqpChannel(String channelName, AmqpTemplate amqpTemplate) {
		super(amqpTemplate);
		Assert.hasText(channelName, "channel name must not be empty");
		this.channelName = channelName;
	}


	@Override
	protected void onInit() throws Exception {
		AmqpTemplate amqpTemplate = this.getAmqpTemplate();
		if (!(amqpTemplate instanceof RabbitTemplate)) {
			throw new IllegalArgumentException("AmqpTemplate must be a RabbitTemplate");
		}
		RabbitTemplate rabbitTemplate = (RabbitTemplate) amqpTemplate;
		RabbitAdmin admin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
		String queueName = "si." + this.channelName;
		Queue queue = new Queue(queueName);
		admin.declareQueue(queue);
		rabbitTemplate.setRoutingKey(queueName);
		rabbitTemplate.setQueue(queueName);
	}

	public Message<?> receive() {
		if (!this.getInterceptors().preReceive(this)) {
 			return null;
 		}
		Object object = this.getAmqpTemplate().receiveAndConvert();
		if (object == null) {
			return null;
		}
		Message<?> replyMessage = null;
		if (object instanceof Message<?>) {
			replyMessage = (Message<?>) object;
		}
		else {
			replyMessage = MessageBuilder.withPayload(object).build();
		}
		return this.getInterceptors().postReceive(replyMessage, this) ;
	}

	public Message<?> receive(long timeout) {
		if (logger.isInfoEnabled()) {
			logger.info("Calling receive with a timeout value on PollableAmqpChannel. " +
					"The timeout will be ignored since no receive timeout is supported.");
		}
		return this.receive();
	}

}
