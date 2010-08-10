package org.springframework.integration.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class UniqueMethodFilter implements MethodFilter {

	private List<Method> uniqueMethods = new ArrayList<Method>();

	public UniqueMethodFilter(Class<?> targetClass) {
		ArrayList<Method> allMethods = new ArrayList<Method>(Arrays.asList(targetClass.getMethods()));
		for (Method method : allMethods) {
			uniqueMethods.add(org.springframework.util.ClassUtils.getMostSpecificMethod(method, targetClass));
		}
	}

	public boolean matches(Method method) {
		return uniqueMethods.contains(method);
	}
}