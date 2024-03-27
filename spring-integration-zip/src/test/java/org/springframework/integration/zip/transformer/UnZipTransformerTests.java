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

package org.springframework.integration.zip.transformer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.zeroturnaround.zip.ZipException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Ingo Dueppe
 *
 * @since 6.1
 */
@SpringJUnitConfig
@DirtiesContext
public class UnZipTransformerTests {

	@TempDir
	public File workDir;

	@Autowired
	private ResourceLoader resourceLoader;

	@Test
	public void unzipFlatFileEntryZip() throws IOException {
		final Resource zipResource = this.resourceLoader.getResource("classpath:testzipdata/flatfileentry.zip");
		final InputStream is = zipResource.getInputStream();

		final Message<InputStream> message = MessageBuilder.withPayload(is).build();

		final UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.FILE);
		unZipTransformer.afterPropertiesSet();

		final Message<?> resultMessage = unZipTransformer.transform(message);

		assertThat(resultMessage).isNotNull();

		@SuppressWarnings("unchecked")
		Map<String, byte[]> unzippedData = (Map<String, byte[]>) resultMessage.getPayload();

		assertThat(unzippedData).isNotNull().hasSize(1);
	}

	@Test
	public void unzipSingleFileAsInputStreamToByteArray() throws IOException {
		final Resource resource = this.resourceLoader.getResource("classpath:testzipdata/single.zip");
		final InputStream is = resource.getInputStream();

		final Message<InputStream> message = MessageBuilder.withPayload(is).build();

		final UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		unZipTransformer.afterPropertiesSet();

		final Message<?> resultMessage = unZipTransformer.transform(message);

		assertThat(resultMessage).isNotNull();

		@SuppressWarnings("unchecked")
		Map<String, byte[]> unzippedData = (Map<String, byte[]>) resultMessage.getPayload();

		assertThat(unzippedData).isNotNull().hasSize(1);
		assertThat(new String(unzippedData.values().iterator().next())).isEqualTo("Spring Integration Rocks!");
	}

	@Test
	public void unzipSingleFileToByteArray() throws IOException {
		final Resource resource = this.resourceLoader.getResource("classpath:testzipdata/single.zip");
		final InputStream is = resource.getInputStream();

		final File inputFile = new File(this.workDir, "unzipSingleFileToByteArray");

		FileOutputStream out = new FileOutputStream(inputFile);
		IOUtils.copy(is, out);
		is.close();
		out.close();

		final Message<File> message = MessageBuilder.withPayload(inputFile).build();

		final UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		unZipTransformer.afterPropertiesSet();

		final Message<?> resultMessage = unZipTransformer.transform(message);

		assertThat(resultMessage).isNotNull();

		@SuppressWarnings("unchecked")
		Map<String, byte[]> unzippedData = (Map<String, byte[]>) resultMessage.getPayload();

		assertThat(unzippedData).isNotNull().hasSize(1);
		assertThat(inputFile).exists();
		assertThat(new String(unzippedData.values().iterator().next())).isEqualTo("Spring Integration Rocks!");
	}

	@Test
	public void unzipSingleFileToByteArrayWithDeleteFilesTrue() throws IOException {
		final Resource resource = this.resourceLoader.getResource("classpath:testzipdata/single.zip");
		final InputStream is = resource.getInputStream();

		final File inputFile = new File(this.workDir, "unzipSingleFileToByteArray");

		FileOutputStream output = new FileOutputStream(inputFile);
		IOUtils.copy(is, output);
		is.close();
		output.close();

		final Message<File> message = MessageBuilder.withPayload(inputFile).build();

		final UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		unZipTransformer.setDeleteFiles(true);
		unZipTransformer.afterPropertiesSet();

		final Message<?> resultMessage = unZipTransformer.transform(message);

		assertThat(resultMessage).isNotNull();

		@SuppressWarnings("unchecked")
		Map<String, byte[]> unzippedData = (Map<String, byte[]>) resultMessage.getPayload();

		assertThat(unzippedData).isNotNull().hasSize(1);
		assertThat(inputFile).doesNotExist();
		assertThat(new String(unzippedData.values().iterator().next())).isEqualTo("Spring Integration Rocks!");
	}

	@Test
	public void unzipMultipleFilesAsInputStreamToByteArray() throws IOException {
		final Resource resource = this.resourceLoader.getResource("classpath:testzipdata/countries.zip");
		final InputStream is = resource.getInputStream();

		final Message<InputStream> message = MessageBuilder.withPayload(is).build();

		final UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		unZipTransformer.afterPropertiesSet();

		final Message<?> resultMessage = unZipTransformer.transform(message);

		assertThat(resultMessage).isNotNull();

		@SuppressWarnings("unchecked")
		Map<String, byte[]> unzippedData = (Map<String, byte[]>) resultMessage.getPayload();

		assertThat(unzippedData).isNotNull().hasSize(5);
	}

	@Test
	public void unzipMultipleFilesAsInputStreamWithExpectSingleResultTrue() throws IOException {
		final Resource resource = this.resourceLoader.getResource("classpath:testzipdata/countries.zip");
		final InputStream is = resource.getInputStream();

		final Message<InputStream> message = MessageBuilder.withPayload(is).build();

		final UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		unZipTransformer.setExpectSingleResult(true);
		unZipTransformer.afterPropertiesSet();

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> unZipTransformer.transform(message))
				.withStackTraceContaining("The UnZip operation extracted 5 result objects " +
						"but expectSingleResult was 'true'.");
	}

	@Test
	public void unzipInvalidZipFile() throws IOException {
		File fileToUnzip = File.createTempFile("test1", "tmp");
		FileUtils.writeStringToFile(fileToUnzip, "hello world", Charset.defaultCharset());

		UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		unZipTransformer.setExpectSingleResult(true);
		unZipTransformer.afterPropertiesSet();

		Message<File> message = MessageBuilder.withPayload(fileToUnzip).build();

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> unZipTransformer.transform(message))
				.withStackTraceContaining(String.format("Not a zip file: %s", fileToUnzip.getAbsolutePath()));
	}

	@Test
	public void testUnzipMaliciousTraversalZipFile() throws IOException {
		final Resource resource = this.resourceLoader.getResource("classpath:testzipdata/zip-malicious-traversal.zip");
		final InputStream is = resource.getInputStream();

		final Message<InputStream> message = MessageBuilder.withPayload(is).build();

		final UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.afterPropertiesSet();

		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> unZipTransformer.transform(message))
				.withRootCauseInstanceOf(ZipException.class)
				.withStackTraceContaining("is trying to leave the target output directory");
	}

	@Configuration
	public static class TestConfiguration {

	}

}
