/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.handler.support;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.Expression;
import org.springframework.integration.annotation.Payloads;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The {@link HandlerMethodArgumentResolver} for resolving a {@link Collection}
 * of {@code payloads} or expression against each {@code payload}.
 * <p>
 * IMPORTANT: The {@link Message} for argument resolution must contain a {@code payload}
 * as {@link Collection} of {@link Message}s.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class PayloadsArgumentResolver extends AbstractExpressionEvaluator
		implements HandlerMethodArgumentResolver {

	private final Map<MethodParameter, Expression> expressionCache = new HashMap<>();

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(Payloads.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		Object payload = message.getPayload();
		Assert.state(payload instanceof Collection,
				"This Argument Resolver support only messages with payload as Collection<Message<?>>");
		Collection<Message<?>> messages = (Collection<Message<?>>) payload;

		if (!this.expressionCache.containsKey(parameter)) {
			Payloads payloads = parameter.getParameterAnnotation(Payloads.class);
			String expression = payloads.value(); // NOSONAR never null - supportsParameter()
			if (StringUtils.hasText(expression)) {
				this.expressionCache.put(parameter, EXPRESSION_PARSER.parseExpression("![payload." + expression + "]"));
			}
			else {
				this.expressionCache.put(parameter, null);
			}

		}

		Expression expression = this.expressionCache.get(parameter);
		if (expression != null) {
			return evaluateExpression(expression, messages, parameter.getParameterType());
		}
		else {
			try (Stream<Message<?>> messageStream = messages.stream()) {
				List<?> payloads = messageStream
						.map(Message::getPayload)
						.collect(Collectors.toList());
				return getEvaluationContext()
						.getTypeConverter()
						.convertValue(payloads,
								TypeDescriptor.forObject(payloads),
								TypeDescriptor.valueOf(parameter.getParameterType()));
			}

		}
	}

}
