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

package org.springframework.integration.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.integration.core.Message;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * An implementation of {@link HandlerMethodResolver} that matches the payload
 * type of the Message against the expected type of its candidate methods.
 * 
 * @author Mark Fisher
 */
public class PayloadTypeMatchingHandlerMethodResolver implements HandlerMethodResolver {

	private final Map<Class<?>, Method> methodMap = new HashMap<Class<?>, Method>();

	private volatile Method fallbackMethod;


	public PayloadTypeMatchingHandlerMethodResolver(Method... candidates) {
		Assert.notEmpty(candidates, "candidates must not be empty");
		this.initMethodMap(candidates);
	}


	public Method resolveHandlerMethod(Message<?> message) {
		Method method = this.methodMap.get(message.getClass());
		if (method == null) {
			Class<?> payloadType = message.getPayload().getClass();
			method = this.methodMap.get(payloadType);
			if (method == null) {
				method = this.findClosestMatch(payloadType);
			}
			if (method == null) {
				method = this.fallbackMethod;
			}
		}
		return method;
	}

	private void initMethodMap(Method[] candidates) {
		for (Method method : candidates) {
			Class<?> expectedType = this.determineExpectedType(method);
			if (expectedType == null) {
				Assert.isTrue(fallbackMethod == null,
						"At most one method can expect only Message headers rather than a Message or payload, " +
						"but found two: [" + method + "] and [" + this.fallbackMethod + "]");
				this.fallbackMethod = method;
			}
			else {
				Assert.isTrue(!this.methodMap.containsKey(expectedType),
						"More than one method matches type [" + expectedType +
						"]. Consider using annotations or providing a method name.");
				this.methodMap.put(expectedType, method);
			}
		}
	}

	private Class<?> determineExpectedType(Method method) {
		Class<?> expectedType = null;
		Type[] parameterTypes = method.getGenericParameterTypes();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		for (int i = 0; i < parameterTypes.length; i++) {
			if (!HandlerMethodUtils.containsHeaderAnnotation(parameterAnnotations[i])) {
				Assert.isTrue(expectedType == null,
						"Message-handling method must only have one parameter expecting a Message or Message payload."
						+ " Other parameters may be included but only if they have @Header or @Headers annotations.");
				Type parameterType = parameterTypes[i];
				if (parameterType instanceof ParameterizedType) {
					ParameterizedType parameterizedType = (ParameterizedType) parameterType;
					Type rawType = parameterizedType.getRawType();
					if (rawType instanceof Class) {
						Class<?> rawTypeClass = (Class<?>) rawType;
						if (Message.class.isAssignableFrom(rawTypeClass)) {
							expectedType = this.determineExpectedTypeFromParameterizedMessageType(parameterizedType);
						}
						else {
							expectedType = rawTypeClass;
						}
					}
				}
				else if (parameterType instanceof Class) {
					expectedType = (Class<?>) parameterType;
				}
				Assert.notNull(expectedType, "Failed to determine expected type for parameter ["
						+ parameterType + "] on Method [" + method + "]");
			}
		}
		return expectedType;
	}

	private Method findClosestMatch(Class<?> payloadType) {
		Set<Class<?>> expectedTypes = this.methodMap.keySet();
		int minTypeDiffWeight = Integer.MAX_VALUE;
		Method matchingMethod = null;
		for (Class<?> expectedType : expectedTypes) {
			int typeDiffWeight = getTypeDifferenceWeight(expectedType, payloadType);
			if (typeDiffWeight < minTypeDiffWeight) {
				minTypeDiffWeight = typeDiffWeight;
				matchingMethod = this.methodMap.get(expectedType);
			}
		}
		if (matchingMethod != null) {
			this.methodMap.put(payloadType, matchingMethod);
		}
		return matchingMethod;
	}

	private Class<?> determineExpectedTypeFromParameterizedMessageType(ParameterizedType parameterizedType) {
		Class<?> expectedType = null;
		Type actualType = parameterizedType.getActualTypeArguments()[0];
		if (actualType instanceof WildcardType) {
			WildcardType wildcardType = (WildcardType) actualType;
			if (wildcardType.getUpperBounds().length == 1) {
				Type upperBound = wildcardType.getUpperBounds()[0];
				if (upperBound instanceof Class) {
					expectedType = (Class<?>) upperBound;
				}
			}
		}
		else if (actualType instanceof Class) {
			expectedType = (Class<?>) actualType;
		}
		return expectedType;
	}

	private int getTypeDifferenceWeight(Class<?> expectedType, Class<?> payloadType) {
		int result = 0;
		if (!ClassUtils.isAssignable(expectedType, payloadType)) {
			return Integer.MAX_VALUE;
		}
		Class<?> superClass = payloadType.getSuperclass();
		while (superClass != null) {
			if (expectedType.equals(superClass)) {
				result = result + 2;
				superClass = null;
			}
			else if (ClassUtils.isAssignable(expectedType, superClass)) {
				result = result + 2;
				superClass = superClass.getSuperclass();
			}
			else {
				superClass = null;
			}
		}
		if (expectedType.isInterface()) {
			result = result + 1;
		}
		return result;
	}

}
