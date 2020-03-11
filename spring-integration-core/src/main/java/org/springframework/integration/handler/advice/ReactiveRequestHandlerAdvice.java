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

package org.springframework.integration.handler.advice;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import reactor.core.publisher.Mono;

/**
 * A {@link MethodInterceptor} for message handlers producing a {@link Mono} as a payload for reply.
 * The returned {@link Mono} is customized via {@link Mono#transform(java.util.function.Function)} operator
 * calling provided {@code replyCustomizer} {@link BiFunction} with request message as a context.
 *
 * A customization assumes to use supporting reactive operators like {@link Mono#timeout},
 * {@link Mono#retry}, {@link Mono#tag} etc.
 * A {@link Mono#transform(java.util.function.Function)}  also can be used
 * for further customization like reactive circuit breaker.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class ReactiveRequestHandlerAdvice implements MethodInterceptor {

	private static final Log LOGGER = LogFactory.getLog(ReactiveRequestHandlerAdvice.class);

	private final BiFunction<Message<?>, Mono<?>, Publisher<?>> replyCustomizer;

	/**
	 * Instantiate advice based on a provided {@link BiFunction} customizer.
	 * @param replyCustomizer the {@link BiFunction} to customize produced {@link Mono}.
	 */
	public ReactiveRequestHandlerAdvice(BiFunction<Message<?>, Mono<?>, Publisher<?>> replyCustomizer) {
		Assert.notNull(replyCustomizer, "'replyCustomizer' must not be null");
		this.replyCustomizer = replyCustomizer;
	}

	@Override
	public final Object invoke(MethodInvocation invocation) throws Throwable {
		Object result = invocation.proceed();

		Method method = invocation.getMethod();
		Object invocationThis = invocation.getThis();
		Object[] arguments = invocation.getArguments();
		boolean isReactiveMethod =
				method.getName().equals("handleRequestMessage") &&
						(arguments.length == 1 && arguments[0] instanceof Message) &&
						result instanceof Mono<?>;
		if (!isReactiveMethod) {
			if (LOGGER.isWarnEnabled()) {
				String clazzName =
						invocationThis == null
								? method.getDeclaringClass().getName()
								: invocationThis.getClass().getName();
				LOGGER.warn("This advice " + getClass().getName() +
						" can only be used for MessageHandlers with reactive reply; an attempt to advise method '"
						+ method.getName() + "' in '" + clazzName + "' is ignored.");
			}
			return result;
		}

		Mono<?> replyMono = (Mono<?>) result;

		Message<?> requestMessage = (Message<?>) arguments[0];

		return replyMono
				.transform(mono -> this.replyCustomizer.apply(requestMessage, mono));
	}

}
