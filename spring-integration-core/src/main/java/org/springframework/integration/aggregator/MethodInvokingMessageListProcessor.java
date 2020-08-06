/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.handler.support.MessagingMethodInvokerHelper;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;

/**
 * A MessageListProcessor implementation that invokes a method on a target POJO.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
public class MethodInvokingMessageListProcessor<T> extends AbstractExpressionEvaluator
		implements ManageableLifecycle {

	private final MessagingMethodInvokerHelper delegate;

	public MethodInvokingMessageListProcessor(Object targetObject, Method method, Class<T> expectedType) {
		this.delegate = new MessagingMethodInvokerHelper(targetObject, method, expectedType, true);
	}

	public MethodInvokingMessageListProcessor(Object targetObject, Method method) {
		this.delegate = new MessagingMethodInvokerHelper(targetObject, method, true);
	}

	public MethodInvokingMessageListProcessor(Object targetObject, String methodName, Class<T> expectedType) {
		this.delegate = new MessagingMethodInvokerHelper(targetObject, methodName,
				expectedType, true);
	}

	public MethodInvokingMessageListProcessor(Object targetObject, String methodName) {
		this.delegate = new MessagingMethodInvokerHelper(targetObject, methodName, true);
	}

	public MethodInvokingMessageListProcessor(Object targetObject, Class<? extends Annotation> annotationType) {
		this.delegate = new MessagingMethodInvokerHelper(targetObject, annotationType, Object.class, true);
	}

	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		this.delegate.setBeanFactory(beanFactory);
	}

	/**
	 * A {@code boolean} flag to use SpEL Expression evaluation or
	 * {@link org.springframework.messaging.handler.invocation.InvocableHandlerMethod}
	 * for target method invocation.
	 * @param useSpelInvoker to use SpEL Expression evaluation or not.
	 * @since 5.0
	 */
	public void setUseSpelInvoker(boolean useSpelInvoker) {
		this.delegate.setUseSpelInvoker(useSpelInvoker);
	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}

	@SuppressWarnings("unchecked")
	public T process(Collection<Message<?>> messages, Map<String, Object> aggregateHeaders) {
		return (T) this.delegate.process(messages, aggregateHeaders);
	}

	@Override
	public void start() {
		this.delegate.start();
	}

	@Override
	public void stop() {
		this.delegate.stop();
	}

	@Override
	public boolean isRunning() {
		return this.delegate.isRunning();
	}

}
