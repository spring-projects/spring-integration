/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.integration.util.MessagingMethodInvokerHelper;

/**
 * A MessageListProcessor implementation that invokes a method on a target POJO.
 * 
 * @author Dave Syer
 * @since 2.0
 */
public class MethodInvokingMessageListProcessor<T> extends AbstractExpressionEvaluator {

	private final MessagingMethodInvokerHelper<T> delegate;

	public MethodInvokingMessageListProcessor(Object targetObject, Method method, Class<T> expectedType) {
		delegate = new MessagingMethodInvokerHelper<T>(targetObject, method, expectedType, true);
	}

	public MethodInvokingMessageListProcessor(Object targetObject, Method method) {
		delegate = new MessagingMethodInvokerHelper<T>(targetObject, method, true);
	}

	public MethodInvokingMessageListProcessor(Object targetObject, String methodName, Class<T> expectedType) {
		delegate = new MessagingMethodInvokerHelper<T>(targetObject, methodName,
				expectedType, true);
	}

	public MethodInvokingMessageListProcessor(Object targetObject, String methodName) {
		delegate = new MessagingMethodInvokerHelper<T>(targetObject, methodName, true);
	}

	public MethodInvokingMessageListProcessor(Object targetObject, Class<? extends Annotation> annotationType) {
		delegate = new MessagingMethodInvokerHelper<T>(targetObject, annotationType, Object.class, true);
	}

	public String toString() {
		return delegate.toString();
	}

	public T process(Collection<? extends Message<?>> messages, Map<String, Object> aggregateHeaders) {
		try {
			return delegate.process(new ArrayList<Message<?>>(messages), aggregateHeaders);
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to process message list", e);
		}
	}

}
