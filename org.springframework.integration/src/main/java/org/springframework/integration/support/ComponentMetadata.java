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

package org.springframework.integration.support;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata describing a messaging component including the
 * component's name and type as well as any other attributes.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class ComponentMetadata implements Serializable {

	public static final String COMPONENT_NAME = "componentName";

	public static final String COMPONENT_TYPE = "componentType";


	private final Map<String, Object> attributes = new HashMap<String, Object>();


	/**
	 * Create a new ComponentMetadata instance with no attributes.
	 */
	public ComponentMetadata() {
	}

	/**
	 * Create a new ComponentMetadata instance that copies all attributes
	 * from the provided ComponentMetadata.
	 */
	public ComponentMetadata(ComponentMetadata metadata) {
		this.attributes.putAll(metadata.getAttributes());
	}


	public ComponentMetadata setComponentName(String componentName) {
		this.setAttribute(COMPONENT_NAME, componentName);
		return this;
	}

	public ComponentMetadata setComponentType(String componentType) {
		this.setAttribute(COMPONENT_TYPE, componentType);
		return this;
	}

	public String getComponentName() {
		return this.getAttribute(COMPONENT_NAME, String.class);
	}

	public String getComponentType() {
		return this.getAttribute(COMPONENT_TYPE, String.class);
	}

	public ComponentMetadata setAttribute(String key, String value) {
		this.attributes.put(key, value);
		return this;
	}

	public ComponentMetadata setAttribute(String key, Number value) {
		this.attributes.put(key, value);
		return this;
	}

	public ComponentMetadata setAttribute(String key, Boolean value) {
		this.attributes.put(key, value);
		return this;
	}

	public Object getAttribute(String key) {
		return this.attributes.get(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String key, Class<T> type) {
		Object value = this.attributes.get(key);
		if (value != null && type.isAssignableFrom(value.getClass())) {
			return (T) value;
		}
		return null;
	}

	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(this.attributes);
	}

	public String toString() {
		return this.attributes.toString();
	}

}
