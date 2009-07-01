/*
 * Copyright 2002-2009 the original author or authors.
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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;

/**
 * Utility methods for common behavior related to Message-handling methods.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
abstract class HandlerMethodUtils {

	/**
	 * Verifies that the provided Method is capable of handling Messages.
	 * It must be public, and not defined directly on Object. If it expects
	 * more than one parameter, at most one of them may expect the payload
	 * object. Others must be annotated for accepting Message header values.
	 */
	public static boolean isValidHandlerMethod(Method method) {
		if (method.getDeclaringClass().equals(Object.class)) {
			return false;
		}
		if (!Modifier.isPublic(method.getModifiers())) {
			return false;
		}
		Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length > 1) {
			// at most one parameter can be expecting a Message or payload.
			boolean foundPayloadParam = false;
			Annotation[][] allParamAnnotations = method.getParameterAnnotations();
			for (int i = 0; i < parameterTypes.length; i++) {
				Class<?> currentType = parameterTypes[i];
				Annotation[] paramAnnotations = allParamAnnotations[i];
				// it's possible that a Map is the payload type at runtime, but we can't know that here
				if (!containsHeaderAnnotation(paramAnnotations) && !Map.class.isAssignableFrom(currentType)) {
					if (foundPayloadParam) {
						return false;
					}
					foundPayloadParam = true;
				}
			}
		}
		return true;
	}

	public static Method[] getCandidateHandlerMethods(Object object) {
		final List<Method> candidates = new ArrayList<Method>();
		Class<?> clazz = AopUtils.getTargetClass(object);
		if (clazz == null) {
			clazz = object.getClass();
		}
		for (Method method : clazz.getMethods()) {
			if (HandlerMethodUtils.isValidHandlerMethod(method)) {
				candidates.add(method);
			}
		}
		return candidates.toArray(new Method[candidates.size()]);
	}

	/**
	 * Checks the array of Annotations for a method parameter to see if either
	 * the @Header or @Headers annotation is present.
	 */
	public static boolean containsHeaderAnnotation(Annotation[] parameterAnnotations) {
		for (Annotation annotation : parameterAnnotations) {
			if (annotation.annotationType().equals(Header.class)
					|| annotation.annotationType().equals(Headers.class)) {
				return true;
			}
		}
		return false;
	}

}
