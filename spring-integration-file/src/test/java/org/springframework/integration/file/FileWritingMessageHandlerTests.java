/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.FileCopyUtils;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Alex Peters
 * @author Gary Russell
 * @author Tony Falabella
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class FileWritingMessageHandlerTests {

	static final String DEFAULT_ENCODING = "UTF-8";

	static final String SAMPLE_CONTENT = "HelloWorld\näöüß";


	private File sourceFile;

	@Rule
	public TemporaryFolder temp = new TemporaryFolder() {

		@Override
		public void create() throws IOException {
			super.create();
			outputDirectory = temp.newFolder("outputDirectory");
			handler = new FileWritingMessageHandler(outputDirectory);
			handler.setBeanFactory(mock(BeanFactory.class));
			handler.afterPropertiesSet();
			sourceFile = temp.newFile("sourceFile");
			FileCopyUtils.copy(SAMPLE_CONTENT.getBytes(DEFAULT_ENCODING),
					new FileOutputStream(sourceFile, false));
		}

	};

	private File outputDirectory;

	private FileWritingMessageHandler handler;

	@Before
	public void setup() throws Exception {
		//don't tamper with temp files here, Rule is applied later
	}

	@Test(expected = MessageHandlingException.class)
	public void unsupportedType() throws Exception {
		handler.handleMessage(new GenericMessage<Integer>(99));
		assertThat(outputDirectory.listFiles()[0], nullValue());
	}

	@Test
	public void supportedType() throws Exception {
		handler.setOutputChannel(new NullChannel());
		handler.handleMessage(new GenericMessage<String>("test"));
		assertThat(outputDirectory.listFiles()[0], notNullValue());
	}

	@Test
	public void stringPayloadCopiedToNewFile() throws Exception {
		Message<?> message = MessageBuilder.withPayload(SAMPLE_CONTENT).build();
		QueueChannel output = new QueueChannel();
		handler.setCharset(DEFAULT_ENCODING);
		handler.setOutputChannel(output);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
	}

	@Test
	public void testFileNameHeader() throws Exception {
		Message<?> message = MessageBuilder.withPayload(SAMPLE_CONTENT)
				.setHeader(FileHeaders.FILENAME, "dir1" + File.separator + "dir2/test")
				.build();
		QueueChannel output = new QueueChannel();
		handler.setCharset(DEFAULT_ENCODING);
		handler.setOutputChannel(output);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		File destFile = (File) result.getPayload();
		assertThat(destFile.getAbsolutePath(), containsString(TestUtils.applySystemFileSeparator("/dir1/dir2/test")));
	}

	@Test
	public void stringPayloadCopiedToNewFileWithNewLines() throws Exception {
		Message<?> message = MessageBuilder.withPayload(SAMPLE_CONTENT).build();
		QueueChannel output = new QueueChannel();
		String newLine = System.getProperty("line.separator");
		handler.setCharset(DEFAULT_ENCODING);
		handler.setOutputChannel(output);
		handler.setAppendNewLine(true);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIs(result, SAMPLE_CONTENT + newLine);
	}

	@Test
	public void byteArrayPayloadCopiedToNewFile() throws Exception {
		Message<?> message = MessageBuilder.withPayload(
				SAMPLE_CONTENT.getBytes(DEFAULT_ENCODING)).build();
		QueueChannel output = new QueueChannel();
		handler.setOutputChannel(output);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
	}

	@Test
	public void byteArrayPayloadCopiedToNewFileWithNewLines() throws Exception {
		Message<?> message = MessageBuilder.withPayload(
				SAMPLE_CONTENT.getBytes(DEFAULT_ENCODING)).build();
		QueueChannel output = new QueueChannel();
		String newLine = System.getProperty("line.separator");
		handler.setOutputChannel(output);
		handler.setAppendNewLine(true);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIs(result, SAMPLE_CONTENT + newLine);
	}

	@Test
	public void filePayloadCopiedToNewFile() throws Exception {
		Message<?> message = MessageBuilder.withPayload(sourceFile).build();
		QueueChannel output = new QueueChannel();
		handler.setOutputChannel(output);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
	}

	@Test
	public void filePayloadCopiedToNewFileWithNewLines() throws Exception {
		Message<?> message = MessageBuilder.withPayload(sourceFile).build();
		QueueChannel output = new QueueChannel();
		handler.setOutputChannel(output);
		handler.setAppendNewLine(true);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIs(result, SAMPLE_CONTENT + System.getProperty("line.separator"));
	}

	@Test
	public void inputStreamPayloadCopiedToNewFile() throws Exception {
		InputStream is = new FileInputStream(sourceFile);
		Message<?> message = MessageBuilder.withPayload(is).build();
		QueueChannel output = new QueueChannel();
		handler.setOutputChannel(output);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
	}

	@Test
	public void inputStreamPayloadCopiedToNewFileWithNewLines() throws Exception {
		InputStream is = new FileInputStream(sourceFile);
		Message<?> message = MessageBuilder.withPayload(is).build();
		QueueChannel output = new QueueChannel();
		handler.setOutputChannel(output);
		handler.setAppendNewLine(true);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIs(result, SAMPLE_CONTENT + System.getProperty("line.separator"));
	}

	@Test
	@Ignore("INT-3289: doesn't fail on all OS")
	public void testCreateDirFail() {
		File dir = new File("/foo");
		FileWritingMessageHandler handler = new FileWritingMessageHandler(dir);
		handler.setBeanFactory(mock(BeanFactory.class));
		try {
			handler.afterPropertiesSet();
			fail("Expected exception");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), containsString("[/foo] could not be created"));
		}
	}

	@Test
	public void deleteFilesFalseByDefault() throws Exception {
		QueueChannel output = new QueueChannel();
		handler.setOutputChannel(output);
		Message<?> message = MessageBuilder.withPayload(sourceFile).build();
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertTrue(sourceFile.exists());
	}

	@Test
	public void deleteFilesTrueWithFilePayload() throws Exception {
		QueueChannel output = new QueueChannel();
		handler.setDeleteSourceFiles(true);
		handler.setOutputChannel(output);
		Message<?> message = MessageBuilder.withPayload(sourceFile).build();
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertFalse(sourceFile.exists());
	}

	@Test
	public void deleteSourceFileWithStringPayloadAndFileInstanceHeader() throws Exception {
		QueueChannel output = new QueueChannel();
		handler.setCharset(DEFAULT_ENCODING);
		handler.setDeleteSourceFiles(true);
		handler.setOutputChannel(output);
		Message<?> message = MessageBuilder.withPayload(SAMPLE_CONTENT)
				.setHeader(FileHeaders.ORIGINAL_FILE, sourceFile)
				.build();
		assertTrue(sourceFile.exists());
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertFalse(sourceFile.exists());
	}

	@Test
	public void deleteSourceFileWithStringPayloadAndFilePathHeader() throws Exception {
		QueueChannel output = new QueueChannel();
		handler.setCharset(DEFAULT_ENCODING);
		handler.setDeleteSourceFiles(true);
		handler.setOutputChannel(output);
		Message<?> message = MessageBuilder.withPayload(SAMPLE_CONTENT)
				.setHeader(FileHeaders.ORIGINAL_FILE, sourceFile.getAbsolutePath())
				.build();
		assertTrue(sourceFile.exists());
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertFalse(sourceFile.exists());
	}

	@Test
	public void deleteSourceFileWithByteArrayPayloadAndFileInstanceHeader() throws Exception {
		QueueChannel output = new QueueChannel();
		handler.setCharset(DEFAULT_ENCODING);
		handler.setDeleteSourceFiles(true);
		handler.setOutputChannel(output);
		Message<?> message = MessageBuilder.withPayload(
				SAMPLE_CONTENT.getBytes(DEFAULT_ENCODING))
				.setHeader(FileHeaders.ORIGINAL_FILE, sourceFile)
				.build();
		assertTrue(sourceFile.exists());
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertFalse(sourceFile.exists());
	}

	@Test
	public void deleteSourceFileWithByteArrayPayloadAndFilePathHeader() throws Exception {
		QueueChannel output = new QueueChannel();
		handler.setCharset(DEFAULT_ENCODING);
		handler.setDeleteSourceFiles(true);
		handler.setOutputChannel(output);
		Message<?> message = MessageBuilder.withPayload(
				SAMPLE_CONTENT.getBytes(DEFAULT_ENCODING))
				.setHeader(FileHeaders.ORIGINAL_FILE, sourceFile.getAbsolutePath())
				.build();
		assertTrue(sourceFile.exists());
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertFalse(sourceFile.exists());
	}

	@Test
	public void deleteSourceFileWithInputstreamPayloadAndFileInstanceHeader() throws Exception {
		QueueChannel output = new QueueChannel();
		handler.setCharset(DEFAULT_ENCODING);
		handler.setDeleteSourceFiles(true);
		handler.setOutputChannel(output);

		InputStream is = new FileInputStream(sourceFile);

		Message<?> message = MessageBuilder.withPayload(is)
				.setHeader(FileHeaders.ORIGINAL_FILE, sourceFile)
				.build();
		assertTrue(sourceFile.exists());
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertFalse(sourceFile.exists());
	}

	@Test
	public void deleteSourceFileWithInputstreamPayloadAndFilePathHeader() throws Exception {
		QueueChannel output = new QueueChannel();
		handler.setCharset(DEFAULT_ENCODING);
		handler.setDeleteSourceFiles(true);
		handler.setOutputChannel(output);

		InputStream is = new FileInputStream(sourceFile);

		Message<?> message = MessageBuilder.withPayload(is)
				.setHeader(FileHeaders.ORIGINAL_FILE, sourceFile.getAbsolutePath())
				.build();
		assertTrue(sourceFile.exists());
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertFalse(sourceFile.exists());
	}

	@Test
	public void customFileNameGenerator() throws Exception {
		final String anyFilename = "fooBar.test";
		QueueChannel output = new QueueChannel();
		handler.setOutputChannel(output);
		handler.setFileNameGenerator(new FileNameGenerator() {

			@Override
			public String generateFileName(Message<?> message) {
				return anyFilename;
			}
		});
		Message<?> message = MessageBuilder.withPayload("test").build();
		handler.handleMessage(message);
		File result = (File) output.receive(0).getPayload();
		assertThat(result.getName(), is(anyFilename));
	}

	@Test
	public void existingFileIgnored() throws Exception {
		Message<?> message = MessageBuilder.withPayload(SAMPLE_CONTENT).build();
		QueueChannel output = new QueueChannel();
		File outFile = temp.newFile("/outputDirectory/" + message.getHeaders().getId().toString() + ".msg");
		FileCopyUtils.copy("foo".getBytes(), new FileOutputStream(outFile));
		handler.setCharset(DEFAULT_ENCODING);
		handler.setOutputChannel(output);
		handler.setFileExistsMode(FileExistsMode.IGNORE);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIs(result, "foo");
	}

	@Test
	public void existingWritingFileIgnored() throws Exception {
		Message<?> message = MessageBuilder.withPayload(SAMPLE_CONTENT).build();
		QueueChannel output = new QueueChannel();
		File outFile = temp.newFile("/outputDirectory/" + message.getHeaders().getId().toString() + ".msg.writing");
		FileCopyUtils.copy("foo".getBytes(), new FileOutputStream(outFile));
		handler.setCharset(DEFAULT_ENCODING);
		handler.setOutputChannel(output);
		handler.setFileExistsMode(FileExistsMode.IGNORE);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		File destFile = (File) result.getPayload();
		assertNotSame(destFile, sourceFile);
		assertThat(destFile.exists(), is(false));
		assertThat(outFile.exists(), is(true));
	}

	@Test
	public void existingWritingFileNotIgnoredIfEmptySuffix() throws Exception {
		Message<?> message = MessageBuilder.withPayload(SAMPLE_CONTENT).build();
		QueueChannel output = new QueueChannel();
		File outFile = temp.newFile("/outputDirectory/" + message.getHeaders().getId().toString() + ".msg.writing");
		FileCopyUtils.copy("foo".getBytes(), new FileOutputStream(outFile));
		handler.setCharset(DEFAULT_ENCODING);
		handler.setOutputChannel(output);
		handler.setFileExistsMode(FileExistsMode.IGNORE);
		handler.setTemporaryFileSuffix("");
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		File destFile = (File) result.getPayload();
		assertNotSame(destFile, sourceFile);
		assertFileContentIsMatching(result);
		assertThat(outFile.exists(), is(true));
		assertFileContentIs(outFile, "foo");
	}

	@Test
	public void noFlushAppend() throws Exception {
		File tempFolder = this.temp.newFolder();
		FileWritingMessageHandler handler = new FileWritingMessageHandler(tempFolder);
		handler.setFileExistsMode(FileExistsMode.APPEND_NO_FLUSH);
		handler.setFileNameGenerator(new FileNameGenerator() {

			@Override
			public String generateFileName(Message<?> message) {
				return "foo.txt";
			}
		});
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		handler.setTaskScheduler(taskScheduler);
		handler.setOutputChannel(new NullChannel());
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setFlushInterval(30000);
		handler.afterPropertiesSet();
		handler.start();
		File file = new File(tempFolder, "foo.txt");
		handler.handleMessage(new GenericMessage<String>("foo"));
		handler.handleMessage(new GenericMessage<String>("bar"));
		handler.handleMessage(new GenericMessage<String>("baz"));
		handler.handleMessage(new GenericMessage<byte[]>("qux".getBytes())); // change of payload type forces flush
		assertThat(file.length(), greaterThanOrEqualTo(9L));
		handler.stop(); // forces flush
		assertThat(file.length(), equalTo(12L));
		handler.setFlushInterval(100);
		handler.start();
		handler.handleMessage(new GenericMessage<InputStream>(new ByteArrayInputStream("fiz".getBytes())));
		int n = 0;
		while (n++ < 100 && file.length() < 15) {
			Thread.sleep(100);
		}
		assertThat(file.length(), equalTo(15L));
		handler.handleMessage(new GenericMessage<InputStream>(new ByteArrayInputStream("buz".getBytes())));
		handler.trigger(new GenericMessage<String>(file.getAbsolutePath()));
		assertThat(file.length(), equalTo(18L));
		handler.stop();
	}

	void assertFileContentIsMatching(Message<?> result) throws IOException {
		assertFileContentIs(result, SAMPLE_CONTENT);
	}

	void assertFileContentIs(Message<?> result, String expected) throws IOException {
		assertThat(result, is(notNullValue()));
		assertThat(result.getPayload(), is(instanceOf(File.class)));
		File destFile = (File) result.getPayload();
		assertFileContentIs(destFile, expected);
	}

	void assertFileContentIs(File destFile, String expected) throws IOException {
		assertNotSame(destFile, sourceFile);
		assertThat(destFile.exists(), is(true));
		byte[] destFileContent = FileCopyUtils.copyToByteArray(destFile);
		assertThat(new String(destFileContent, DEFAULT_ENCODING), is(expected));
	}

}
