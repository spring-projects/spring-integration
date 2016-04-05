/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.integration.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.Message;

/**
 * Advice for a {@link MessageSource#receive()} method to decide whether a poll
 * should be ignored and/or take action after the receive.
 *
 * @author Gary Russell
 * @since 4.2
 */
public abstract class AbstractMessageSourceAdvice implements MethodInterceptor {

	@Override
	public final Object invoke(MethodInvocation invocation) throws Throwable {
		Object target = invocation.getThis();
		if (!(target instanceof MessageSource)
				|| !invocation.getMethod().getName().equals("receive")) {
			return invocation.proceed();
		}

		Message<?> result = null;
		if (beforeReceive((MessageSource<?>) target)) {
			result = (Message<?>) invocation.proceed();
		}
		return afterReceive(result, (MessageSource<?>) target);
	}

	/**
	 * Subclasses can decide whether to proceed with this poll.
	 * @param source the message source.
	 * @return true to proceed.
	 */
	public abstract boolean beforeReceive(MessageSource<?> source);

	/**
	 * Subclasses can take actions based on the result of the poll; e.g.
	 * adjust the {@code trigger}. The message can also be replaced with a new one.
	 * @param result the received message.
	 * @param source the message source.
	 * @return a message to continue to process the result, null to discard whatever the poll returned.
	 */
	public abstract Message<?> afterReceive(Message<?> result, MessageSource<?> source);

}
