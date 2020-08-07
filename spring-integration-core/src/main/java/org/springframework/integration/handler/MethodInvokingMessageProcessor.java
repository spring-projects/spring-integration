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

package org.springframework.integration.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.handler.support.MessagingMethodInvokerHelper;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * A MessageProcessor implementation that invokes a method on a target Object.
 * The Method instance or method name may be provided as a constructor argument.
 * If a method name is provided, and more than one declared method has that name,
 * the method-selection will be dynamic, based on the underlying SpEL method resolution.
 * Alternatively, an annotation type may be provided so that the candidates for
 * SpEL's method resolution are determined by the presence of that
 * annotation rather than the method name.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
public class MethodInvokingMessageProcessor<T> extends AbstractMessageProcessor<T> implements ManageableLifecycle {

	private final MessagingMethodInvokerHelper delegate;

	public MethodInvokingMessageProcessor(Object targetObject, Method method) {
		this.delegate = new MessagingMethodInvokerHelper(targetObject, method, false);
	}

	public MethodInvokingMessageProcessor(Object targetObject, String methodName) {
		this.delegate = new MessagingMethodInvokerHelper(targetObject, methodName, false);
	}

	public MethodInvokingMessageProcessor(Object targetObject, String methodName, boolean canProcessMessageList) {
		this.delegate = new MessagingMethodInvokerHelper(targetObject, methodName, canProcessMessageList);
	}

	public MethodInvokingMessageProcessor(Object targetObject, Class<? extends Annotation> annotationType) {
		this.delegate = new MessagingMethodInvokerHelper(targetObject, annotationType, false);
	}

	@Override
	public void setConversionService(ConversionService conversionService) {
		super.setConversionService(conversionService);
		this.delegate.setConversionService(conversionService);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
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

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public T processMessage(Message<?> message) {
		try {
			return (T) this.delegate.process(message);
		}
		catch (Exception ex) {
			throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
					() -> "error occurred during processing message in 'MethodInvokingMessageProcessor' [" + this + ']',
					ex);
		}
	}

}
