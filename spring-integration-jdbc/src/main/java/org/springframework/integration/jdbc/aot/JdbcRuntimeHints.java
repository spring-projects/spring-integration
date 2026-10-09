/*
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.jdbc.aot;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * {@link RuntimeHintsRegistrar} for Spring Integration JDBC module.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
class JdbcRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.resources().registerPattern("org/springframework/integration/jdbc/schema-*.sql");
	}

}
