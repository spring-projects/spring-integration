/*
 * Copyright 2002-2024 the original author or authors.
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
