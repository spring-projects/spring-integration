/*
 * Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.integration.security.context;

import java.util.Deque;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The AOP {@link MethodInterceptor} implementation to clean up {@link SecurityContext}
 * in the current {@link Thread} after the advised method invocation.
 * <p>
 * This {@link org.aopalliance.aop.Advice} makes sense in those cases, when
 * {@link SecurityContextCleanupChannelInterceptor} doesn't fit and can't reach its
 * purpose. For example we have an {@code <outbound-channel-adapter>} on the
 * {@link org.springframework.integration.channel.QueueChannel} and the last one propagates
 * {@link SecurityContext} for {@link org.springframework.scheduling.TaskScheduler}'s thread.
 * Having that we can't clean up {@link SecurityContext} after the service invocation neither
 * automatically, nor with {@link SecurityContextCleanupChannelInterceptor}.
 * The {@code <outbound-channel-adapter>} is the last operation in the current call stack.
 * <p>
 * Adding {@link SecurityContextCleanupAdvice} to the {@code <request-handler-advice-chain>}
 * of that {@code <outbound-channel-adapter>} provide for us the feature to achieve
 * {@link SecurityContext} clean up requirements.
 *
 * @author Artem Bilan
 * @since 4.2
 *
 * @see SecurityContextHolder
 * @see SecurityContextCleanupChannelInterceptor
 * @see SecurityContextPropagationChannelInterceptor
 */
public class SecurityContextCleanupAdvice implements MethodInterceptor {

	private final static SecurityContext EMPTY_CONTEXT = SecurityContextHolder.createEmptyContext();

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		try {
			return invocation.proceed();
		}
		finally {
			cleanup();
		}
	}

	public static void cleanup() {
		Deque<SecurityContext> contextStack = SecurityContextPropagationChannelInterceptor.ORIGINAL_CONTEXT.get();

		if (contextStack == null || contextStack.isEmpty()) {
			SecurityContextHolder.clearContext();
			SecurityContextPropagationChannelInterceptor.ORIGINAL_CONTEXT.remove();
			return;
		}

		SecurityContext originalContext = contextStack.pop();

		try {
			if (EMPTY_CONTEXT.equals(originalContext)) {
				SecurityContextHolder.clearContext();
				SecurityContextPropagationChannelInterceptor.ORIGINAL_CONTEXT.remove();
			}
			else {
				SecurityContextHolder.setContext(originalContext);
			}
		}
		catch (Throwable t) {
			SecurityContextHolder.clearContext();
		}
	}

}
