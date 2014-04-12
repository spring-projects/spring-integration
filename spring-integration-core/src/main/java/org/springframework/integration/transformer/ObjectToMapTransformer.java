/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.transformer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.support.json.JacksonJsonObjectMapperProvider;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Will transform an object graph into a Map. It supports a conventional Map (map of maps) where complex attributes are
 * represented as Map values as well as a flat Map where keys document the path to the value. By default it will
 * transform to a flat Map. If you need to transform to a Map of Maps set the 'shouldFlattenKeys' property to 'false'
 * via the {@link ObjectToMapTransformer#setShouldFlattenKeys(boolean)} method. It supports Collections, Maps and Arrays
 * which means that for flat maps it will flatten an Object's properties. Below is an example showing how a flattened
 * Object hierarchy is represented when 'shouldFlattenKeys' is TRUE.<br>
 *
 * <code>
 * public class Person {
 *     public String name = "John";
 *     public Address address = new Address();
 * }
 * public class Address {
 *     private String street = "123 Main Street";
 * }
 * </code>
 *
 * The resulting Map would look like this:
 * <code>
 * {name=John, address.street=123 Main Street}
 * </code>
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class ObjectToMapTransformer extends AbstractPayloadTransformer<Object, Map<?,?>> {

	private final JsonObjectMapper<?, ?> jsonObjectMapper = JacksonJsonObjectMapperProvider.newInstance();

	private volatile boolean shouldFlattenKeys = true;

	public void setShouldFlattenKeys(boolean shouldFlattenKeys) {
		this.shouldFlattenKeys = shouldFlattenKeys;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Map<String, Object> transformPayload(Object payload) throws Exception {
		Map<String,Object> result = this.jsonObjectMapper.fromJson(this.jsonObjectMapper.toJson(payload), Map.class);
		if (this.shouldFlattenKeys) {
			result = this.flattenMap(result);
		}
		return result;
	}

	@Override
	public String getComponentType() {
		return "object-to-map-transformer";
	}

	@SuppressWarnings("unchecked")
	private void doProcessElement(String propertyPrefix, Object element, Map<String, Object> resultMap) {
		if (element instanceof Map) {
			this.doFlatten(propertyPrefix, (Map<String, Object>) element, resultMap);
		}
		else if (element instanceof Collection) {
			this.doProcessCollection(propertyPrefix, (Collection<?>) element, resultMap);
		}
		else if (element != null && element.getClass().isArray()) {
			Collection<?> collection =  CollectionUtils.arrayToList(element);
			this.doProcessCollection(propertyPrefix, collection, resultMap);
		}
		else {
			resultMap.put(propertyPrefix, element);
		}
	}

	private Map<String, Object> flattenMap(Map<String,Object> result){
		Map<String,Object> resultMap = new HashMap<String, Object>();
		this.doFlatten("", result, resultMap);
		return resultMap;
	}

	private void doFlatten(String propertyPrefix, Map<String,Object> inputMap, Map<String,Object> resultMap){
		if (StringUtils.hasText(propertyPrefix)) {
			propertyPrefix = propertyPrefix + ".";
		}
		for (String key : inputMap.keySet()) {
			Object value = inputMap.get(key);
			this.doProcessElement(propertyPrefix + key, value, resultMap);
		}
	}

	private void doProcessCollection(String propertyPrefix,  Collection<?> list, Map<String, Object> resultMap) {
		int counter = 0;
		for (Object element : list) {
			this.doProcessElement(propertyPrefix + "[" + counter + "]", element, resultMap);
			counter ++;
		}
	}

}
