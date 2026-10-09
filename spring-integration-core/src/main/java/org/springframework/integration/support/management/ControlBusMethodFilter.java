/*
 * Copyright 2014-present the original author or authors.
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
