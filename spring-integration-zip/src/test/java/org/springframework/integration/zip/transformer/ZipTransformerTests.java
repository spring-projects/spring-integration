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

package org.springframework.integration.zip.transformer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.zip.ZipHeaders;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 6.1
 */
public class ZipTransformerTests {

	@TempDir
	public File workDir;

	@Test
	public void zipString() {
		final ZipTransformer zipTransformer = new ZipTransformer();
		zipTransformer.setBeanFactory(mock(BeanFactory.class));
		zipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		zipTransformer.afterPropertiesSet();

		final String stringToCompress = "Hello World";

		final Date fileDate = new Date();

		final Message<String> message = MessageBuilder.withPayload(stringToCompress)
				.setHeader(ZipHeaders.ZIP_ENTRY_FILE_NAME, "test.txt")
				.setHeader(ZipHeaders.ZIP_ENTRY_LAST_MODIFIED_DATE, fileDate)
				.build();

		final Message<?> result = zipTransformer.transform(message);

		Object resultPayload = result.getPayload();

		assertThat(resultPayload).isInstanceOf(byte[].class);

		ZipUtil.unpack(new ByteArrayInputStream((byte[]) resultPayload), this.workDir);

		final File unzippedEntry = new File(this.workDir, "test.txt");
		assertThat(unzippedEntry).exists().isFile();

		//See https://stackoverflow.com/questions/3725662/what-is-the-earliest-timestamp-value-that-is-supported-in-zip-file-format
		assertThat(unzippedEntry.lastModified())
				.isGreaterThan(fileDate.getTime() - 3000)
				.isLessThan(fileDate.getTime() + 3000);
	}

	@Test
	public void zipStringCollection() {
		final ZipTransformer zipTransformer = new ZipTransformer();
		zipTransformer.setBeanFactory(mock(BeanFactory.class));
		zipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		zipTransformer.afterPropertiesSet();

		final String string1ToCompress = "Cartman";
		final String string2ToCompress = "Kenny";
		final String string3ToCompress = "Butters";

		final List<String> strings = new ArrayList<>(3);

		strings.add(string1ToCompress);
		strings.add(string2ToCompress);
		strings.add(string3ToCompress);

		final Date fileDate = new Date();

		final Message<List<String>> message = MessageBuilder.withPayload(strings)
				.setHeader(ZipHeaders.ZIP_ENTRY_FILE_NAME, "test.txt")
				.setHeader(ZipHeaders.ZIP_ENTRY_LAST_MODIFIED_DATE, fileDate)
				.build();

		final Message<?> result = zipTransformer.transform(message);

		Object resultPayload = result.getPayload();

		assertThat(resultPayload).isInstanceOf(byte[].class);

		ZipUtil.unpack(new ByteArrayInputStream((byte[]) resultPayload), this.workDir);

		File[] files = this.workDir.listFiles();

		assertThat(files).hasSizeGreaterThanOrEqualTo(3);

		final Set<String> expectedFileNames = new HashSet<>();

		expectedFileNames.add("test_1.txt");
		expectedFileNames.add("test_2.txt");
		expectedFileNames.add("test_3.txt");

		for (File file : files) {
			if (file.getName().startsWith("test")) {
				assertThat(file).exists().isFile();

				//See https://stackoverflow.com/questions/3725662/what-is-the-earliest-timestamp-value-that-is-supported-in-zip-file-format
				assertThat(file.lastModified())
						.isLessThan(fileDate.getTime() + 4000)
						.isGreaterThan(fileDate.getTime() - 4000);

				assertThat(file).hasExtension("txt");

				assertThat(expectedFileNames).contains(file.getName());
			}
		}
	}

	@Test
	public void zipStringToFile() {
		final ZipTransformer zipTransformer = new ZipTransformer();
		zipTransformer.setBeanFactory(mock(BeanFactory.class));
		zipTransformer.afterPropertiesSet();

		final String stringToCompress = "Hello World";

		final String zipEntryFileName = "test.txt";
		final Message<String> message = MessageBuilder.withPayload(stringToCompress)
				.setHeader(ZipHeaders.ZIP_ENTRY_FILE_NAME, zipEntryFileName)
				.build();

		final Message<?> result = zipTransformer.transform(message);

		assertThat(result.getPayload()).isInstanceOf(File.class);

		final File payload = (File) result.getPayload();

		assertThat(payload).hasName(message.getHeaders().getId().toString() + ".msg.zip");
		assertThat((SpringZipUtils.isValid(payload))).isTrue();

		final byte[] zipEntryData = ZipUtil.unpackEntry(payload, "test.txt");

		assertThat(zipEntryData).isNotNull();
		assertThat(new String(zipEntryData)).isEqualTo("Hello World");
	}

	@Test
	public void zipFile() {

		ZipTransformer zipTransformer = new ZipTransformer();
		zipTransformer.setBeanFactory(mock(BeanFactory.class));
		zipTransformer.setDeleteFiles(true);
		zipTransformer.afterPropertiesSet();

		final File testFile = createTestFile(10);

		assertThat(testFile).exists();

		final Message<File> message = MessageBuilder.withPayload(testFile).build();

		final Message<?> result = zipTransformer.transform(message);

		assertThat(result.getPayload()).isInstanceOf(File.class);

		final File payload = (File) result.getPayload();

		assertThat(payload).hasName(testFile.getName() + ".zip");
		assertThat(SpringZipUtils.isValid(payload)).isTrue();
	}

	@Test
	public void zipCollection() {

		final File testFile1 = createTestFile(1);
		final File testFile2 = createTestFile(2);
		final File testFile3 = createTestFile(3);
		final File testFile4 = createTestFile(4);

		assertThat(testFile1).exists();
		assertThat(testFile2).exists();
		assertThat(testFile3).exists();
		assertThat(testFile4).exists();

		final Collection<File> files = new ArrayList<>();

		files.add(testFile1);
		files.add(testFile2);
		files.add(testFile3);
		files.add(testFile4);

		final ZipTransformer zipTransformer = new ZipTransformer();
		zipTransformer.setBeanFactory(mock(BeanFactory.class));
		zipTransformer.afterPropertiesSet();

		final Message<Collection<File>> message = MessageBuilder.withPayload(files).build();

		final Message<?> result = zipTransformer.transform(message);

		assertThat(result.getPayload()).isInstanceOf(File.class);

		final File outputZipFile = (File) result.getPayload();

		assertThat(outputZipFile).exists().isFile().hasExtension("zip");
		assertThat(SpringZipUtils.isValid(outputZipFile)).isTrue();
	}

	private File createTestFile(int size) {
		final File testFile = new File(this.workDir, "testdata" + UUID.randomUUID().toString() + ".data");

		try (RandomAccessFile f = new RandomAccessFile(testFile, "rw")) {
			f.setLength((long) size * 1024 * 1024);
		}
		catch (Exception e) {
			// Ignore
		}
		return testFile;

	}

}
