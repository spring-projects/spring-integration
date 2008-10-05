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

package org.springframework.integration.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Helper methods for detecting Methods.
 * 
 * @author Mark Fisher
 */
public abstract class MethodUtils {

	/**
	 * Find a <em>single</em> Method on the given Class that contains the
	 * specified annotation type.
	 * 
	 * @param clazz the Class instance to check for the annotation
	 * @param annotationType the Method-level annotation type
	 * 
	 * @return a single matching Method instance or <code>null</code> if the
	 * Class contains no Methods with the specified annotation
	 * 
	 * @throws IllegalArgumentException if more than one Method has the
	 * specified annotation
	 */
	public static <T extends Annotation> Method findMethodWithAnnotation(
			final Class<?> clazz, final Class<T> annotationType) {
		final AtomicReference<Method> annotatedMethod = new AtomicReference<Method>();
		ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				T annotation = AnnotationUtils.findAnnotation(method, annotationType);
				if (annotation != null) {
					Assert.isNull(annotatedMethod.get(), "found more than one method on target class ["
							+ clazz + "] with the annotation type [" + annotationType + "]");
					annotatedMethod.set(method);
				}
			}
		});
		return annotatedMethod.get();
	}

	/**
	 * Find all public Methods of a given Class.
	 * 
	 * @param clazz the class to search
	 * @param includeMethodsDeclaredOnObject whether to include Methods
	 * that are declared on the Object class
	 * 
	 * @return array of public Methods
	 */
	public static Method[] findPublicMethods(
			final Class<?> clazz, final boolean includeMethodsDeclaredOnObject) {
		final List<Method> methods = new ArrayList<Method>();
		for (Method method : clazz.getMethods()) {
			if (includeMethodsDeclaredOnObject
					|| !method.getDeclaringClass().equals(Object.class)) {
				methods.add(method);
			}
		}
		return methods.toArray(new Method[methods.size()]);
	}

}
