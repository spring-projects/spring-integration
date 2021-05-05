/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * The {@link MessageProcessor} implementation for method invocation on the single method classes
 * - functional interface implementations.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class LambdaMessageProcessor implements MessageProcessor<Object>, BeanFactoryAware {

	private static final Log LOGGER = LogFactory.getLog(LambdaMessageProcessor.class);

	private final Object target;

	private final Method method;

	private final Class<?> payloadType;

	private final Class<?>[] parameterTypes;

	private MessageConverter messageConverter;

	public LambdaMessageProcessor(Object target, Class<?> payloadType) {
		Assert.notNull(target, "'target' must not be null");
		this.target = target;

		Set<Method> methods =
				MethodIntrospector.selectMethods(target.getClass(),
						(ReflectionUtils.MethodFilter) methodCandidate ->
								methodCandidate.getDeclaringClass() != Object.class &&
										!methodCandidate.getDeclaringClass().getName()
												.equals("kotlin.jvm.internal.Lambda") &&
										!methodCandidate.isDefault() &&
										!Modifier.isStatic(methodCandidate.getModifiers()));

		Assert.state(methods.size() == 1,
				"LambdaMessageProcessor is applicable for inline or lambda " +
						"classes with single method - functional interface implementations.");

		this.method = methods.iterator().next();
		this.method.setAccessible(true);
		this.parameterTypes = this.method.getParameterTypes();
		this.payloadType = payloadType;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.messageConverter =
				beanFactory.getBean(IntegrationContextUtils.ARGUMENT_RESOLVER_MESSAGE_CONVERTER_BEAN_NAME,
						MessageConverter.class);
	}

	@Override
	public Object processMessage(Message<?> message) {
		Object[] args = buildArgs(message);

		try {
			Object result = this.method.invoke(this.target, args);
			if (result != null && org.springframework.integration.util.ClassUtils.isKotlinUnit(result.getClass())) {
				result = null;
			}
			return result;
		}
		catch (InvocationTargetException e) {
			final Throwable cause = e.getCause();
			if (e.getTargetException() instanceof ClassCastException) {
				LOGGER.error("Could not invoke the method '" + this.method + "' due to a class cast exception, " +
						"if using a lambda in the DSL, consider using an overloaded EIP method " +
						"that takes a Class<?> argument to explicitly  specify the type. " +
						"An example of when this often occurs is if the lambda is configured to " +
						"receive a Message<?> argument.", cause);
			}
			if (cause instanceof RuntimeException) { // NOSONAR
				throw (RuntimeException) cause;
			}
			throw new IllegalStateException(// NOSONAR lost stack trace
					"Could not invoke the method '" + this.method + "'", cause);
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"error occurred during processing message in 'LambdaMessageProcessor' for method [" +
							this.method + "]", e);
		}
	}

	private Object[] buildArgs(Message<?> message) {
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
				if (this.payloadType != null &&
						!ClassUtils.isAssignable(this.payloadType, message.getPayload().getClass())) {
					if (Message.class.isAssignableFrom(this.payloadType)) {
						args[i] = message;
					}
					else {
						args[i] = this.messageConverter.fromMessage(message, this.payloadType);
					}
				}
				else {
					args[i] = message.getPayload();
				}
			}
		}
		return args;
	}

}
