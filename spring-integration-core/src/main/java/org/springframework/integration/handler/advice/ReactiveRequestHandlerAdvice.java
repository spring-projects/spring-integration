/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.handler.advice;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

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
