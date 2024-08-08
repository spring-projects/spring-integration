/*
 * Copyright 2014-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.support.management;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationFilter;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.integration.core.Pausable;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.ReflectionUtils;

/**
 * The {@link ReflectionUtils.MethodFilter} to restrict method invocations to:
 * <ul>
 *     <li> {@link Pausable} or {@link Lifecycle} components
 *     <li> {@code get}, {@code set} and {@code shutdown} methods of {@link CustomizableThreadCreator}
 *     <li> methods with {@link ManagedAttribute} or {@link ManagedOperation} annotations
 * </ul>
 *
 * @author Artem Bilan
 *
 * @since 6.4
 */
public class ControlBusMethodFilter implements ReflectionUtils.MethodFilter {

	@Override
	public boolean matches(Method method) {
		if (Modifier.isPublic(method.getModifiers())) {
			Class<?> declaringClass = method.getDeclaringClass();
			String methodName = method.getName();
			if ((Pausable.class.isAssignableFrom(declaringClass) || Lifecycle.class.isAssignableFrom(declaringClass))
					&& ReflectionUtils.findMethod(Pausable.class, methodName, method.getParameterTypes()) != null) {
				return true;
			}

			if (CustomizableThreadCreator.class.isAssignableFrom(declaringClass)
					&& (methodName.startsWith("get")
					|| methodName.startsWith("set")
					|| methodName.equals("shutdown"))) {
				return true;
			}

			MergedAnnotations mergedAnnotations =
					MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY,
							RepeatableContainers.none(), AnnotationFilter.PLAIN);

			return mergedAnnotations.get(ManagedAttribute.class).isPresent()
					|| mergedAnnotations.get(ManagedOperation.class).isPresent();
		}

		return false;
	}

}
