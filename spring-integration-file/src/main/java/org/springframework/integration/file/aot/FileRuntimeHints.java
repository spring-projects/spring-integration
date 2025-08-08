/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.file.aot;

import java.util.stream.Stream;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.SerializationHints;
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
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		SerializationHints serializationHints = hints.serialization();
		Stream.of(FileSplitter.FileMarker.class, FileSplitter.FileMarker.Mark.class)
				.forEach(serializationHints::registerType);
	}

}
