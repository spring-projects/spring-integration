/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.http.aot;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.server.WebHandler;

/**
 * {@link RuntimeHintsRegistrar} for Spring Integration HTTP module.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
class HttpRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		ReflectionHints reflectionHints = hints.reflection();
		reflectionHints.registerType(WebHandler.class, MemberCategory.INVOKE_PUBLIC_METHODS);

		reflectionHints.registerType(HttpRequestHandler.class, builder ->
				builder.onReachableType(TypeReference.of("jakarta.servlet.http.HttpServletRequest"))
						.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS));
	}

}
