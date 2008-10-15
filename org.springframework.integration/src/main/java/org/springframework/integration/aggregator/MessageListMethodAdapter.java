/*
 * Copyright 2002-2008 the original author or authors.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.util.DefaultMethodInvoker;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Base class for implementing adapters for methods which take as an argument a
 * list of {@link Message Message} instances or payloads.
 * 
 * @author Marius Bogoevici
 */
public class MessageListMethodAdapter {

	private final DefaultMethodInvoker invoker;

	protected final Method method;


	public MessageListMethodAdapter(Object object, String methodName) {
		Assert.notNull(object, "'object' must not be null");
		Assert.notNull(methodName, "'methodName' must not be null");
		this.method = ReflectionUtils.findMethod(object.getClass(), methodName, new Class<?>[] { List.class });
		Assert.notNull(this.method, "Method '" + methodName +
				"(List<?> args)' not found on '" + object.getClass().getName() + "'.");
		this.invoker = new DefaultMethodInvoker(object, this.method);
	}

	public MessageListMethodAdapter(Object object, Method method) {
		Assert.notNull(object, "'object' must not be null");
		Assert.notNull(method, "'method' must not be null");
		Assert.isTrue(method.getParameterTypes().length == 1
				&& method.getParameterTypes()[0].equals(List.class),
				"Method must accept exactly one parameter, and it must be a List.");
		this.method = method;
		this.invoker = new DefaultMethodInvoker(object, this.method);
	}


	protected Method getMethod() {
		return method;
	}

	private static boolean isActualTypeParameterizedMessage(Method method) {
		return (getCollectionActualType(method) instanceof ParameterizedType)
				&& Message.class.isAssignableFrom((Class<?>) ((ParameterizedType) getCollectionActualType(method)).getRawType());
	}

	protected final Object executeMethod(List<Message<?>> messages) {
		try {
			if (isMethodParameterParameterized(this.method) && isHavingActualTypeArguments(this.method)
					&& (isActualTypeRawMessage(this.method) || isActualTypeParameterizedMessage(this.method))) {
				return this.invoker.invokeMethod(messages);
			}
			return this.invoker.invokeMethod(extractPayloadsFromMessages(messages));
		}
		catch (InvocationTargetException e) {
			throw new MessagingException(
					"Method '" + this.method + "' threw an Exception.", e.getTargetException());
		}
		catch (Exception e) {
			throw new MessagingException("Failed to invoke method '" + this.method + "'.");
		}
	}

	private List<?> extractPayloadsFromMessages(List<Message<?>> messages) {
		List<Object> payloadList = new ArrayList<Object>();
		for (Message<?> message : messages) {
			payloadList.add(message.getPayload());
		}
		return payloadList;
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

	private static boolean isMethodParameterParameterized(Method method) {
		return method.getGenericParameterTypes().length == 1
				&& method.getGenericParameterTypes()[0] instanceof ParameterizedType;
	}

}
