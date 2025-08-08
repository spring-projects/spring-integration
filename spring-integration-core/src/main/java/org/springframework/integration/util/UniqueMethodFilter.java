/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
 * @since 2.0
 */
public class UniqueMethodFilter implements MethodFilter {

	private final List<Method> uniqueMethods = new ArrayList<Method>();

	public UniqueMethodFilter(Class<?> targetClass) {
		Method[] allMethods = ReflectionUtils.getAllDeclaredMethods(targetClass);
		for (Method method : allMethods) {
			this.uniqueMethods.add(org.springframework.util.ClassUtils.getMostSpecificMethod(method, targetClass));
		}
	}

	public boolean matches(Method method) {
		return this.uniqueMethods.contains(method);
	}

}
