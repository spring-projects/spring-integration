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
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;

/**
 * A {@link HandlerMethodArgumentResolver} implementation for {@link Collection},
 * {@link Iterator} or {@code array} {@link MethodParameter}.
 * <p>
 * If {@link #canProcessMessageList} is set to {@code true}, only messages
 * with a payload of {@code Collection<Message<?>>} are supported.
 * Depending on the {@link MethodParameter#getNestedParameterType()} the whole
 * {@code Collection<Message<?>>} or just payloads of those messages can be use as an actual argument.
 * <p>
 * If the value isn't compatible with {@link MethodParameter},
 * the {@link org.springframework.core.convert.ConversionService} is used
 * to convert the value to the target type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class CollectionArgumentResolver extends AbstractExpressionEvaluator
		implements HandlerMethodArgumentResolver {

	private final boolean canProcessMessageList;

	public CollectionArgumentResolver(boolean canProcessMessageList) {
		this.canProcessMessageList = canProcessMessageList;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> parameterType = parameter.getParameterType();
		return Collection.class.isAssignableFrom(parameterType)
				|| Iterator.class.isAssignableFrom(parameterType)
				|| parameterType.isArray();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		Object value = message.getPayload();

		if (this.canProcessMessageList) {
			Assert.state(value instanceof Collection,
					"This Argument Resolver only supports messages with a payload of Collection<Message<?>>, "
							+ "payload is: " + value.getClass());

			Collection<Message<?>> messages = (Collection<Message<?>>) value;

			if (!Message.class.isAssignableFrom(parameter.nested().getNestedParameterType())) {
				try (Stream<Message<?>> messageStream = messages.stream()) {
					value = messageStream
							.map(Message::getPayload)
							.collect(Collectors.toList());
				}
			}
		}

		if (Iterator.class.isAssignableFrom(parameter.getParameterType())) {
			if (value instanceof Iterable) {
				return ((Iterable<?>) value).iterator();
			}
			else {
				return Collections.singleton(value).iterator();
			}
		}
		else {
			return getEvaluationContext()
					.getTypeConverter()
					.convertValue(value,
							TypeDescriptor.forObject(value),
							TypeDescriptor.valueOf(parameter.getParameterType()));
		}
	}

}
