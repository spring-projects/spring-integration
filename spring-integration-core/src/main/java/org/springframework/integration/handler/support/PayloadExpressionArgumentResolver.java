/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.handler.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.expression.Expression;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.StringUtils;

/**
 * The {@link HandlerMethodArgumentResolver} for evaluating {@link Payload#expression()}
 * as a SpEL expression against {@code message} and converting result to expected parameter type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 *
 * @see org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver
 */
public class PayloadExpressionArgumentResolver extends AbstractExpressionEvaluator
		implements HandlerMethodArgumentResolver {

	private final Map<MethodParameter, Expression> expressionCache = new HashMap<>();

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Payload ann = parameter.getParameterAnnotation(Payload.class);
		return ann != null && StringUtils.hasText(ann.expression());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		Expression expression = this.expressionCache.get(parameter);
		if (expression == null) {
			Payload ann = parameter.getParameterAnnotation(Payload.class);
			expression = EXPRESSION_PARSER.parseExpression(ann.expression()); // NOSONAR never null - supportsParameter()
			this.expressionCache.put(parameter, expression);
		}
		return evaluateExpression(expression, message.getPayload(), parameter.getParameterType());
	}

}
