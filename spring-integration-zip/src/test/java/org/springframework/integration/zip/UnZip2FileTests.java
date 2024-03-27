/*
 * Copyright 2015-2024 the original author or authors.
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
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.zeroturnaround.zip.ZipException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 6.1
 */
@SpringJUnitConfig
@DirtiesContext
public class UnZip2FileTests {

	@Autowired
	private ApplicationContext context;

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
		cleanupDirectory(workDir);
	}

	@Test
	public void unZipWithOneEntry() throws Exception {
		final Resource resource = this.context.getResource("classpath:testzipdata/single.zip");
		final InputStream is = resource.getInputStream();

		byte[] zipdata = IOUtils.toByteArray(is);
		is.close();

		final Message<byte[]> message = MessageBuilder.withPayload(zipdata).build();

		input.send(message);

		assertThat(workDir.list()).hasSize(1);

		File fileInWorkDir = workDir.listFiles()[0];

		assertThat(fileInWorkDir).isFile();
		assertThat(fileInWorkDir).hasName("single.txt");
	}

	@Test
	public void unZipWithMultipleEntries() throws Exception {
		final Resource resource = this.context.getResource("classpath:testzipdata/countries.zip");
		final InputStream is = resource.getInputStream();

		byte[] zipdata = IOUtils.toByteArray(is);
		is.close();

		final Message<byte[]> message = MessageBuilder.withPayload(zipdata).build();

		input.send(message);

		assertThat(workDir.list()).hasSize(4);

		File[] files = workDir.listFiles();

		boolean continents = false;
		boolean de = false;
		boolean fr = false;
		boolean pl = false;

		for (File file : files) {
			if (file.getName().equals("continents")) {
				continents = true;
				assertThat(file).isDirectory();
				assertThat(file.list()).hasSize(2);
			}
			if (file.getName().equals("de.txt")) {
				de = true;
				assertThat(file).isFile();
			}
			if (file.getName().equals("fr.txt")) {
				fr = true;
				assertThat(file).isFile();
			}
			if (file.getName().equals("pl.txt")) {
				pl = true;
				assertThat(file).isFile();
			}
		}

		assertThat(continents).isTrue();
		assertThat(de).isTrue();
		assertThat(fr).isTrue();
		assertThat(pl).isTrue();
	}

	@Test
	public void unZipTraversal() throws Exception {
		final Resource resource = this.context.getResource("classpath:testzipdata/zip-malicious-traversal.zip");
		final InputStream is = resource.getInputStream();
		byte[] zipdata = IOUtils.toByteArray(is);
		final Message<byte[]> message = MessageBuilder.withPayload(zipdata).build();
		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> input.send(message))
				.withRootCauseInstanceOf(ZipException.class)
				.withStackTraceContaining("is trying to leave the target output directory");
	}

	private static void cleanupDirectory(File dir) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				cleanupDirectory(file);
			}
			file.delete();
		}
	}

}
