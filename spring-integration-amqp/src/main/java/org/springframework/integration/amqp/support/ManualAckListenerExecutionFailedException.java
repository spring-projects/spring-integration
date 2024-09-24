/*
 * Copyright 2019-2024 the original author or authors.
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

import java.io.Serial;

import com.rabbitmq.client.Channel;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;

/**
 * A {@link ListenerExecutionFailedException} enhanced with the channel and delivery tag.
 * Used for conversion errors when using manual acks.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.1.3
 *
 */
public class ManualAckListenerExecutionFailedException extends ListenerExecutionFailedException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final transient Channel channel;

	private final long deliveryTag;

	/**
	 * Construct an instance with the provided properties.
	 * @param msg the exception message.
	 * @param cause the cause.
	 * @param channel the channel.
	 * @param deliveryTag the delivery tag for the last message.
	 * @param failedMessages the failed message(s).
	 * @since 5.3
	 */
	public ManualAckListenerExecutionFailedException(String msg, Throwable cause,
			Channel channel, long deliveryTag, Message... failedMessages) {

		super(msg, cause, failedMessages);
		this.channel = channel;
		this.deliveryTag = deliveryTag;
	}

	/**
	 * Return the channel.
	 * @return the channel.
	 */
	public Channel getChannel() {
		return this.channel;
	}

	/**
	 * Return the delivery tag for the last failed message.
	 * @return the tag.
	 */
	public long getDeliveryTag() {
		return this.deliveryTag;
	}

}
