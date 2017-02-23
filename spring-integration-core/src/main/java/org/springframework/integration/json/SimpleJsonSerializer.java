/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.json;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ReflectionUtils;

/**
 * Extremely simple JSON serializer. Only handles top level
 * properties accessed by getters.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public final class SimpleJsonSerializer {

	private static final Log logger = LogFactory.getLog(SimpleJsonSerializer.class);

	private SimpleJsonSerializer() {
		super();
	}

	/**
	 * Convert the bean to JSON with the provided properties.
	 * @param bean the object to serialize.
	 * @param properties the property names (must have getters).
	 * @return the JSON.
	 */
	public static String toJson(Object bean, String... properties) {
		Map<String, String> propertyMap = new HashMap<>();
		List<String> list = Arrays.asList(properties);
		list.forEach(p -> {
			char[] chars = p.toCharArray();
			chars[0] = Character.toUpperCase(chars[0]);
			propertyMap.put(new String(chars), p);
		});
		final StringBuilder stringBuilder = new StringBuilder("{");
		final Object[] emptyArgs = new Object[0];
		ReflectionUtils.doWithMethods(bean.getClass(), method -> {
			int beginIndex = method.getName().startsWith("get") ? 3 : 2;
			stringBuilder.append(toElement(propertyMap.get(method.getName().substring(beginIndex)))).append(":");
			Object result;
			try {
				result = method.invoke(bean, emptyArgs);
			}
			catch (InvocationTargetException e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to serialize property " + method.getName(), e);
				}
				result = e.getMessage();
			}
			stringBuilder.append(toElement(result)).append(",");
		}, method -> {
			String name = method.getName();
			return (name.length() > 3 && name.startsWith("get") && propertyMap.keySet().contains(name.substring(3)))
				|| (name.length() > 2 && name.startsWith("is") && propertyMap.keySet().contains(name.substring(2)));
		});
		stringBuilder.setLength(stringBuilder.length() - 1);
		stringBuilder.append("}");
		return stringBuilder.toString();
	}

	private static String toElement(Object result) {
		if (result instanceof Number || result instanceof Boolean) {
			return result.toString();
		}
		else {
			return "\"" + result.toString() + "\"";
		}
	}

}
