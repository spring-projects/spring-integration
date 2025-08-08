/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.integration.core.MessageSource;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

/**
 * An AOP advice to perform hooks before and/or after a {@code receive()} contract is called.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
@FunctionalInterface
public interface ReceiveMessageAdvice extends MethodInterceptor {

	/**
	 * Subclasses can decide whether to {@link MethodInvocation#proceed()} or not.
	 * @param source the source of the message to receive.
	 * @return true to proceed (default).
	 */
	default boolean beforeReceive(Object source) {
		return true;
	}

	@Override
	@Nullable
	default Object invoke(MethodInvocation invocation) throws Throwable {
		Object target = invocation.getThis();
		if (!(target instanceof MessageSource) && !(target instanceof PollableChannel)) {
			return invocation.proceed();
		}

		Message<?> result = null;
		if (beforeReceive(target)) {
			result = (Message<?>) invocation.proceed();
		}
		return afterReceive(result, target);
	}

	/**
	 * Subclasses can take actions based on the result of the {@link MethodInvocation#proceed()}; e.g.
	 * adjust the {@code trigger}. The message can also be replaced with a new one.
	 * @param result the received message.
	 * @param source the source of the message to receive.
	 * @return a message to continue to process the result, null to discard whatever
	 * the {@link MethodInvocation#proceed()} returned.
	 */
	@Nullable
	Message<?> afterReceive(@Nullable Message<?> result, Object source);

}
