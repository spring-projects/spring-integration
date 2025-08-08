/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.support.json;

/**
 * Simple factory to provide {@linkplain JsonObjectMapper}
 * instances based on jackson-databind lib in the classpath.
 * If there is no JSON processor in classpath, {@linkplain IllegalStateException} will be thrown.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Vikas Prasad
 *
 * @since 3.0
 *
 * @see Jackson2JsonObjectMapper
 */
public final class JsonObjectMapperProvider {

	private JsonObjectMapperProvider() {
	}

	/**
	 * Return an object mapper if available.
	 * @return the mapper.
	 * @throws IllegalStateException if an implementation is not available.
	 */
	public static JsonObjectMapper<?, ?> newInstance() {
		if (JacksonPresent.isJackson2Present()) {
			return new Jackson2JsonObjectMapper();
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
		return JacksonPresent.isJackson2Present();
	}

}
