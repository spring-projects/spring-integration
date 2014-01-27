/*
 * Copyright 2002-2014 the original author or authors.
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


/**
 * Simple factory to provide {@linkplain Jackson2JsonObjectMapper} or {@linkplain JacksonJsonObjectMapper}
 * instances dependently of jackson-databind or jackson-mapper-asl libs in the classpath.
 * If there are both libs in the classpath, it prefers Jackson 2 JSON-processor implementation.
 * If there is not any of them, {@linkplain IllegalStateException} will be thrown.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 *
 * @see Jackson2JsonObjectMapper
 */
public final class JacksonJsonObjectMapperProvider {

	@SuppressWarnings("deprecation")
	public static JsonObjectMapper<?, ?> newInstance() {
		if (JacksonJsonUtils.isJackson2Present()) {
			return new Jackson2JsonObjectMapper();
		}
		if(JacksonJsonUtils.isJacksonPresent()) {
			return new JacksonJsonObjectMapper();
		}
		throw JacksonJsonUtils.getNoJacksonLibException();
	}

}
