/*
 * Copyright 2002-2017 the original author or authors.
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


import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple factory to provide {@linkplain JsonObjectMapper}
 * instances dependently of jackson-databind or boon libs in the classpath.
 * If there are both libs in the classpath, it prefers Jackson 2 JSON-processor implementation.
 * If there is not any of them, {@linkplain IllegalStateException} will be thrown.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 *
 * @see Jackson2JsonObjectMapper
 * @see org.springframework.integration.support.json.BoonJsonObjectMapper
 */
public final class JsonObjectMapperProvider {

	private static final ClassLoader classLoader = JsonObjectMapperProvider.class.getClassLoader();

	private static final boolean boonPresent =
			ClassUtils.isPresent("org.boon.json.ObjectMapper", classLoader);

	private JsonObjectMapperProvider() {
		super();
	}

	/**
	 * Return an object mapper if available.
	 * @return the mapper.
	 * @throws IllegalStateException if an implementation is not available.
	 */
	public static JsonObjectMapper<?, ?> newInstance() {
		if (JacksonJsonUtils.isJackson2Present()) {
			return new Jackson2JsonObjectMapper();
		}
		else if (boonPresent) {
			return new BoonJsonObjectMapper();
		}
		else {
			throw new IllegalStateException("Neither jackson-databind.jar, nor boon.jar is present in the classpath.");
		}
	}

	/**
	 * Return an object mapper builder if available.
	 * @param preferBoon true to prefer boon if available.
	 * @return the mapper builder.
	 * @throws IllegalStateException if an implementation is not available.
	 * @since 5.0
	 */
	public static JsonObjectMapperBuilder<?> newInstanceBuilder(boolean preferBoon) {
		if (JacksonJsonUtils.isJackson2Present() && (!preferBoon || !boonPresent)) {
			return new JacksonJsonObjectMapperBuilder();
		}
		else if (boonPresent) {
			return new BoonJsonObjectMapperBuilder();
		}
		else {
			throw new IllegalStateException("Neither jackson-databind.jar, nor boon.jar is present in the classpath.");
		}
	}

	/**
	 * Returns true if a supported JSON implementation is on the class path.
	 * @return true if {@link #newInstance()} will return a mapper.
	 * @since 4.2.7
	 */
	public static boolean jsonAvailable() {
		return JacksonJsonUtils.isJackson2Present() || boonPresent;
	}

	public static abstract class JsonObjectMapperBuilder<B extends JsonObjectMapperBuilder<B>> {

		protected boolean usePropertyOnly = true; // NOSONAR

		protected boolean includeAllValues = true; // NOSONAR

		public B usePropertyOnly(boolean use) {
			this.usePropertyOnly = use;
			return _this();
		}

		public B includeAllValues(boolean include) {
			this.includeAllValues = include;
			return _this();
		}

		@SuppressWarnings("unchecked")
		protected final B _this() {
			return (B) this;
		}

		public abstract JsonObjectMapper<?, ?> build();

	}

	private static class JacksonJsonObjectMapperBuilder
			extends JsonObjectMapperBuilder<JacksonJsonObjectMapperBuilder> {

		JacksonJsonObjectMapperBuilder() {
			super();
		}

		@Override
		public JsonObjectMapper<?, ?> build() {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			if (this.usePropertyOnly) {
				objectMapper.setVisibility(PropertyAccessor.GETTER, Visibility.PUBLIC_ONLY);
				objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.NONE);
			}
			if (this.includeAllValues) {
				objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
			}
			return new Jackson2JsonObjectMapper(objectMapper);
		}

	}

	private static class BoonJsonObjectMapperBuilder extends JsonObjectMapperBuilder<BoonJsonObjectMapperBuilder> {

		BoonJsonObjectMapperBuilder() {
			super();
		}

		@Override
		public JsonObjectMapper<?, ?> build() {
			return new BoonJsonObjectMapper(null, f -> {
				f.useAnnotations();
				if (this.usePropertyOnly) {
					f.usePropertyOnly();
				}
				if (this.includeAllValues) {
					f.includeDefaultValues()
						.includeNulls()
						.includeEmpty();
				}
			});
		}

	}

}
