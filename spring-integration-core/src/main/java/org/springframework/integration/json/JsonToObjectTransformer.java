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

package org.springframework.integration.json;

import org.springframework.integration.transformer.AbstractPayloadTransformer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Transformer implementation that converts a JSON string payload into an instance of the provided target Class.
 * By default this transformer uses {@linkplain JacksonJsonObjectMapperProvider} factory
 * to get an instance of Jackson 1 or Jackson 2 JSON-processor {@linkplain JsonObjectMapper} implementation
 * dependently of jackson-databind or jackson-mapper-asl libs in the classpath.
 * Any other {@linkplain JsonObjectMapper} implementation can be provided.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @see JsonObjectMapper
 * @see JacksonJsonObjectMapperProvider
 * @since 2.0
 */
public class JsonToObjectTransformer<T> extends AbstractPayloadTransformer<String, T> {

	private final Class<T> targetClass;

	private final JsonObjectMapper<?> jsonObjectMapper;

	public JsonToObjectTransformer(Class<T> targetClass) {
		this(targetClass, null);
	}

	/**
	 * @deprecated in favor of {@link #JsonToObjectTransformer(Class, JsonObjectMapper)}
	 */
	@Deprecated
	public JsonToObjectTransformer(Class<T> targetClass, Object objectMapper) throws ClassNotFoundException {
		Assert.notNull(targetClass, "targetClass must not be null");
		this.targetClass = targetClass;
		if (objectMapper != null) {
			try {
				Class<?> objectMapperClass = ClassUtils.forName("org.codehaus.jackson.map.ObjectMapper", ClassUtils.getDefaultClassLoader());
				Assert.isTrue(objectMapperClass.isAssignableFrom(objectMapper.getClass()));
				this.jsonObjectMapper = new JacksonJsonObjectMapper((org.codehaus.jackson.map.ObjectMapper) objectMapper);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(e);
			}
		}
		else {
			this.jsonObjectMapper = JacksonJsonObjectMapperProvider.newInstance();
		}
	}

	public JsonToObjectTransformer(Class<T> targetClass, JsonObjectMapper jsonObjectMapper) {
		Assert.notNull(targetClass, "targetClass must not be null");
		this.targetClass = targetClass;
		this.jsonObjectMapper = (jsonObjectMapper != null) ? jsonObjectMapper : JacksonJsonObjectMapperProvider.newInstance();
	}

	protected T transformPayload(String payload) throws Exception {
		return this.jsonObjectMapper.fromJson(payload, this.targetClass);
	}

}
