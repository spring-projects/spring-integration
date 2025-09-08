/*
 * Copyright 2025-present the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.util.Assert;

/**
 * Jackson 3 JSON-processor (@link https://github.com/FasterXML)
 * {@linkplain JsonObjectMapper} implementation.
 * Delegates {@link #toJson} and {@link #fromJson}
 * to the {@linkplain JsonMapper}
 * <p>
 * It customizes Jackson's default properties with the following ones:
 * <ul>
 * <li>The well-known modules are registered through the classpath scan</li>
 * </ul>
 *
 * See {@code tools.jackson.databind.json.JsonMapper.builder} for more information.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 *
 */
public class JacksonJsonObjectMapper extends AbstractJacksonJsonObjectMapper<JsonNode, JsonParser, JavaType> {

	private final JsonMapper jsonMapper;

	public JacksonJsonObjectMapper() {
		this.jsonMapper = JsonMapper.builder()
				.findAndAddModules(JacksonJsonObjectMapper.class.getClassLoader())
				.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
				.build();
	}

	public JacksonJsonObjectMapper(JsonMapper jsonMapper) {
		Assert.notNull(jsonMapper, "jsonMapper must not be null");
		this.jsonMapper = jsonMapper;
	}

	public JsonMapper getObjectMapper() {
		return this.jsonMapper;
	}

	@Override
	public String toJson(Object value) throws IOException {
		try {
			return this.jsonMapper.writeValueAsString(value);
		}
		catch (JacksonException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void toJson(Object value, Writer writer) throws IOException {
		try {
			this.jsonMapper.writeValue(writer, value);
		}
		catch (JacksonException e) {
			throw new IOException(e);
		}
	}

	@Override
	public JsonNode toJsonNode(Object json) throws IOException {
		try {
			if (json instanceof String stringValue) {
				return this.jsonMapper.readTree(stringValue);
			}
			else if (json instanceof byte[] bytesValue) {
				return this.jsonMapper.readTree(bytesValue);
			}
			else if (json instanceof File fileValue) {
				return this.jsonMapper.readTree(fileValue);
			}
			else if (json instanceof InputStream inputStreamValue) {
				return this.jsonMapper.readTree(inputStreamValue);
			}
			else if (json instanceof Reader readerValue) {
				return this.jsonMapper.readTree(readerValue);
			}
		}
		catch (JacksonException ex) {
			if (!(json instanceof String) && !(json instanceof byte[])) {
				throw new IOException(ex);
			}
			// Otherwise the input might not be valid JSON, fallback to TextNode with JsonMapper.valueToTree()
		}

		try {
			return this.jsonMapper.valueToTree(json);
		}
		catch (JacksonException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	protected <T> T fromJson(Object json, JavaType type) throws IOException {
		try {
			if (json instanceof String stringValue) {
				return this.jsonMapper.readValue(stringValue, type);
			}
			else if (json instanceof byte[] bytesValue) {
				return this.jsonMapper.readValue(bytesValue, type);
			}
			else if (json instanceof File fileValue) {
				return this.jsonMapper.readValue(fileValue, type);
			}
			else if (json instanceof InputStream inputStreamValue) {
				return this.jsonMapper.readValue(inputStreamValue, type);
			}
			else if (json instanceof Reader readerValue) {
				return this.jsonMapper.readValue(readerValue, type);
			}
			else {
				throw new IllegalArgumentException("'json' argument must be an instance of: " + SUPPORTED_JSON_TYPES
						+ " , but gotten: " + json.getClass());
			}
		}
		catch (JacksonException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public <T> T fromJson(JsonParser parser, Type valueType) throws IOException {
		try {
			return this.jsonMapper.readValue(parser, constructType(valueType));
		}
		catch (JacksonException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	protected JavaType extractJavaType(Map<String, Object> javaTypes) {
		JavaType classType = this.createJavaType(javaTypes, JsonHeaders.TYPE_ID);
		if (!classType.isContainerType() || classType.isArrayType()) {
			return classType;
		}

		JavaType contentClassType = this.createJavaType(javaTypes, JsonHeaders.CONTENT_TYPE_ID);
		if (classType.getKeyType() == null) {
			return this.jsonMapper.getTypeFactory()
					.constructCollectionType((Class<? extends Collection<?>>) classType.getRawClass(),
							contentClassType);
		}

		JavaType keyClassType = createJavaType(javaTypes, JsonHeaders.KEY_TYPE_ID);
		return this.jsonMapper.getTypeFactory()
				.constructMapType((Class<? extends Map<?, ?>>) classType.getRawClass(), keyClassType, contentClassType);
	}

	@Override
	protected JavaType constructType(Type type) {
		return this.jsonMapper.constructType(type);
	}

}
