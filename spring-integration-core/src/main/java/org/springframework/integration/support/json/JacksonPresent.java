/*
 * Copyright 2017 the original author or authors.
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

/**
 * The utility to check if Jackson JSON processor is present in the classpath.
 *
 * @author Artem Bilan
 *
 * @since 4.3.10
 */
public final class JacksonPresent {

	private static final ClassLoader classLoader = JacksonPresent.class.getClassLoader();

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);

	private static final boolean jacksonPresent =
			ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", classLoader) &&
					ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", classLoader);

	public static boolean isJackson2Present() {
		return jackson2Present;
	}

	public static boolean isJacksonPresent() {
		return jacksonPresent;
	}


	private JacksonPresent() {
		super();
	}

}
