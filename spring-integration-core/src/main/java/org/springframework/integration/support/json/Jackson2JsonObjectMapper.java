/*
 * Copyright 2013-2021 the original author or authors.
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
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Jackson 2 JSON-processor (@link https://github.com/FasterXML)
 * {@linkplain JsonObjectMapper} implementation.
 * Delegates {@link #toJson} and {@link #fromJson}
 * to the {@linkplain com.fasterxml.jackson.databind.ObjectMapper}
 * <p>
 * It customizes Jackson's default properties with the following ones:
 * <ul>
 * <li>{@link MapperFeature#DEFAULT_VIEW_INCLUSION} is disabled</li>
 * <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} is disabled</li>
 * <li>The well-known modules are registered through the classpath scan</li>
 * </ul>
 *
 * See {@code org.springframework.http.converter.json.Jackson2ObjectMapperBuilder}
 * in the spring-web for more information.
 *
 * @author Artem Bilan
 * @author Vikas Prasad
 *
 * @since 3.0
 *
 */
public class Jackson2JsonObjectMapper extends AbstractJacksonJsonObjectMapper<JsonNode, JsonParser, JavaType> {

	private static final boolean JDK8_MODULE_PRESENT =
			ClassUtils.isPresent("com.fasterxml.jackson.datatype.jdk8.Jdk8Module", null);

	private static final boolean JAVA_TIME_MODULE_PRESENT =
			ClassUtils.isPresent("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", null);

	private static final boolean JODA_MODULE_PRESENT =
			ClassUtils.isPresent("com.fasterxml.jackson.datatype.joda.JodaModule", null);

	private static final boolean KOTLIN_MODULE_PRESENT =
			ClassUtils.isPresent("kotlin.Unit", null) &&
					ClassUtils.isPresent("com.fasterxml.jackson.module.kotlin.KotlinModule", null);

	private final ObjectMapper objectMapper;

	public Jackson2JsonObjectMapper() {
		this.objectMapper = JsonMapper.builder()
				.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.build();
		registerWellKnownModulesIfAvailable();
	}

	public Jackson2JsonObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "objectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	public ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	@Override
	public String toJson(Object value) throws JsonProcessingException {
		return this.objectMapper.writeValueAsString(value);
	}

	@Override
	public void toJson(Object value, Writer writer) throws IOException {
		this.objectMapper.writeValue(writer, value);
	}

	@Override
	public JsonNode toJsonNode(Object json) throws IOException {
		try {
			if (json instanceof String) {
				return this.objectMapper.readTree((String) json);
			}
			else if (json instanceof byte[]) {
				return this.objectMapper.readTree((byte[]) json);
			}
			else if (json instanceof File) {
				return this.objectMapper.readTree((File) json);
			}
			else if (json instanceof URL) {
				return this.objectMapper.readTree((URL) json);
			}
			else if (json instanceof InputStream) {
				return this.objectMapper.readTree((InputStream) json);
			}
			else if (json instanceof Reader) {
				return this.objectMapper.readTree((Reader) json);
			}
		}
		catch (JsonParseException e) {
			if (!(json instanceof String) && !(json instanceof byte[])) {
				throw e;
			}
			// Otherwise the input might not be valid JSON, fallback to TextNode with ObjectMapper.valueToTree()
		}

		return this.objectMapper.valueToTree(json);
	}

	@Override
	protected <T> T fromJson(Object json, JavaType type) throws IOException {
		if (json instanceof String) {
			return this.objectMapper.readValue((String) json, type);
		}
		else if (json instanceof byte[]) {
			return this.objectMapper.readValue((byte[]) json, type);
		}
		else if (json instanceof File) {
			return this.objectMapper.readValue((File) json, type);
		}
		else if (json instanceof URL) {
			return this.objectMapper.readValue((URL) json, type);
		}
		else if (json instanceof InputStream) {
			return this.objectMapper.readValue((InputStream) json, type);
		}
		else if (json instanceof Reader) {
			return this.objectMapper.readValue((Reader) json, type);
		}
		else {
			throw new IllegalArgumentException("'json' argument must be an instance of: " + SUPPORTED_JSON_TYPES
					+ " , but gotten: " + json.getClass());
		}
	}

	@Override
	public <T> T fromJson(JsonParser parser, Type valueType) throws IOException {
		return this.objectMapper.readValue(parser, constructType(valueType));
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	protected JavaType extractJavaType(Map<String, Object> javaTypes) {
		JavaType classType = this.createJavaType(javaTypes, JsonHeaders.TYPE_ID);
		if (!classType.isContainerType() || classType.isArrayType()) {
			return classType;
		}

		JavaType contentClassType = this.createJavaType(javaTypes, JsonHeaders.CONTENT_TYPE_ID);
		if (classType.getKeyType() == null) {
			return this.objectMapper.getTypeFactory()
					.constructCollectionType((Class<? extends Collection<?>>) classType.getRawClass(),
							contentClassType);
		}

		JavaType keyClassType = createJavaType(javaTypes, JsonHeaders.KEY_TYPE_ID);
		return this.objectMapper.getTypeFactory()
				.constructMapType((Class<? extends Map<?, ?>>) classType.getRawClass(), keyClassType, contentClassType);
	}

	@Override
	protected JavaType constructType(Type type) {
		return this.objectMapper.constructType(type);
	}

	private void registerWellKnownModulesIfAvailable() {
		if (JDK8_MODULE_PRESENT) {
			this.objectMapper.registerModule(Jdk8ModuleProvider.MODULE);
		}

		if (JAVA_TIME_MODULE_PRESENT) {
			this.objectMapper.registerModule(JavaTimeModuleProvider.MODULE);
		}

		if (JODA_MODULE_PRESENT) {
			this.objectMapper.registerModule(JodaModuleProvider.MODULE);
		}

		if (KOTLIN_MODULE_PRESENT) {
			this.objectMapper.registerModule(KotlinModuleProvider.MODULE);
		}
	}

	private static final class Jdk8ModuleProvider {

		static final com.fasterxml.jackson.databind.Module MODULE =
				new com.fasterxml.jackson.datatype.jdk8.Jdk8Module();

	}

	private static final class JavaTimeModuleProvider {

		static final com.fasterxml.jackson.databind.Module MODULE =
				new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule();

	}

	private static final class JodaModuleProvider {

		static final com.fasterxml.jackson.databind.Module MODULE =
				new com.fasterxml.jackson.datatype.joda.JodaModule();

	}

	private static final class KotlinModuleProvider {

		static final com.fasterxml.jackson.databind.Module MODULE =
				new com.fasterxml.jackson.module.kotlin.KotlinModule();

	}

}
