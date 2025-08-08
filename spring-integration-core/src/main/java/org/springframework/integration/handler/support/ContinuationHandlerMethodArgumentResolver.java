/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.handler.support;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.integration.util.CoroutinesUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * No-op resolver for method arguments of type {@link kotlin.coroutines.Continuation}.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class ContinuationHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return CoroutinesUtils.isContinuationType(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		return Mono.empty();
	}

}
