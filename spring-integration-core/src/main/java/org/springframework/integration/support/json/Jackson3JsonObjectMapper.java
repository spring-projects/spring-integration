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
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Jackson 3 JSON-processor (@link https://github.com/FasterXML)
 * {@linkplain JsonObjectMapper} implementation.
 * Delegates {@link #toJson} and {@link #fromJson}
 * to the {@linkplain ObjectMapper}
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
public class Jackson3JsonObjectMapper extends AbstractJacksonJsonObjectMapper<JsonNode, JsonParser, JavaType> {

	private static final boolean JODA_MODULE_PRESENT =
			ClassUtils.isPresent("tools.jackson.datatype.joda.JodaModule", null);

	private static final boolean KOTLIN_MODULE_PRESENT =
			ClassUtils.isPresent("kotlin.Unit", null) &&
					ClassUtils.isPresent("tools.jackson.module.kotlin.KotlinModule", null);

	private final ObjectMapper objectMapper;

	public Jackson3JsonObjectMapper() {
		List<JacksonModule> jacksonModules = collectWellKnownModulesIfAvailable();
		this.objectMapper = JsonMapper.builder()
				.addModules(jacksonModules)
				.build();
	}

	public Jackson3JsonObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "objectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	public ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	@Override
	public String toJson(Object value) throws JacksonException {
		return this.objectMapper.writeValueAsString(value);
	}

	@Override
	public void toJson(Object value, Writer writer) throws JacksonException {
		this.objectMapper.writeValue(writer, value);
	}

	@Override
	public JsonNode toJsonNode(Object json) throws JacksonException {
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
		catch (JacksonException e) {
			if (!(json instanceof String) && !(json instanceof byte[])) {
				throw e;
			}
			// Otherwise the input might not be valid JSON, fallback to TextNode with ObjectMapper.valueToTree()
		}

		return this.objectMapper.valueToTree(json);
	}

	@Override
	protected <T> T fromJson(Object json, JavaType type) throws RuntimeException {
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
	public <T> T fromJson(JsonParser parser, Type valueType) throws JacksonException {
		return this.objectMapper.readValue(parser, constructType(valueType));
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

	private List<JacksonModule> collectWellKnownModulesIfAvailable() {
		List<JacksonModule> modules = new ArrayList<>();
		if (JODA_MODULE_PRESENT) {
			modules.add(JodaModuleProvider.MODULE);
		}
		if (KOTLIN_MODULE_PRESENT) {
			modules.add(KotlinModuleProvider.MODULE);
		}
		return modules;
	}

	private static final class JodaModuleProvider {

		static final tools.jackson.databind.JacksonModule MODULE =
				new tools.jackson.datatype.joda.JodaModule();

	}

	private static final class KotlinModuleProvider {

		static final tools.jackson.databind.JacksonModule MODULE =
				new tools.jackson.module.kotlin.KotlinModule.Builder().build();

	}

}
