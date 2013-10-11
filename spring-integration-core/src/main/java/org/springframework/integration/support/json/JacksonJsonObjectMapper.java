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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Jackson JSON-processor (@link http://jackson.codehaus.org) {@linkplain JsonObjectMapper} implementation.
 * Delegates <code>toJson</code> and <code>fromJson</code>
 * to the {@linkplain org.codehaus.jackson.map.ObjectMapper}
 *
 * @author Artem Bilan
 * @since 3.0
 */
public class JacksonJsonObjectMapper implements JsonObjectMapper<JsonParser> {

	private final ObjectMapper objectMapper;

	public JacksonJsonObjectMapper() {
		this.objectMapper = new ObjectMapper();
	}

	public JacksonJsonObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "objectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	public String toJson(Object value) throws Exception {
		return this.objectMapper.writeValueAsString(value);
	}

	@Override
	public void toJson(Object value, Writer writer) throws Exception {
		this.objectMapper.writeValue(writer, value);
	}

	public <T> T fromJson(String json, Class<T> valueType) throws Exception {
		return this.objectMapper.readValue(json, valueType);
	}

	@Override
	public <T> T fromJson(String json, Map<String, Object> javaTypeProperties) throws Exception {
		JavaType javaType = this.extractJavaType(javaTypeProperties);
		return this.objectMapper.readValue(json, javaType);
	}

	@Override
	public <T> T fromJson(Reader json, Class<T> valueType) throws Exception {
		return this.objectMapper.readValue(json, valueType);
	}

	@Override
	public <T> T fromJson(JsonParser parser, Type valueType) throws Exception {
		return this.objectMapper.readValue(parser, this.objectMapper.constructType(valueType));
	}

	@Override
	public Map<String, String> getJavaTypeProperties(Class<?> valueClass) throws Exception {
		JavaType javaType = this.objectMapper.constructType(valueClass);
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(CLASSID, javaType.getRawClass().getName());

		if (javaType.isContainerType() && !javaType.isArrayType()) {
			properties.put(CONTENT_CLASSID, javaType.getContentType().getRawClass().getName());
		}

		if (javaType.getKeyType() != null) {
			properties.put(KEY_CLASSID, javaType.getKeyType().getRawClass().getName());
		}
		return properties;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JavaType extractJavaType(Map<String, Object> javaTypeProperties) throws Exception {
		JavaType classType = this.getClassIdType(javaTypeProperties, CLASSID);
		if (!classType.isContainerType() || classType.isArrayType()) {
			return classType;
		}

		JavaType contentClassType = this.getClassIdType(javaTypeProperties, CONTENT_CLASSID);
		if (classType.getKeyType() == null) {
			return this.objectMapper.getTypeFactory()
					.constructCollectionType((Class<? extends Collection>) classType.getRawClass(), contentClassType);
		}

		JavaType keyClassType = this.getClassIdType(javaTypeProperties, KEY_CLASSID);
		return this.objectMapper.getTypeFactory()
				.constructMapType((Class<? extends Map>) classType.getRawClass(), keyClassType, contentClassType);
	}

	private JavaType getClassIdType(Map<String, Object> javaTypeProperties, String javaTypeKey) throws Exception {
		Object classId = javaTypeProperties.get(javaTypeKey);
		if (classId == null) {
			throw new IllegalArgumentException("Could not resolve '" + javaTypeKey + "' in 'javaTypeProperties'.");
		} else {
			return this.objectMapper.constructType(ClassUtils.forName(classId.toString(), this.getClass().getClassLoader()));
		}
	}

}
