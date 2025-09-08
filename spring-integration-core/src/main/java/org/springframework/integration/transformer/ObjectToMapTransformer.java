/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.transformer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapperProvider;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Transforms an object graph into a Map. It supports a conventional Map (map of maps)
 * where complex attributes are represented as Map values as well as a flat Map
 * where keys document the path to the value. By default, it will transform to a flat Map.
 * If you need to transform to a Map of Maps set the 'shouldFlattenKeys' property to 'false'
 * via the {@link ObjectToMapTransformer#setShouldFlattenKeys(boolean)} method.
 * It supports Collections, Maps and Arrays, which means that for flat maps it will flatten
 * an Object's properties. Below is an example showing how a flattened
 * Object hierarchy is represented when 'shouldFlattenKeys' is TRUE.
 *<p>
 * The transformation is based on to and then from JSON conversion.
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
 * @author Gary Russell
 * @author Vikas Prasad
 *
 * @since 2.0
 *
 * @see JsonObjectMapperProvider
 */
public class ObjectToMapTransformer extends AbstractPayloadTransformer<Object, Map<?, ?>> {

	private final JsonObjectMapper<?, ?> jsonObjectMapper;

	private volatile boolean shouldFlattenKeys = true;

	/**
	 * Construct with the default {@link JsonObjectMapper} instance available via
	 * {@link JsonObjectMapperProvider#newInstance() factory}.
	 */
	public ObjectToMapTransformer() {
		this(JsonObjectMapperProvider.newInstance());
	}

	/**
	 * Construct with the provided {@link JsonObjectMapper} instance.
	 * @param jsonObjectMapper the {@link JsonObjectMapper} to use.
	 * @since 5.0
	 */
	public ObjectToMapTransformer(JsonObjectMapper<?, ?> jsonObjectMapper) {
		Assert.notNull(jsonObjectMapper, "'jsonObjectMapper' must not be null");
		this.jsonObjectMapper = jsonObjectMapper;
	}

	public void setShouldFlattenKeys(boolean shouldFlattenKeys) {
		this.shouldFlattenKeys = shouldFlattenKeys;
	}

	@Override
	public String getComponentType() {
		return "object-to-map-transformer";
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Map<String, @Nullable Object> transformPayload(Object payload) {
		Map<String, @Nullable Object> result;
		try {
			var payloadValue = this.jsonObjectMapper.toJson(payload);
			Assert.state(payloadValue != null, "'payload' must not be null");
			result = this.jsonObjectMapper.fromJson(payloadValue, Map.class);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		Assert.state(result != null, "The payload must not be null");
		if (this.shouldFlattenKeys) {
			result = flattenMap(result);
		}
		return result;
	}

	private Map<String, @Nullable Object> flattenMap(Map<String, @Nullable Object> result) {
		Map<String, @Nullable Object> resultMap = new HashMap<>();
		doFlatten("", result, resultMap);
		return resultMap;
	}

	private void doFlatten(String propertyPrefixArg, Map<String, @Nullable Object> inputMap,
			Map<String, @Nullable Object> resultMap) {

		String propertyPrefix = propertyPrefixArg;
		if (StringUtils.hasText(propertyPrefix)) {
			propertyPrefix = propertyPrefix + ".";
		}
		for (Entry<String, Object> entry : inputMap.entrySet()) {
			doProcessElement(propertyPrefix + entry.getKey(), entry.getValue(), resultMap);
		}
	}

	@SuppressWarnings("unchecked")
	private void doProcessElement(String propertyPrefix, @Nullable Object element,
			Map<String, @Nullable Object> resultMap) {

		if (element instanceof Map) {
			doFlatten(propertyPrefix, (Map<String, @Nullable Object>) element, resultMap);
		}
		else if (element instanceof Collection<?> collection) {
			doProcessCollection(propertyPrefix, collection, resultMap);
		}
		else if (element != null && element.getClass().isArray()) {
			Collection<?> collection = CollectionUtils.arrayToList(element);
			doProcessCollection(propertyPrefix, collection, resultMap);
		}
		else {
			resultMap.put(propertyPrefix, element);
		}
	}

	private void doProcessCollection(String propertyPrefix, Collection<?> list,
			Map<String, @Nullable Object> resultMap) {

		int counter = 0;
		for (Object element : list) {
			doProcessElement(propertyPrefix + "[" + (counter++) + "]", element, resultMap);
		}
	}

}
