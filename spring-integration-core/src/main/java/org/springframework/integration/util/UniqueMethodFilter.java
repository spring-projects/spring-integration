/*
 * Copyright 2002-2024 the original author or authors.
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
 * @since 2.0
 */
public class UniqueMethodFilter implements MethodFilter {

	private final List<Method> uniqueMethods = new ArrayList<>();

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
