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

package org.springframework.integration.expression;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.MethodFilter;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.ReflectionUtils;

/**
 * SpEL {@link MethodFilter} to restrict method invocations to:
 * <ul>
 *     <li> {@link Lifecycle} components
 *     <li> {@code get}, {@code set} and {@code shutdown} methods of {@link CustomizableThreadCreator}
 *     <li> methods with {@link ManagedAttribute} and {@link ManagedOperation} annotations
 * </ul>
 * This class isn't designed for target applications and typically is used from {@code ExpressionControlBusFactoryBean}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
* @since 4.0
*/
public class ControlBusMethodFilter implements MethodFilter {

	public List<Method> filter(List<Method> methods) {
		List<Method> supportedMethods = new ArrayList<Method>();
		for (Method method : methods) {
			if (this.accept(method)) {
				supportedMethods.add(method);
			}
		}
		return supportedMethods;
	}

	private boolean accept(Method method) {
		Class<?> declaringClass = method.getDeclaringClass();
		if (Lifecycle.class.isAssignableFrom(declaringClass)
				&& ReflectionUtils.findMethod(Lifecycle.class, method.getName(), method.getParameterTypes()) != null) {
			return true;
		}
		if (CustomizableThreadCreator.class.isAssignableFrom(declaringClass)
				&& (method.getName().startsWith("get")
						|| method.getName().startsWith("set")
						|| method.getName().startsWith("shutdown"))) {
			return true;
		}
		if (this.hasAnnotation(method, ManagedAttribute.class) || this.hasAnnotation(method, ManagedOperation.class)) {
			return true;
		}
		return false;
	}

	private boolean hasAnnotation(Method method, Class<? extends Annotation> annotationType) {
		return AnnotationUtils.findAnnotation(method, annotationType) != null;
	}
}
