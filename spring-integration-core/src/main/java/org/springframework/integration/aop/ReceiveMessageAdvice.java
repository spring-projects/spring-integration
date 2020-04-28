/*
 * Copyright 2020 the original author or authors.
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
