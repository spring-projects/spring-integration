/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.router;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.util.SimpleMethodInvoker;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Aggregator adapter for methods used through <aggregator
 * ref="beanReference"method="methodName"/>
 * 
 * @author Marius Bogoevici
 */
public class AggregatorAdapter implements Aggregator {

	private final SimpleMethodInvoker<Object> invoker;

	private final Method method;

	public AggregatorAdapter(Object object, String methodName) {
		Assert.notNull(object, "'object' must not be null");
		Assert.notNull(methodName, "'methodName' must not be null.");
		this.method = ReflectionUtils.findMethod(object.getClass(), methodName, new Class<?>[] { Collection.class });
		Assert
				.notNull(this.method, "Method '" + methodName + "'(Collection<?> args) not found on "
						+ object.getClass());
		this.invoker = new SimpleMethodInvoker<Object>(object, method.getName());
	}

	public AggregatorAdapter(Object object, Method method) {
		Assert.notNull(object, "'object' must not be null");
		Assert.notNull(method, "'method' must not be null.");
		assertMethodCanBeAdapted();
		this.method = method;
		this.invoker = new SimpleMethodInvoker<Object>(object, method.getName());
	}

	private void assertMethodCanBeAdapted() {
		if (this.method.getParameterTypes().length != 1 && this.method.getParameterTypes()[0].equals(Collection.class)) {
			throw new MessagingConfigurationException(
					"Aggregator method must accept exactly one parameter, and it must be a Collection.");
		}
	}

	public Message<?> aggregate(Collection<Message<?>> messages) {
		Object returnedValue = null;

		if (isMethodParameterParametrized(method) && isHavingActualTypeArguments(method)
				&& (isActualTypeRawMessage(method) || isActualTypeParametrizedMessage(method))) {
			returnedValue = invoker.invokeMethod(messages);
		}
		else {
			returnedValue = invoker.invokeMethod(extractPayloadsFromMessages(messages));
		}
		if (returnedValue == null) {
			return null;
		}
		else if (returnedValue instanceof Message) {
			return (Message<?>) returnedValue;
		}
		else {
			return new GenericMessage<Object>(returnedValue);
		}
	}

	private static boolean isActualTypeParametrizedMessage(Method method) {
		return getCollectionActualType(method) instanceof ParameterizedType
				&& Message.class.isAssignableFrom((Class<?>) ((ParameterizedType) getCollectionActualType(method))
						.getRawType());
	}

	private static boolean isActualTypeRawMessage(Method method) {
		return getCollectionActualType(method).equals(Message.class);
	}

	private static Type getCollectionActualType(Method method) {
		return ((ParameterizedType) method.getGenericParameterTypes()[0]).getActualTypeArguments()[0];
	}

	private static boolean isHavingActualTypeArguments(Method method) {
		return ((ParameterizedType) method.getGenericParameterTypes()[0]).getActualTypeArguments().length == 1;
	}

	private static boolean isMethodParameterParametrized(Method method) {
		return method.getGenericParameterTypes().length == 1
				&& method.getGenericParameterTypes()[0] instanceof ParameterizedType;
	}

	private Collection<?> extractPayloadsFromMessages(Collection<Message<?>> messages) {
		ArrayList payloadList = new ArrayList<Object>();
		for (Message<?> message : messages) {
			payloadList.add(message.getPayload());
		}
		return payloadList;
	}

}
