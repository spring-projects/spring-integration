/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.integration.jmx;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * @author Stuart Williams
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class DefaultMBeanObjectConverter implements MBeanObjectConverter {

	private static final Log LOGGER = LogFactory.getLog(DefaultMBeanObjectConverter.class);

	private final MBeanAttributeFilter filter;

	public DefaultMBeanObjectConverter() {
		this(new DefaultMBeanAttributeFilter());
	}

	public DefaultMBeanObjectConverter(MBeanAttributeFilter filter) {
		Assert.notNull(filter, "'filter' must not be null.");
		this.filter = filter;
	}

	@Override
	public Object convert(MBeanServerConnection connection, ObjectInstance instance) {
		Map<String, Object> attributeMap = new HashMap<>();

		try {
			ObjectName objName = instance.getObjectName();
			if (!connection.isRegistered(objName)) {
				return attributeMap;
			}

			MBeanInfo info = connection.getMBeanInfo(objName);
			MBeanAttributeInfo[] attributeInfos = info.getAttributes();

			for (MBeanAttributeInfo attrInfo : attributeInfos) {
				// we don't need to repeat name of this as an attribute
				if ("ObjectName".equals(attrInfo.getName()) || !this.filter.accept(objName, attrInfo.getName())) {
					continue;
				}

				Object value;
				try {
					value = connection.getAttribute(objName, attrInfo.getName());
				}
				catch (RuntimeMBeanException e) {
					// N.B. standard MemoryUsage MBeans will throw an exception when some
					// measurement is unsupported. Logging at trace rather than debug to
					// avoid confusion.
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Error getting attribute '" + attrInfo.getName() + "' on '" + objName + "'", e);
					}

					// try to unwrap the exception somewhat; not sure this is ideal
					Throwable t = e;
					while (t.getCause() != null) {
						t = t.getCause();
					}
					value = String.format("%s[%s]", t.getClass().getName(), t.getMessage());
				}

				attributeMap.put(attrInfo.getName(), checkAndConvert(value));
			}

		}
		catch (Exception e) {
			throw new IllegalArgumentException(e);
		}

		return attributeMap;
	}

	private Object checkAndConvert(Object input) {
		Object converted = null;
		if (input instanceof CompositeData) {
			converted = convertFromCompositeData((CompositeData) input);
		}
		else if (input instanceof TabularData) {
			converted = convertFromTabularData((TabularData) input);
		}
		else if (input != null && input.getClass().isArray()) {
			converted = convertFromArray(input);
		}

		if (converted != null) {
			return converted;
		}
		else {
			return input;
		}
	}

	private Object convertFromArray(Object input) {
		if (CompositeData.class.isAssignableFrom(input.getClass().getComponentType())) {
			List<Object> converted = new ArrayList<>();
			int length = Array.getLength(input);
			for (int i = 0; i < length; i++) {
				Object value = checkAndConvert(Array.get(input, i));
				converted.add(value);
			}
			return converted;
		}
		if (TabularData.class.isAssignableFrom(input.getClass().getComponentType())) {
			// TODO haven't hit this yet, but expect to
			LOGGER.warn("TabularData.isAssignableFrom(getComponentType) for " + input.toString());
		}
		return null;
	}

	private Object convertFromCompositeData(CompositeData data) {
		if (data.getCompositeType().isArray()) {
			// TODO? I haven't found an example where this gets thrown - but need to test it on Tomcat/Jetty or
			// something
			LOGGER.warn("(data.getCompositeType().isArray for " + data.toString());
			return null;
		}
		else {
			Map<String, Object> returnable = new HashMap<>();
			Set<String> keys = data.getCompositeType().keySet();
			for (String key : keys) {
				// we don't need to repeat name of this as an attribute
				if ("ObjectName".equals(key)) {
					continue;
				}
				Object value = checkAndConvert(data.get(key));
				returnable.put(key, value);
			}
			return returnable;
		}
	}

	private Object convertFromTabularData(TabularData data) {
		if (data.getTabularType().isArray()) {
			// TODO? I haven't found an example where this gets thrown, so might not be required
			LOGGER.warn("TabularData.isArray for " + data.toString());
			return null;
		}
		else {
			Map<Object, Object> returnable = new HashMap<>();
			@SuppressWarnings("unchecked")
			Set<List<?>> keySet = (Set<List<?>>) data.keySet();
			for (List<?> keys : keySet) {
				CompositeData cd = data.get(keys.toArray());
				Object value = checkAndConvert(cd);
				if (keys.size() == 1 && (value instanceof Map) && ((Map<?, ?>) value).size() == 2) {
					Object actualKey = keys.get(0);
					Map<?, ?> valueMap = (Map<?, ?>) value;
					if (valueMap.containsKey("key") && valueMap.containsKey("value")
							&& actualKey.equals(valueMap.get("key"))) {
						returnable.put(valueMap.get("key"), valueMap.get("value"));
					}
					else {
						returnable.put(actualKey, value);
					}
				}
				else {
					returnable.put(keys, value);
				}
			}
			return returnable;
		}
	}

}
