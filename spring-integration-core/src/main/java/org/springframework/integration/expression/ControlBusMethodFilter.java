/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.expression;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationFilter;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.expression.MethodFilter;
import org.springframework.integration.core.Pausable;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.ReflectionUtils;

/**
 * SpEL {@link MethodFilter} to restrict method invocations to:
 * <ul>
 *     <li> {@link Pausable} or {@link Lifecycle} components
 *     <li> {@code get}, {@code set} and {@code shutdown} methods of {@link CustomizableThreadCreator}
 *     <li> methods with {@link ManagedAttribute} and {@link ManagedOperation} annotations
 * </ul>
 * This class isn't designed for target applications and typically is used from {@code ExpressionControlBusFactoryBean}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
public class ControlBusMethodFilter implements MethodFilter {

	@Override
	public List<Method> filter(List<Method> methods) {
		List<Method> supportedMethods = new ArrayList<>();
		for (Method method : methods) {
			if (accept(method)) {
				supportedMethods.add(method);
			}
		}
		return supportedMethods;
	}

	private boolean accept(Method method) {
		Class<?> declaringClass = method.getDeclaringClass();
		String methodName = method.getName();
		if ((Pausable.class.isAssignableFrom(declaringClass) || Lifecycle.class.isAssignableFrom(declaringClass))
				&& ReflectionUtils.findMethod(Pausable.class, methodName, method.getParameterTypes()) != null) {
			return true;
		}

		if (CustomizableThreadCreator.class.isAssignableFrom(declaringClass)
				&& (methodName.startsWith("get")
				|| methodName.startsWith("set")
				|| methodName.startsWith("shutdown"))) {
			return true;
		}

		MergedAnnotations mergedAnnotations =
				MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY,
						RepeatableContainers.none(), AnnotationFilter.PLAIN);

		return mergedAnnotations.get(ManagedAttribute.class).isPresent()
				|| mergedAnnotations.get(ManagedOperation.class).isPresent();
	}

}
