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

import java.util.Map;
import java.util.Properties;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * A {@link HandlerMethodArgumentResolver} implementation to resolve argument
 * for the {@link MethodParameter} as a {@link Map} or {@link Properties}.
 * <p>
 * The {@link Message#getHeaders()} is used when {@link MethodParameter} is marked
 * with the {@link Headers} annotation or {@link Message#getPayload()} isn't {@link Map}
 * or {@link Properties} compatible.
 * <p>
 * If {@link MethodParameter} is of {@link Properties} type and {@link Message#getPayload()}
 * is a {@link String} containing {@code =} symbol, the {@link MapArgumentResolver} uses
 * the {@link org.springframework.core.convert.ConversionService} trying to convert that
 * {@link String} to a {@link Properties} object.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class MapArgumentResolver extends AbstractExpressionEvaluator
		implements HandlerMethodArgumentResolver {

	private static final TypeDescriptor PROPERTIES_TYPE = TypeDescriptor.valueOf(Properties.class);

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return !parameter.hasParameterAnnotation(Payload.class)
				&& Map.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object resolveArgument(MethodParameter parameter, Message<?> message) {
		Object payload = message.getPayload();
		if (Properties.class.isAssignableFrom(parameter.getParameterType())) {
			Map<String, Object> map = message.getHeaders();
			if (!parameter.hasParameterAnnotation(Headers.class)) {
				if (payload instanceof Map) {
					map = (Map<String, Object>) payload;
				}
				else if (payload instanceof String && ((String) payload).contains("=")) {
					return getEvaluationContext()
							.getTypeConverter()
							.convertValue(payload, TypeDescriptor.valueOf(String.class), PROPERTIES_TYPE);
				}
			}
			Properties properties = new Properties();
			properties.putAll(map);
			return properties;
		}
		else {
			if (!parameter.hasParameterAnnotation(Headers.class) && payload instanceof Map) {
				return payload;
			}
			else {
				return message.getHeaders();
			}
		}

	}

}
