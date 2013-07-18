/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

/**
 * Strategy interface to convert an Object to/from the JSON representation.
 *
 * @author Artem Bilan
 * @since 3.0
 *
 */
public interface JsonObjectMapper<P> {

	String toJson(Object value) throws Exception;

	void toJson(Object value, Writer writer) throws Exception;

	<T> T fromJson(String json, Class<T> valueType) throws Exception;

	<T> T fromJson(Reader json, Class<T> valueType) throws Exception;

	<T> T fromJson(P parser, Type valueType) throws Exception;

}
