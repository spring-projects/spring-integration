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

import org.springframework.util.ClassUtils;

/**
 * Simple factory to provide {@linkplain Jackson2JsonObjectMapper} or {@linkplain JacksonJsonObjectMapper}
 * instances dependently of jackson-databind or jackson-mapper-asl libs in the classpath.
 * If there are both libs in the classpath, it prefers Jackson 2 JSON-processor implementation.
 * If there is no any of them, {@linkplain IllegalStateException} will be thrown.
 *
 * @author Artem Bilan
 * @since 3.0
 *
 * @see Jackson2JsonObjectMapper
 * @see JacksonJsonObjectMapper
 * @see JsonToObjectTransformer
 * @see ObjectToJsonTransformer
 */
public final class JacksonJsonObjectMapperProvider {

	private static final ClassLoader classLoader = JacksonJsonObjectMapperProvider.class.getClassLoader();

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);

	private static final boolean jacksonPresent =
			ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", classLoader) &&
					ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", classLoader);

	private static final IllegalStateException NO_JACKSON_LIB_EXCEPTION =
			new IllegalStateException("Neither jackson-databind.jar, nor jackson-mapper-asl.jar aren't presented in the classpath.");

	public static JsonObjectMapper<?> newInstance() {
		if (jackson2Present) {
			return new Jackson2JsonObjectMapper();
		}
		if(jacksonPresent) {
			return new JacksonJsonObjectMapper();
		}
		throw NO_JACKSON_LIB_EXCEPTION;
	}

	public static JsonInboundMessageMapper.JsonMessageParser<?> newJsonMessageParser() {
		if (jackson2Present) {
			return new Jackson2JsonMessageParser();
		}
		if(jacksonPresent) {
			return new Jackson2JsonMessageParser();
		}
		throw NO_JACKSON_LIB_EXCEPTION;
	}

}
