/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Ngoc Nhan
 *
 * @since 2.0
 */
public class UniqueMethodFilter implements MethodFilter {

	private final List<Method> uniqueMethods;

	public UniqueMethodFilter(Class<?> targetClass) {
		Method[] allMethods = ReflectionUtils.getAllDeclaredMethods(targetClass);
		this.uniqueMethods = new ArrayList<>(allMethods.length);
		for (Method method : allMethods) {
			this.uniqueMethods.add(org.springframework.util.ClassUtils.getMostSpecificMethod(method, targetClass));
		}
	}

	public boolean matches(Method method) {
		return this.uniqueMethods.contains(method);
	}

}
