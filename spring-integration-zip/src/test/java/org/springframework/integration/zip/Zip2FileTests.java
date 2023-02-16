/*
 * Copyright 2015-2023 the original author or authors.
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

package org.springframework.integration.zip;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 6.1
 */
@SpringJUnitConfig
@DirtiesContext
public class Zip2FileTests {

	@Autowired
	private MessageChannel input;

	@TempDir
	public static File workDir;

	@BeforeAll
	public static void setup() {
		System.setProperty("workDir", workDir.getAbsolutePath());
	}

	@BeforeEach
	public void cleanUp() {
		for (File file : workDir.listFiles()) {
			file.delete();
		}
	}

	@Test
	public void zipStringWithDefaultFileName() {
		final Message<String> message = MessageBuilder.withPayload("Zip me up.").build();
		input.send(message);
		assertThat(workDir.list()).hasSize(1);

		File fileInWorkDir = workDir.listFiles()[0];

		assertThat(fileInWorkDir.isFile()).isTrue();
		assertThat(fileInWorkDir.getName()).contains(message.getHeaders().getId().toString());
		assertThat(fileInWorkDir.getName()).endsWith(".zip");
	}

	@Test
	public void zipStringWithExplicitFileName() {
		input.send(MessageBuilder.withPayload("Zip me up.")
				.setHeader(FileHeaders.FILENAME, "zipString.zip")
				.build());

		assertThat(workDir.list()).hasSize(1);
		assertThat(workDir.list()[0]).isEqualTo("zipString.zip");
	}

	@Test
	public void zipBytesWithExplicitFileName() {
		input.send(MessageBuilder.withPayload("Zip me up.".getBytes())
				.setHeader(FileHeaders.FILENAME, "zipString.zip")
				.build());

		assertThat(workDir.list()).hasSize(1);
		assertThat(workDir.list()[0]).isEqualTo("zipString.zip");
	}

	@Test
	public void zipFile() throws IOException {
		File fileToCompress = File.createTempFile("test1", "tmp");
		FileUtils.writeStringToFile(fileToCompress, "hello world", Charset.defaultCharset());

		input.send(MessageBuilder.withPayload(fileToCompress).build());

		assertThat(workDir.list()).hasSize(1);
		assertThat(workDir.list()[0]).isEqualTo(fileToCompress.getName() + ".zip");
	}

	@Test
	public void zipIterableWithMultipleStrings() {
		String stringToCompress1 = "String1";
		String stringToCompress2 = "String2";
		String stringToCompress3 = "String3";
		String stringToCompress4 = "String4";

		List<String> stringsToCompress = new ArrayList<>(4);

		stringsToCompress.add(stringToCompress1);
		stringsToCompress.add(stringToCompress2);
		stringsToCompress.add(stringToCompress3);
		stringsToCompress.add(stringToCompress4);

		input.send(MessageBuilder.withPayload(stringsToCompress)
				.setHeader(FileHeaders.FILENAME, "zipWith4Strings.zip")
				.build());

		assertThat(workDir.list()).hasSize(1);
		assertThat(workDir.list()[0]).isEqualTo("zipWith4Strings.zip");
	}

	@Test
	public void zipIterableWithDifferentTypes() throws IOException {

		String stringToCompress = "String1";
		byte[] bytesToCompress = "String2".getBytes();
		File fileToCompress = File.createTempFile("test2", "tmp");
		FileUtils.writeStringToFile(fileToCompress, "hello world", Charset.defaultCharset());

		final List<Object> objectsToCompress = new ArrayList<>(3);

		objectsToCompress.add(stringToCompress);
		objectsToCompress.add(bytesToCompress);
		objectsToCompress.add(fileToCompress);

		input.send(MessageBuilder.withPayload(objectsToCompress)
				.setHeader(FileHeaders.FILENAME, "objects-to-compress.zip")
				.build());

		assertThat(workDir.list()).hasSize(1);
		assertThat(workDir.list()[0]).isEqualTo("objects-to-compress.zip");
	}

}
