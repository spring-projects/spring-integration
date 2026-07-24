/*
 * Copyright 2015-present the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Ingo Dueppe
 * @author Glenn Renfro
 *
 * @since 6.1
 */
@SpringJUnitConfig
@DirtiesContext
public class UnZipTransformerTests {

	@TempDir
	public File workDir;

	@Autowired
	private ApplicationContext testApplicationContext;

	@Autowired
	private ResourceLoader resourceLoader;

	@Test
	public void unzipFlatFileEntryZip() throws IOException {
		final Resource zipResource = this.resourceLoader.getResource("classpath:testzipdata/flatfileentry.zip");
		final InputStream is = zipResource.getInputStream();

		final Message<InputStream> message = MessageBuilder.withPayload(is).build();

		final UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.FILE);
		unZipTransformer.setBeanFactory(this.testApplicationContext);
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
		unZipTransformer.setBeanFactory(this.testApplicationContext);
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
		unZipTransformer.setBeanFactory(this.testApplicationContext);
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
		unZipTransformer.setBeanFactory(this.testApplicationContext);
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
		unZipTransformer.setBeanFactory(this.testApplicationContext);
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
		unZipTransformer.setBeanFactory(this.testApplicationContext);
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
		unZipTransformer.setBeanFactory(this.testApplicationContext);
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
		unZipTransformer.setBeanFactory(this.testApplicationContext);
		unZipTransformer.afterPropertiesSet();

		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> unZipTransformer.transform(message))
				.withRootCauseInstanceOf(ZipException.class)
				.withStackTraceContaining("is trying to leave the target output directory");
	}

	@Test
	public void testMaxUncompressedSizeBreach() throws IOException {
		InputStream is = createZip(1, 2048);
		Message<InputStream> message = MessageBuilder.withPayload(is).build();

		UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		unZipTransformer.setMaxUncompressedSize(1024);
		unZipTransformer.setBeanFactory(this.testApplicationContext);
		unZipTransformer.afterPropertiesSet();

		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> unZipTransformer.transform(message))
				.withStackTraceContaining("Exceeded max uncompressed size");
	}

	@Test
	public void testCumulativeSizeBreach() throws IOException {
		InputStream is = createZip(3, 1024);
		Message<InputStream> message = MessageBuilder.withPayload(is).build();

		UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		unZipTransformer.setMaxUncompressedSize(2048);
		unZipTransformer.setBeanFactory(this.testApplicationContext);
		unZipTransformer.afterPropertiesSet();

		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> unZipTransformer.transform(message))
				.withStackTraceContaining("Exceeded max uncompressed size");
	}

	@Test
	public void testMaxEntryCountBreach() throws IOException {
		InputStream is = createZip(6, 0);
		Message<InputStream> message = MessageBuilder.withPayload(is).build();

		UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		unZipTransformer.setMaxEntryCount(5);
		unZipTransformer.setBeanFactory(this.testApplicationContext);
		unZipTransformer.afterPropertiesSet();

		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> unZipTransformer.transform(message))
				.withStackTraceContaining("Exceeded max entry count of 5");
	}

	@Test
	public void testMaxCompressionRatioBreach() throws Exception {
		File tempFile = new File(this.workDir, "test.zip");
		File sourceFile = new File(this.workDir, "test.txt");
		byte[] zeros = new byte[102400];
		FileUtils.writeByteArrayToFile(sourceFile, zeros);
		ZipUtil.packEntry(sourceFile, tempFile);

		Message<File> message = MessageBuilder.withPayload(tempFile).build();

		UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		unZipTransformer.setMaxCompressionRatio(10.0);
		unZipTransformer.setBeanFactory(this.testApplicationContext);
		unZipTransformer.afterPropertiesSet();

		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> unZipTransformer.transform(message))
				.withStackTraceContaining("Exceeded max compression ratio");
	}

	@Test
	public void testMaxUncompressedSizeBreachMidBuffer() throws IOException {
		// 20000 bytes exceed one 8192-byte internal read buffer, so the 10000-byte
		// limit is breached partway through the second chunk read, not on a chunk boundary.
		InputStream is = createZip(1, 20000);
		Message<InputStream> message = MessageBuilder.withPayload(is).build();

		UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		unZipTransformer.setMaxUncompressedSize(10000);
		unZipTransformer.setBeanFactory(this.testApplicationContext);
		unZipTransformer.afterPropertiesSet();

		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> unZipTransformer.transform(message))
				.withStackTraceContaining("Exceeded max uncompressed size");
	}

	@Test
	public void testFileModeParityAndCleanup() throws IOException {
		InputStream is = createZip(1, 2048);
		Message<InputStream> message = MessageBuilder.withPayload(is).build();

		UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.FILE);
		unZipTransformer.setWorkDirectory(this.workDir);
		unZipTransformer.setMaxUncompressedSize(1024);
		unZipTransformer.setBeanFactory(this.testApplicationContext);
		unZipTransformer.afterPropertiesSet();

		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> unZipTransformer.transform(message))
				.withStackTraceContaining("Exceeded max uncompressed size");

		// Verify that partial (exceeded maxUncompressed size) files and tempDir were deleted
		File tempDir = new File(this.workDir, message.getHeaders().getId().toString());
		assertThat(tempDir).doesNotExist();
	}

	@Test
	public void testFileModeParityAndCleanupWithMultipleEntries() throws IOException {
		InputStream is = createZip(3, 1024);
		Message<InputStream> message = MessageBuilder.withPayload(is).build();

		UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.FILE);
		unZipTransformer.setWorkDirectory(this.workDir);
		unZipTransformer.setMaxUncompressedSize(2048);
		unZipTransformer.setBeanFactory(this.testApplicationContext);
		unZipTransformer.afterPropertiesSet();

		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> unZipTransformer.transform(message))
				.withStackTraceContaining("Exceeded max uncompressed size");

		// Verify that previously extracted files and tempDir were deleted
		File tempDir = new File(this.workDir, message.getHeaders().getId().toString());
		assertThat(tempDir).doesNotExist();
	}

	@Test
	public void testFileModeMaxUncompressedSizeBreachMidBufferCleanup() throws IOException {
		// 20000 bytes exceed one 8192-byte internal read buffer, so the 10000-byte
		// limit is breached partway through the second chunk read, not on a chunk boundary.
		InputStream is = createZip(1, 20000);
		Message<InputStream> message = MessageBuilder.withPayload(is).build();

		UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.FILE);
		unZipTransformer.setWorkDirectory(this.workDir);
		unZipTransformer.setMaxUncompressedSize(10000);
		unZipTransformer.setBeanFactory(this.testApplicationContext);
		unZipTransformer.afterPropertiesSet();

		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> unZipTransformer.transform(message))
				.withStackTraceContaining("Exceeded max uncompressed size");

		// Verify that the partially written file and tempDir were deleted even though
		// the breach occurred mid-chunk (i.e. after a partial buffer was already flushed to disk).
		File tempDir = new File(this.workDir, message.getHeaders().getId().toString());
		assertThat(tempDir).doesNotExist();
	}

	private static InputStream createZip(int entries, int sizePerEntry) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			for (int i = 0; i < entries; i++) {
				ZipEntry entry = new ZipEntry("file" + i + ".txt");
				zos.putNextEntry(entry);
				if (sizePerEntry > 0) {
					byte[] data = new byte[sizePerEntry];
					zos.write(data);
				}
				zos.closeEntry();
			}
		}
		return new ByteArrayInputStream(baos.toByteArray());
	}

	@Test
	@EnabledOnOs(value = {OS.LINUX, OS.MAC},
			disabledReason = "Requires Files.createSymbolicLink(), which needs elevated privilege on Windows")
	public void workDirectoryAsSymbolicLinkIsRejected() throws IOException {
		File realTargetDir = new File(this.workDir, "real-target");
		realTargetDir.mkdirs();

		File symlinkedWorkDir = new File(this.workDir, "ziptransformer-symlink");
		Files.createSymbolicLink(symlinkedWorkDir.toPath(), realTargetDir.toPath());

		UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setWorkDirectory(symlinkedWorkDir);
		unZipTransformer.setBeanFactory(this.testApplicationContext);

		assertThatIllegalArgumentException()
				.isThrownBy(unZipTransformer::afterPropertiesSet)
				.withMessageContaining("must not be a symbolic link");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	public static class TestConfiguration {

	}

}
