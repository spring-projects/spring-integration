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

package org.springframework.integration.expression;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.Lifecycle;
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
 *
 * @deprecated in favor of {@link org.springframework.integration.support.management.ControlBusMethodFilter}
 */
@Deprecated(since = "6.4", forRemoval = true)
public class ControlBusMethodFilter implements MethodFilter {

	private static final ReflectionUtils.MethodFilter CONTROL_BUS_METHOD_FILTER =
			new org.springframework.integration.support.management.ControlBusMethodFilter();

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
		return CONTROL_BUS_METHOD_FILTER.matches(method);
	}

}
