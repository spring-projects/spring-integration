/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.aop;

import org.springframework.integration.core.MessageSource;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * A {@link ReceiveMessageAdvice} extension that can mutate a {@link MessageSource} before and/or after
 * {@link MessageSource#receive()} is called.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0.7
 */
@FunctionalInterface
public interface MessageSourceMutator extends ReceiveMessageAdvice {

	@Override
	default boolean beforeReceive(Object source) {
		if (source instanceof MessageSource<?>) {
			return beforeReceive((MessageSource<?>) source);
		}
		else {
			throw new IllegalArgumentException(
					"The 'MessageSourceMutator' supports only a 'MessageSource' in the before/after hooks: " + source);
		}
	}

	/**
	 * Subclasses can decide whether to proceed with this poll.
	 * @param source the message source.
	 * @return true to proceed (default).
	 */
	default boolean beforeReceive(MessageSource<?> source) {
		return true;
	}

	@Override
	@Nullable
	default Message<?> afterReceive(@Nullable Message<?> result, Object source) {
		if (source instanceof MessageSource<?>) {
			return afterReceive(result, (MessageSource<?>) source);
		}
		else {
			throw new IllegalArgumentException(
					"The 'MessageSourceMutator' supports only a 'MessageSource' in the before/after hooks: " + source);
		}
	}

	/**
	 * Subclasses can take actions based on the result of the poll; e.g.
	 * adjust the {@code trigger}. The message can also be replaced with a new one.
	 * @param result the received message.
	 * @param source the message source.
	 * @return a message to continue to process the result, null to discard whatever the poll returned.
	 */
	@Nullable
	Message<?> afterReceive(@Nullable Message<?> result, MessageSource<?> source);

}
