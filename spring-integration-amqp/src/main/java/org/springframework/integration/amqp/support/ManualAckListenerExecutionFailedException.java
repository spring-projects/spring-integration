/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.amqp.support;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;

import com.rabbitmq.client.Channel;

/**
 * A {@link ListenerExecutionFailedException} enhanced with the channel and delivery tag.
 * Used for conversion errors when using manual acks.
 *
 * @author Gary Russell
 * @since 5.1.3
 *
 */
public class ManualAckListenerExecutionFailedException extends ListenerExecutionFailedException {

	private static final long serialVersionUID = 1L;

	private final Channel channel;

	private final long deliveryTag;

	public ManualAckListenerExecutionFailedException(String msg, Throwable cause, Message failedMessage,
			Channel channel, long deliveryTag) {

		super(msg, cause, failedMessage);
		this.channel = channel;
		this.deliveryTag = deliveryTag;
	}

	public Channel getChannel() {
		return this.channel;
	}

	public long getDeliveryTag() {
		return this.deliveryTag;
	}

}
