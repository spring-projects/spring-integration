/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.integration.support.json;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.integration.mapping.support.JsonHeaders;

/**
 * Strategy interface to convert an Object to/from the JSON representation.
 *
 * @param <N> - The expected type of JSON Node.
 * @param <P> - The expected type of JSON Parser.
 *
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public interface JsonObjectMapper<N, P> {

	default String toJson(Object value) throws IOException {
		return null;
	}

	default void toJson(Object value, Writer writer) throws IOException {

	}

	default N toJsonNode(Object value) throws IOException {
		return null;
	}

	default <T> T fromJson(Object json, Class<T> valueType) throws IOException {
		return null;
	}

	/**
	 * Deserialize a JSON to an expected {@link ResolvableType}.
	 * @param json the JSON to deserialize
	 * @param valueType the {@link ResolvableType} for the target object.
	 * @param <T> the expected object type
	 * @return deserialization result object
	 * @throws IOException a JSON parsing exception
	 * @since 5.2
	 */
	default <T> T fromJson(Object json, ResolvableType valueType) throws IOException {
		return null;
	}

	default <T> T fromJson(Object json, Map<String, Object> javaTypes) throws IOException {
		return null;
	}

	default <T> T fromJson(P parser, Type valueType) throws IOException {
		return null;
	}

	default void populateJavaTypes(Map<String, Object> map, Object object) {
		Class<?> targetClass = object.getClass();
		Class<?> contentClass = null;
		Class<?> keyClass = null;
		map.put(JsonHeaders.TYPE_ID, targetClass);
		if (object instanceof Collection && !((Collection<?>) object).isEmpty()) {
			Object firstElement = ((Collection<?>) object).iterator().next();
			if (firstElement != null) {
				contentClass = firstElement.getClass();
				map.put(JsonHeaders.CONTENT_TYPE_ID, contentClass);
			}
		}
		if (object instanceof Map && !((Map<?, ?>) object).isEmpty()) {
			Object firstValue = ((Map<?, ?>) object).values().iterator().next();
			if (firstValue != null) {
				contentClass = firstValue.getClass();
				map.put(JsonHeaders.CONTENT_TYPE_ID, contentClass);
			}
			Object firstKey = ((Map<?, ?>) object).keySet().iterator().next();
			if (firstKey != null) {
				keyClass = firstKey.getClass();
				map.put(JsonHeaders.KEY_TYPE_ID, keyClass);
			}
		}

		map.put(JsonHeaders.RESOLVABLE_TYPE, JsonHeaders.buildResolvableType(targetClass, contentClass, keyClass));
	}

}
