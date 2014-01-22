/*
 * Copyright 2002-2014 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.util.MessagingMethodInvokerHelper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;

/**
 * A MessageProcessor implementation that invokes a method on a target Object. The Method instance or method name may be
 * provided as a constructor argument. If a method name is provided, and more than one declared method has that name,
 * the method-selection will be dynamic, based on the underlying SpEL method resolution. Alternatively, an annotation
 * type may be provided so that the candidates for SpEL's method resolution are determined by the presence of that
 * annotation rather than the method name.
 *
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MethodInvokingMessageProcessor<T> extends AbstractMessageProcessor<T> {

	private final MessagingMethodInvokerHelper<T> delegate;

	public MethodInvokingMessageProcessor(Object targetObject, Method method) {
		delegate = new MessagingMethodInvokerHelper<T>(targetObject, method, false);
	}

	public MethodInvokingMessageProcessor(Object targetObject, String methodName) {
		delegate = new MessagingMethodInvokerHelper<T>(targetObject, methodName, false);
	}

	public MethodInvokingMessageProcessor(Object targetObject, String methodName, boolean canProcessMessageList) {
		delegate = new MessagingMethodInvokerHelper<T>(targetObject, methodName, canProcessMessageList);
	}

	public MethodInvokingMessageProcessor(Object targetObject, Class<? extends Annotation> annotationType) {
		delegate = new MessagingMethodInvokerHelper<T>(targetObject, annotationType, false);
	}

	@Override
	public void setConversionService(ConversionService conversionService) {
		super.setConversionService(conversionService);
		delegate.setConversionService(conversionService);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		delegate.setBeanFactory(beanFactory);
	}

	@Override
	public T processMessage(Message<?> message) {
		try {
			return delegate.process(message);
		}
		catch (Exception e) {
			throw new MessageHandlingException(message, e);
		}
	}

}
