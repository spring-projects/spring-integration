/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * The {@link MessageProcessor} implementation for method invocation on the single method classes
 * - functional interface implementations.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class LambdaMessageProcessor implements MessageProcessor<Object>, BeanFactoryAware {

	private final Object target;

	private final Method method;

	private final TypeDescriptor payloadType;

	private final Class<?>[] parameterTypes;

	private ConversionService conversionService;

	public LambdaMessageProcessor(Object target, Class<?> payloadType) {
		Assert.notNull(target, "'target' must not be null");
		this.target = target;
		final AtomicReference<Method> methodValue = new AtomicReference<>();
		ReflectionUtils.doWithMethods(target.getClass(),
				methodValue::set,
				methodCandidate -> {
					boolean isCandidate = !methodCandidate.isBridge()
							&& !methodCandidate.isDefault()
							&& methodCandidate.getDeclaringClass() != Object.class
							&& Modifier.isPublic(methodCandidate.getModifiers())
							&& !Modifier.isStatic(methodCandidate.getModifiers());
					if (isCandidate) {
						Assert.isNull(methodValue.get(), "LambdaMessageProcessor is applicable for inline or lambda " +
								"classes with single method - functional interface implementations.");
					}
					return isCandidate;
				});

		Assert.notNull(methodValue.get(), "LambdaMessageProcessor is applicable for inline or lambda " +
				"classes with single method - functional interface implementations.");

		this.method = methodValue.get();
		this.method.setAccessible(true);
		this.parameterTypes = this.method.getParameterTypes();
		this.payloadType = payloadType != null ? TypeDescriptor.valueOf(payloadType) : null;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		ConversionService conversionService = IntegrationUtils.getConversionService(beanFactory);
		if (conversionService == null) {
			conversionService = DefaultConversionService.getSharedInstance();
		}
		this.conversionService = conversionService;
	}

	@Override
	public Object processMessage(Message<?> message) {
		Object[] args = new Object[this.parameterTypes.length];
		for (int i = 0; i < this.parameterTypes.length; i++) {
			Class<?> parameterType = this.parameterTypes[i];
			if (Message.class.isAssignableFrom(parameterType)) {
				args[i] = message;
			}
			else if (Map.class.isAssignableFrom(parameterType)) {
				if (message.getPayload() instanceof Map && this.parameterTypes.length == 1) {
					args[i] = message.getPayload();
				}
				else {
					args[i] = message.getHeaders();
				}
			}
			else {
				if (this.payloadType != null) {
					if (Message.class.isAssignableFrom(this.payloadType.getType())) {
						args[i] = message;
					}
					else {
						args[i] = this.conversionService.convert(message.getPayload(),
								TypeDescriptor.forObject(message.getPayload()), this.payloadType);
					}

				}
				else {
					args[i] = message.getPayload();
				}
			}
		}

		try {
			return this.method.invoke(this.target, args);
		}
		catch (InvocationTargetException e) {
			throw new MessageHandlingException(message, e.getCause());
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, e);
		}
	}

}
