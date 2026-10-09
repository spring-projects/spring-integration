/*
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.file.aot;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.integration.file.splitter.FileSplitter;

/**
 * {@link RuntimeHintsRegistrar} for Spring Integration file module.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
class FileRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		ReflectionHints reflectionHints = hints.reflection();

		Stream.of(FileSplitter.FileMarker.class, FileSplitter.FileMarker.Mark.class)
				.forEach(clazz -> reflectionHints.registerType(clazz,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
						MemberCategory.INVOKE_PUBLIC_METHODS));
	}

}
