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
