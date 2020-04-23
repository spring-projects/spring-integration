/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.json;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;

/**
 * Extremely simple JSON serializer. Only handles top level
 * properties accessed by getters.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public final class SimpleJsonSerializer {

	private static final Log logger = LogFactory.getLog(SimpleJsonSerializer.class);

	private SimpleJsonSerializer() {
	}

	/**
	 * Convert the bean to JSON with the provided properties.
	 * @param bean the object to serialize.
	 * @param propertiesToExclude the property names to ignore.
	 * @return the JSON.
	 */
	public static String toJson(Object bean, String... propertiesToExclude) {
		PropertyDescriptor[] propertyDescriptors = BeanUtils.getPropertyDescriptors(bean.getClass());
		Set<String> excluded = new HashSet<>(Arrays.asList(propertiesToExclude));
		excluded.add("class");
		final StringBuilder stringBuilder = new StringBuilder("{");
		final Object[] emptyArgs = new Object[0];
		for (PropertyDescriptor descriptor : propertyDescriptors) {
			String propertyName = descriptor.getName();
			Method readMethod = descriptor.getReadMethod();
			if (!excluded.contains(propertyName) && readMethod != null) {
				stringBuilder.append(toElement(propertyName)).append(":");
				Object result;
				try {
					result = readMethod.invoke(bean, emptyArgs);
				}
				catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
					Throwable exception = e;
					if (e instanceof InvocationTargetException) {
						exception = e.getCause();
					}

					if (logger.isDebugEnabled()) {
						logger.debug("Failed to serialize property " + propertyName, exception);
					}

					result =
							exception.getMessage() != null
									? exception.getMessage()
									: exception.toString();
				}
				stringBuilder.append(toElement(result)).append(",");
			}
		}
		stringBuilder.setLength(stringBuilder.length() - 1);
		stringBuilder.append("}");
		if (stringBuilder.length() == 1) {
			return null;
		}
		else {
			return stringBuilder.toString();
		}
	}

	private static String toElement(Object result) {
		if (result instanceof Number || result instanceof Boolean) {
			return result.toString();
		}
		else {
			return "\"" + (result == null ? "null" : Matcher.quoteReplacement(result.toString())) + "\"";
		}
	}

}
