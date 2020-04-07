/*
 * Copyright 2017-2020 the original author or authors.
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

import org.springframework.util.ClassUtils;

/**
 * The utility to check if Jackson JSON processor is present in the classpath.
 *
 * @author Artem Bilan
 *
 * @since 4.3.10
 */
public final class JacksonPresent {

	private static final ClassLoader CLASS_LOADER = ClassUtils.getDefaultClassLoader();

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", CLASS_LOADER) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", CLASS_LOADER);

	private static final boolean jacksonPresent =
			ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", CLASS_LOADER) &&
					ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", CLASS_LOADER);

	public static boolean isJackson2Present() {
		return jackson2Present;
	}

	/**
	 * @return true if Jackson 1.x is present on classpath
	 * @deprecated Jackson 1.x is not supported any more. Use Jackson 2.x.
	 */
	@Deprecated
	public static boolean isJacksonPresent() {
		return jacksonPresent;
	}


	private JacksonPresent() {
	}

}
