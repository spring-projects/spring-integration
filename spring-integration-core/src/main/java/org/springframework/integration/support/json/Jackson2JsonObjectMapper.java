/*
 * Copyright 2013-2018 the original author or authors.
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

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson 2 JSON-processor (@link https://github.com/FasterXML)
 * {@linkplain JsonObjectMapper} implementation.
 * Delegates <code>toJson</code> and <code>fromJson</code>
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

	private final ObjectMapper objectMapper;

	public Jackson2JsonObjectMapper() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
	public String toJson(Object value) throws Exception {
		return this.objectMapper.writeValueAsString(value);
	}

	@Override
	public void toJson(Object value, Writer writer) throws Exception {
		this.objectMapper.writeValue(writer, value);
	}

	@Override
	public JsonNode toJsonNode(Object json) throws Exception {
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
	protected <T> T fromJson(Object json, JavaType type) throws Exception {
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
			throw new IllegalArgumentException("'json' argument must be an instance of: " + supportedJsonTypes
					+ " , but gotten: " + json.getClass());
		}
	}

	@Override
	public <T> T fromJson(JsonParser parser, Type valueType) throws Exception {
		return this.objectMapper.readValue(parser, constructType(valueType));
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	protected JavaType extractJavaType(Map<String, Object> javaTypes) throws Exception {
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

		JavaType keyClassType = this.createJavaType(javaTypes, JsonHeaders.KEY_TYPE_ID);
		return this.objectMapper.getTypeFactory()
				.constructMapType((Class<? extends Map<?, ?>>) classType.getRawClass(), keyClassType, contentClassType);
	}

	@Override
	protected JavaType constructType(Type type) {
		return this.objectMapper.constructType(type);
	}

	@SuppressWarnings("unchecked")
	private void registerWellKnownModulesIfAvailable() {
		try {
			Class<? extends Module> jdk7Module = (Class<? extends Module>)
					ClassUtils.forName("com.fasterxml.jackson.datatype.jdk7.Jdk7Module", getClassLoader());
			this.objectMapper.registerModule(BeanUtils.instantiateClass(jdk7Module));
		}
		catch (ClassNotFoundException ex) {
			// jackson-datatype-jdk7 not available
		}

		try {
			Class<? extends Module> jdk8Module = (Class<? extends Module>)
					ClassUtils.forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module", getClassLoader());
			this.objectMapper.registerModule(BeanUtils.instantiateClass(jdk8Module));
		}
		catch (ClassNotFoundException ex) {
			// jackson-datatype-jdk8 not available
		}

		try {
			Class<? extends Module> javaTimeModule = (Class<? extends Module>)
					ClassUtils.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", getClassLoader());
			this.objectMapper.registerModule(BeanUtils.instantiateClass(javaTimeModule));
		}
		catch (ClassNotFoundException ex) {
			// jackson-datatype-jsr310 not available
		}

		// Joda-Time present?
		if (ClassUtils.isPresent("org.joda.time.LocalDate", getClassLoader())) {
			try {
				Class<? extends Module> jodaModule = (Class<? extends Module>)
						ClassUtils.forName("com.fasterxml.jackson.datatype.joda.JodaModule", getClassLoader());
				this.objectMapper.registerModule(BeanUtils.instantiateClass(jodaModule));
			}
			catch (ClassNotFoundException ex) {
				// jackson-datatype-joda not available
			}
		}

		// Kotlin present?
		if (ClassUtils.isPresent("kotlin.Unit", getClassLoader())) {
			try {
				Class<? extends Module> kotlinModule = (Class<? extends Module>)
						ClassUtils.forName("com.fasterxml.jackson.module.kotlin.KotlinModule", getClassLoader());
				this.objectMapper.registerModule(BeanUtils.instantiateClass(kotlinModule));
			}
			catch (ClassNotFoundException ex) {
				//jackson-module-kotlin not available
			}
		}
	}

}
