/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.amqp.support;

import java.util.List;

import com.rabbitmq.client.Channel;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;

/**
 * Utility methods for messaging endpoints.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.1.3
 *
 */
public final class EndpointUtils {

	private static final String LEFE_MESSAGE = "Message conversion failed";

	private EndpointUtils() {
	}

	/**
	 * Return an {@link ListenerExecutionFailedException} or a {@link ManualAckListenerExecutionFailedException}
	 * depending on whether isManualAck is false or true.
	 * @param message the failed message.
	 * @param channel the channel.
	 * @param isManualAck true if the container uses manual acknowledgment.
	 * @param ex the exception.
	 * @return the exception.
	 */
	public static ListenerExecutionFailedException errorMessagePayload(Message message,
			Channel channel, boolean isManualAck, Exception ex) {

		return isManualAck
				? new ManualAckListenerExecutionFailedException(LEFE_MESSAGE, ex, channel,
				message.getMessageProperties().getDeliveryTag(), message)
				: new ListenerExecutionFailedException(LEFE_MESSAGE, ex, message);
	}

	/**
	 * Return an {@link ListenerExecutionFailedException} or a {@link ManualAckListenerExecutionFailedException}
	 * depending on whether isManualAck is false or true.
	 * @param messages the failed messages.
	 * @param channel the channel.
	 * @param isManualAck true if the container uses manual acknowledgment.
	 * @param ex the exception.
	 * @return the exception.
	 * @since 5.3
	 */
	public static ListenerExecutionFailedException errorMessagePayload(List<Message> messages,
			Channel channel, boolean isManualAck, Exception ex) {

		return isManualAck
				? new ManualAckListenerExecutionFailedException(LEFE_MESSAGE, ex, channel,
				messages.get(messages.size() - 1).getMessageProperties().getDeliveryTag(),
				messages.toArray(new Message[0]))
				: new ListenerExecutionFailedException(LEFE_MESSAGE, ex, messages.toArray(new Message[0]));
	}

}
