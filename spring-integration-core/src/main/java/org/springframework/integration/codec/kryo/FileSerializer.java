/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
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
