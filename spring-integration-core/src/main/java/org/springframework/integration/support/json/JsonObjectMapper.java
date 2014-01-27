/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.integration.support.json;

import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Strategy interface to convert an Object to/from the JSON representation.
 *
 * @param <N> - The expected type of JSON Node.
 * @param <P> - The expected type of JSON Parser.
 *
 * @author Artem Bilan
 * @since 3.0
 *
 */
public interface JsonObjectMapper<N, P> {

	String toJson(Object value) throws Exception;

	void toJson(Object value, Writer writer) throws Exception;

	N toJsonNode(Object value) throws Exception;

	<T> T fromJson(Object json, Class<T> valueType) throws Exception;

	<T> T fromJson(Object json, Map<String, Object> javaTypes) throws Exception;

	<T> T fromJson(P parser, Type valueType) throws Exception;

	void populateJavaTypes(Map<String, Object> map, Class<?> sourceClass);
}
