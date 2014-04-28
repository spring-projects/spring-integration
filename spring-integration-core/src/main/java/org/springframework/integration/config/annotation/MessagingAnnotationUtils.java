/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.config.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods to support annotation processing.
 *
 * @author Gary Russell
 * @author Dave Syer
 * @author Gunnar Hillert
 * @author Soby Chacko
 * @author Artem Bilan
 * @since 4.0
 */
public final class MessagingAnnotationUtils {

	/**
	 * Get the attribute value from the annotation hierarchy, returning the first non-empty
	 * value closest to the annotated method. While traversing up the hierarchy, for string-valued
	 * attributes, an empty string is ignored. For array-valued attributes, an empty
	 * array is ignored.
	 * The overridden attribute must be the same type.
	 * @param annotations The meta-annotations in order (closest first).
	 * @param name The attribute name.
	 * @param requiredType The expected type.
	 * @param <T> The type.
	 * @return The value.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T resolveAttribute(List<Annotation> annotations, String name, Class<T> requiredType) {
		for (Annotation annotation : annotations) {
			if (annotation != null) {
				Object value = AnnotationUtils.getValue(annotation, name);
				if (value != null && value.getClass() == requiredType && hasValue(value)) {
					return (T) value;
				}
			}
		}
		return null;
	}

	public static boolean hasValue(Object value) {
		return value != null && (!(value instanceof String) || (StringUtils.hasText((String) value)))
				&& (!value.getClass().isArray() || ((Object[]) value).length > 0);
	}

	public static Method findAnnotatedMethod(Object target, final Class<? extends Annotation> annotationType) {
		final AtomicReference<Method> reference = new AtomicReference<Method>();

		ReflectionUtils.doWithMethods(getTargetClass(target), new ReflectionUtils.MethodCallback() {

			@Override
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				reference.compareAndSet(null, method);
			}
		}, new ReflectionUtils.MethodFilter() {

			@Override
			public boolean matches(Method method) {
				return ReflectionUtils.USER_DECLARED_METHODS.matches(method) &&
						AnnotatedElementUtils.isAnnotated(method, annotationType.getName());
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

	private MessagingAnnotationUtils() {}

}
