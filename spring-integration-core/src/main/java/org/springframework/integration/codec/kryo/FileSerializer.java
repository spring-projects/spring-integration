/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.integration.codec.kryo;

import java.io.File;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * A custom Kryo {@link Serializer} for serializing File payloads.
 * It serializes the file path and creates a new File instance to preserve the original path.
 * File does not preserve the absolute otherwise as <em>prefixLength</em>
 * is declared transient.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class FileSerializer extends Serializer<File> {

	@Override
	public void write(Kryo kryo, Output output, File file) {
		output.writeString(file.getPath());
	}

	@Override
	public File read(Kryo kryo, Input input, Class<? extends File> type) {
		String path = input.readString();
		return new File(path);
	}

}
