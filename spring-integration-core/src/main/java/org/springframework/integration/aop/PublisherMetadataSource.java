/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aop;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Strategy for determining the channel name, payload expression, and header expressions
 * for the {@link MessagePublishingInterceptor}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
interface PublisherMetadataSource {

	String METHOD_NAME_VARIABLE_NAME = "method";

	String ARGUMENT_MAP_VARIABLE_NAME = "args";

	String RETURN_VALUE_VARIABLE_NAME = "return";

	String EXCEPTION_VARIABLE_NAME = "exception";

	ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	Expression RETURN_VALUE_EXPRESSION =
			EXPRESSION_PARSER.parseExpression("#" + PublisherMetadataSource.RETURN_VALUE_VARIABLE_NAME);

	/**
	 * Returns the channel name to which Messages should be published
	 * for this particular method invocation.
	 *
	 * @param method The Method.
	 * @return The channel name.
	 */
	String getChannelName(Method method);

	/**
	 * Returns the SpEL expression to be evaluated for creating the Message
	 * payload.
	 * @param method the Method.
	 * @return rhe payload expression.
	 * @since 5.0.4
	 */
	Expression getExpressionForPayload(Method method);

	/**
	 * Returns the map of expression strings to be evaluated for any headers
	 * that should be set on the published Message. The keys in the Map are
	 * header names, the values are the expression strings.
	 * @param method The Method.
	 * @return The header expressions.
	 */
	Map<String, Expression> getExpressionsForHeaders(Method method);

}
