/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * Helper to provide common features for inspecting objects and locating annotated methods.
 *
 * @author Dave Syer
 * @author Gunnar Hillert
 * @author Soby Chacko
 *
 */
abstract class AnnotationFinder {

	public static Method findAnnotatedMethod(Object target, final Class<? extends Annotation> annotationType) {
		final AtomicReference<Method> reference = new AtomicReference<Method>();
		ReflectionUtils.doWithMethods(getTargetClass(target), new MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				if (AnnotationUtils.findAnnotation(method, annotationType) != null) {
					reference.set(method);
				}
			}
		});
		return reference.get();
	}

	private static Class<?> getTargetClass(Object targetObject) {
		Class<?> targetClass = targetObject.getClass();
		if (AopUtils.isAopProxy(targetObject)) {
			targetClass = AopUtils.getTargetClass(targetObject);
		}
		else if (ClassUtils.isCglibProxyClass(targetClass)) {
			Class<?> superClass = targetObject.getClass().getSuperclass();
			if (!Object.class.equals(superClass)) {
				targetClass = superClass;
			}
		}
		return targetClass;
	}

}
