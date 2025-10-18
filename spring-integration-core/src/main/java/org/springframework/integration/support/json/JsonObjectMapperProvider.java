/*
 * Copyright 2002-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

/**
 * Simple factory to provide {@linkplain JsonObjectMapper}
 * instances based on jackson-databind lib in the classpath.
 * If there is no JSON processor in classpath, {@linkplain IllegalStateException} will be thrown.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Vikas Prasad
 * @author Jooyoung Pyoung
 *
 * @since 3.0
 *
 * @see JacksonJsonObjectMapper
 */
public final class JsonObjectMapperProvider {

	private JsonObjectMapperProvider() {
	}

	/**
	 * Return an object mapper if available.
	 * @return the mapper.
	 * @throws IllegalStateException if an implementation is not available.
	 */
	@SuppressWarnings("removal")
	public static JsonObjectMapper<?, ?> newInstance() {
		if (JacksonPresent.isJackson3Present()) {
			return new JacksonJsonObjectMapper();
		}
		else if (JacksonPresent.isJackson2Present()) {
			return new Jackson2JsonObjectMapper();
		}
		else {
			throw new IllegalStateException("No jackson-databind.jar is present in the classpath.");
		}
	}

	/**
	 * Return an object mapper (if available) aware of messaging classes to (de)serialize.
	 * @return the mapper.
	 * @throws IllegalStateException if an implementation is not available.
	 * @since 7.0
	 */
	@SuppressWarnings("removal")
	public static JsonObjectMapper<?, ?> newMessagingAwareInstance(String @Nullable ... trustedPackages) {
		if (JacksonPresent.isJackson3Present()) {
			return new JacksonJsonObjectMapper(JacksonMessagingUtils.messagingAwareMapper(trustedPackages));
		}
		else if (JacksonPresent.isJackson2Present()) {
			return new Jackson2JsonObjectMapper(JacksonJsonUtils.messagingAwareMapper(trustedPackages));
		}
		else {
			throw new IllegalStateException("No jackson-databind.jar is present in the classpath.");
		}
	}

	/**
	 * Returns true if a supported JSON implementation is on the class path.
	 * @return true if {@link #newInstance()} will return a mapper.
	 * @since 4.2.7
	 */
	public static boolean jsonAvailable() {
		return JacksonPresent.isJackson3Present() || JacksonPresent.isJackson2Present();
	}

}
