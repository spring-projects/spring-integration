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

import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;

/**
 * Default MethodResolver implementation. It first checks for a single Method
 * with the specified annotation (if not null), and then falls back to a single
 * public Method if available.
 * 
 * @author Mark Fisher
 */
public class DefaultMethodResolver implements MethodResolver {

	private final AnnotationMethodResolver annotationMethodResolver;


	public DefaultMethodResolver() {
		this(null);
	}

	public DefaultMethodResolver(Class<? extends Annotation> annotationType) {
		this.annotationMethodResolver = (annotationType != null) ?
				new AnnotationMethodResolver(annotationType) : null;
	}


	public Method findMethod(Object candidate) {
		Assert.notNull(candidate, "candidate object must not be null");
		Class<?> targetClass = AopUtils.getTargetClass(candidate);
		if (targetClass == null) {
			targetClass = candidate.getClass();
		}
		return this.findMethod(targetClass);
	}

	public Method findMethod(Class<?> clazz) {
		if (this.annotationMethodResolver != null) {
			Method method = this.annotationMethodResolver.findMethod(clazz);
			if (method != null) {
				return method;
			}
		}
		return this.findSinglePublicMethod(clazz);
	}

	private Method findSinglePublicMethod(Class<?> clazz) {
		Method result = null;
		for (Method method : clazz.getMethods()) {
			if (!method.getDeclaringClass().equals(Object.class)) {
				if (result != null) {
					throw new IllegalArgumentException(
							"Class [" + clazz + "] contains more than one public Method.");
				}
				result = method;
			}
		}
		return result;
	}

}
