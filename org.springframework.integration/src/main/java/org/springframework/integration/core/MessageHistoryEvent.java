/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.core;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class MessageHistoryEvent {

	public static final String COMPONENT_NAME = "componentName";

	public static final String COMPONENT_TYPE = "componentType";

	public static final String TIMESTAMP = "timestamp";


	private final Map<String, Object> properties = new HashMap<String, Object>();


	public MessageHistoryEvent(String componentName) {
		this.setProperty(COMPONENT_NAME, componentName);
		this.setProperty(TIMESTAMP, System.currentTimeMillis());
	}


	public MessageHistoryEvent setComponentType(String componentType) {
		this.setProperty(COMPONENT_TYPE, componentType);
		return this;
	}

	public String getComponentType() {
		return this.getProperty(COMPONENT_TYPE, String.class);
	}

	public String getComponentName() {
		return this.getProperty(COMPONENT_NAME, String.class);
	}

	public long getTimestamp() {
		return this.getProperty(TIMESTAMP, long.class);
	}

	public MessageHistoryEvent setProperty(String key, String value) {
		this.properties.put(key, value);
		return this;
	}

	public MessageHistoryEvent setProperty(String key, Number value) {
		this.properties.put(key, value);
		return this;
	}

	public MessageHistoryEvent setProperty(String key, Boolean value) {
		this.properties.put(key, value);
		return this;
	}

	public Object getProperty(String key) {
		return this.properties.get(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T getProperty(String key, Class<T> type) {
		Object value = this.properties.get(key);
		if (value != null && type.isAssignableFrom(value.getClass())) {
			return (T) value;
		}
		return null;
	}

	public String toString() {
		return this.properties.toString();
	}

}
