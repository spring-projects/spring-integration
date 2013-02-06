/*
 * Copyright 2002-2009 the original author or authors.
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

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Alex Peters
 */
public class FileWritingMessageHandlerTests {

	static final String DEFAULT_ENCODING = "UTF-8";

	static final String SAMPLE_CONTENT = "HelloWorld\näöüß";


	private File sourceFile;

	@Rule
	public TemporaryFolder temp = new TemporaryFolder() {
		public void create() throws IOException {
			super.create();
			outputDirectory = temp.newFolder("outputDirectory");
			handler = new FileWritingMessageHandler(outputDirectory);
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
	public void filePayloadCopiedToNewFile() throws Exception {
		Message<?> message = MessageBuilder.withPayload(sourceFile).build();
		QueueChannel output = new QueueChannel();
		handler.setOutputChannel(output);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
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
	public void customFileNameGenerator() throws Exception {
		final String anyFilename = "fooBar.test";
		QueueChannel output = new QueueChannel();
		handler.setOutputChannel(output);
		handler.setFileNameGenerator(new FileNameGenerator() {
			public String generateFileName(Message<?> message) {
				return anyFilename;
			}
		});
		Message<?> message = MessageBuilder.withPayload("test").build();
		handler.handleMessage(message);
		File result = (File) output.receive(0).getPayload();
		assertThat(result.getName(), is(anyFilename));
	}

	void assertFileContentIsMatching(Message<?> result) throws IOException, UnsupportedEncodingException {
		assertThat(result, is(notNullValue()));
		assertThat(result.getPayload(), instanceOf(File.class));
		File destFile = (File) result.getPayload();
		assertNotSame(destFile, sourceFile);
		assertThat(destFile.exists(), is(true));
		byte[] destFileContent = FileCopyUtils.copyToByteArray(destFile);
		assertThat(new String(destFileContent, DEFAULT_ENCODING), is(SAMPLE_CONTENT));
	}

}
