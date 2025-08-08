/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
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
