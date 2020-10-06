/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.log.LogAccessor;
import org.springframework.expression.Expression;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.file.support.FileUtils;
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
 * @author Alen Turkovic
 */
public class FileWritingMessageHandlerTests {

	static final String DEFAULT_ENCODING = "UTF-8";

	static final String SAMPLE_CONTENT = "HelloWorld\näöüß";


	private File sourceFile;

	@TempDir
	File tempDir;

	private File outputDirectory;

	private FileWritingMessageHandler handler;

	@BeforeEach
	void setup() throws IOException {
		outputDirectory = new File(tempDir, "outputDirectory");
		sourceFile = new File(tempDir, "sourceFile");
		FileCopyUtils.copy(SAMPLE_CONTENT.getBytes(DEFAULT_ENCODING),
				new FileOutputStream(sourceFile, false));

		this.handler = new FileWritingMessageHandler(this.outputDirectory);
		this.handler.setBeanFactory(mock(BeanFactory.class));
		this.handler.setApplicationContext(new GenericApplicationContext());
		this.handler.afterPropertiesSet();

	}

	@Test
	public void unsupportedType() {
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.handler.handleMessage(new GenericMessage<>(99)));
	}

	@Test
	public void permissions() {
		if (FileUtils.IS_POSIX) {
			FileWritingMessageHandler handler = new FileWritingMessageHandler(mock(Expression.class));
			handler.setChmod(0421);
			Set<?> permissions = TestUtils.getPropertyValue(handler, "permissions", Set.class);
			assertThat(permissions.size()).isEqualTo(3);
			assertThat(permissions.contains(PosixFilePermission.OWNER_READ)).isTrue();
			assertThat(permissions.contains(PosixFilePermission.GROUP_WRITE)).isTrue();
			assertThat(permissions.contains(PosixFilePermission.OTHERS_EXECUTE)).isTrue();
			handler.setChmod(0600);
			permissions = TestUtils.getPropertyValue(handler, "permissions", Set.class);
			assertThat(permissions.size()).isEqualTo(2);
			assertThat(permissions.contains(PosixFilePermission.OWNER_READ)).isTrue();
			assertThat(permissions.contains(PosixFilePermission.OWNER_WRITE)).isTrue();
			handler.setChmod(0777);
			permissions = TestUtils.getPropertyValue(handler, "permissions", Set.class);
			assertThat(permissions.size()).isEqualTo(9);
		}
	}

	@Test
	public void supportedTypeAndPermissions() throws Exception {
		if (FileUtils.IS_POSIX) {
			handler.setChmod(0777);
		}
		handler.setOutputChannel(new NullChannel());
		handler.handleMessage(new GenericMessage<>("test"));
		File[] output = outputDirectory.listFiles();
		assertThat(output.length).isEqualTo(1);
		assertThat(output[0]).isNotNull();
		if (FileUtils.IS_POSIX) {
			Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(output[0].toPath());
			assertThat(permissions.size()).isEqualTo(9);
		}
	}

	@Test
	public void stringPayloadCopiedToNewFile() throws Exception {
		long lastModified = 1234000L;
		Message<?> message = MessageBuilder.withPayload(SAMPLE_CONTENT)
				.setHeader(FileHeaders.SET_MODIFIED, lastModified)
				.build();
		QueueChannel output = new QueueChannel();
		handler.setCharset(DEFAULT_ENCODING);
		handler.setOutputChannel(output);
		handler.setPreserveTimestamp(true);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertLastModifiedIs(result, lastModified);
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
		assertThat(destFile.getAbsolutePath()).contains(TestUtils.applySystemFileSeparator("/dir1/dir2/test"));
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
		long lastModified = 12345000L;
		sourceFile.setLastModified(lastModified);
		QueueChannel output = new QueueChannel();
		handler.setOutputChannel(output);
		handler.setPreserveTimestamp(true);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertLastModifiedIs(result, lastModified);
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
	@Disabled("INT-3289: doesn't fail on all OS")
	public void testCreateDirFail() {
		File dir = new File("/foo");
		FileWritingMessageHandler handler = new FileWritingMessageHandler(dir);
		handler.setBeanFactory(mock(BeanFactory.class));
		assertThatIllegalArgumentException()
				.isThrownBy(handler::afterPropertiesSet)
				.withMessageContaining("[/foo] could not be created");
	}

	@Test
	public void deleteFilesFalseByDefault() throws Exception {
		QueueChannel output = new QueueChannel();
		handler.setOutputChannel(output);
		Message<?> message = MessageBuilder.withPayload(sourceFile).build();
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertThat(sourceFile.exists()).isTrue();
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
		assertThat(sourceFile.exists()).isFalse();
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
		assertThat(sourceFile.exists()).isTrue();
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertThat(sourceFile.exists()).isFalse();
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
		assertThat(sourceFile.exists()).isTrue();
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertThat(sourceFile.exists()).isFalse();
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
		assertThat(sourceFile.exists()).isTrue();
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertThat(sourceFile.exists()).isFalse();
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
		assertThat(sourceFile.exists()).isTrue();
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertThat(sourceFile.exists()).isFalse();
	}

	@Test
	public void deleteSourceFileWithInputStreamPayloadAndFileInstanceHeader() throws Exception {
		QueueChannel output = new QueueChannel();
		handler.setCharset(DEFAULT_ENCODING);
		handler.setDeleteSourceFiles(true);
		handler.setOutputChannel(output);

		InputStream is = new FileInputStream(sourceFile);

		Message<?> message = MessageBuilder.withPayload(is)
				.setHeader(FileHeaders.ORIGINAL_FILE, sourceFile)
				.build();
		assertThat(sourceFile.exists()).isTrue();
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertThat(sourceFile.exists()).isFalse();
	}

	@Test
	public void deleteSourceFileWithInputStreamPayloadAndFilePathHeader() throws Exception {
		QueueChannel output = new QueueChannel();
		handler.setCharset(DEFAULT_ENCODING);
		handler.setDeleteSourceFiles(true);
		handler.setOutputChannel(output);

		InputStream is = new FileInputStream(sourceFile);

		Message<?> message = MessageBuilder.withPayload(is)
				.setHeader(FileHeaders.ORIGINAL_FILE, sourceFile.getAbsolutePath())
				.build();
		assertThat(sourceFile.exists()).isTrue();
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIsMatching(result);
		assertThat(sourceFile.exists()).isFalse();
	}

	@Test
	public void customFileNameGenerator() {
		final String anyFilename = "fooBar.test";
		QueueChannel output = new QueueChannel();
		handler.setOutputChannel(output);
		handler.setFileNameGenerator(message -> anyFilename);
		Message<?> message = MessageBuilder.withPayload("test").build();
		handler.handleMessage(message);
		File result = (File) output.receive(0).getPayload();
		assertThat(result.getName()).isEqualTo(anyFilename);
	}

	@Test
	public void existingFileIgnored() throws Exception {
		Message<?> message = MessageBuilder.withPayload(SAMPLE_CONTENT).build();
		QueueChannel output = new QueueChannel();
		File outFile = new File(tempDir, "/outputDirectory/" + message.getHeaders().getId().toString() + ".msg");
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
		File outFile =
				new File(tempDir, "/outputDirectory/" + message.getHeaders().getId().toString() + ".msg.writing");
		FileCopyUtils.copy("foo".getBytes(), new FileOutputStream(outFile));
		handler.setCharset(DEFAULT_ENCODING);
		handler.setOutputChannel(output);
		handler.setFileExistsMode(FileExistsMode.IGNORE);
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		File destFile = (File) result.getPayload();
		assertThat(sourceFile).isNotSameAs(destFile);
		assertThat(destFile.exists()).isFalse();
		assertThat(outFile.exists()).isTrue();
	}

	@Test
	public void existingWritingFileNotIgnoredIfEmptySuffix() throws Exception {
		Message<?> message = MessageBuilder.withPayload(SAMPLE_CONTENT).build();
		QueueChannel output = new QueueChannel();
		File outFile =
				new File(tempDir, "/outputDirectory/" + message.getHeaders().getId().toString() + ".msg.writing");
		FileCopyUtils.copy("foo".getBytes(), new FileOutputStream(outFile));
		handler.setCharset(DEFAULT_ENCODING);
		handler.setOutputChannel(output);
		handler.setFileExistsMode(FileExistsMode.IGNORE);
		handler.setTemporaryFileSuffix("");
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		File destFile = (File) result.getPayload();
		assertThat(sourceFile).isNotSameAs(destFile);
		assertFileContentIsMatching(result);
		assertThat(outFile.exists()).isTrue();
		assertFileContentIs(outFile, "foo");
	}

	@Test
	public void noFlushAppend() throws Exception {
		File tempFolder = new File(tempDir, UUID.randomUUID().toString());
		FileWritingMessageHandler handler = new FileWritingMessageHandler(tempFolder);
		handler.setFileExistsMode(FileExistsMode.APPEND_NO_FLUSH);
		handler.setFileNameGenerator(message -> "foo.txt");
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		handler.setTaskScheduler(taskScheduler);
		handler.setOutputChannel(new NullChannel());
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setFlushInterval(30000);
		handler.afterPropertiesSet();
		handler.start();
		File file = new File(tempFolder, "foo.txt");
		handler.handleMessage(new GenericMessage<>("foo"));
		handler.handleMessage(new GenericMessage<>("bar"));
		handler.handleMessage(new GenericMessage<>("baz"));
		handler.handleMessage(new GenericMessage<>("qux".getBytes())); // change of payload type forces flush
		assertThat(file.length()).isGreaterThanOrEqualTo(9L);
		handler.stop(); // forces flush
		assertThat(file.length()).isEqualTo(12L);
		handler.setFlushInterval(100);
		handler.start();
		handler.handleMessage(new GenericMessage<InputStream>(new ByteArrayInputStream("fiz".getBytes())));
		int n = 0;
		while (n++ < 100 && file.length() < 15) {
			Thread.sleep(100);
		}
		assertThat(file.length()).isEqualTo(15L);
		handler.handleMessage(new GenericMessage<InputStream>(new ByteArrayInputStream("buz".getBytes())));
		handler.trigger(new GenericMessage<>(Matcher.quoteReplacement(file.getAbsolutePath())));
		assertThat(file.length()).isEqualTo(18L);
		assertThat(TestUtils.getPropertyValue(handler, "fileStates", Map.class).size()).isEqualTo(0);

		handler.setFlushInterval(30000);
		final AtomicBoolean called = new AtomicBoolean();
		handler.setFlushPredicate((fileAbsolutePath, firstWrite, lastWrite, triggerMessage) -> {
			called.set(true);
			return true;
		});
		handler.handleMessage(new GenericMessage<InputStream>(new ByteArrayInputStream("box".getBytes())));
		handler.trigger(new GenericMessage<>("foo"));
		assertThat(file.length()).isEqualTo(21L);
		assertThat(called.get()).isTrue();

		handler.handleMessage(new GenericMessage<InputStream>(new ByteArrayInputStream("bux".getBytes())));
		called.set(false);
		handler.flushIfNeeded((fileAbsolutePath, firstWrite, lastWrite) -> {
			called.set(true);
			return true;
		});
		assertThat(file.length()).isEqualTo(24L);
		assertThat(called.get()).isTrue();

		handler.stop();
		LogAccessor logger = spy(TestUtils.getPropertyValue(handler, "logger", LogAccessor.class));
		new DirectFieldAccessor(handler).setPropertyValue("logger", logger);
		when(logger.isDebugEnabled()).thenReturn(true);
		final AtomicInteger flushes = new AtomicInteger();
		doAnswer(i -> {
			flushes.incrementAndGet();
			return null;
		}).when(logger).debug(startsWith("Flushed:"));
		handler.setFlushInterval(50);
		handler.setFlushWhenIdle(false);
		handler.start();
		for (int i = 0; i < 40; i++) {
			handler.handleMessage(new GenericMessage<>("foo"));
			Thread.sleep(5);
		}
		assertThat(flushes.get()).isGreaterThanOrEqualTo(2);
		handler.stop();
	}

	@Test
	public void lockForFlush() throws Exception {
		File tempFolder = new File(tempDir, UUID.randomUUID().toString());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final BufferedOutputStream out = spy(new BufferedOutputStream(baos));
		FileWritingMessageHandler handler = new FileWritingMessageHandler(tempFolder) {

			@Override
			protected BufferedOutputStream createOutputStream(File fileToWriteTo, boolean append) {
				return out;
			}

		};
		handler.setFileExistsMode(FileExistsMode.APPEND_NO_FLUSH);
		handler.setFileNameGenerator(message -> "foo.txt");
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		handler.setTaskScheduler(taskScheduler);
		handler.setOutputChannel(new NullChannel());
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setFlushInterval(10);
		handler.setFlushWhenIdle(false);
		handler.afterPropertiesSet();
		handler.start();

		final AtomicBoolean writing = new AtomicBoolean();
		final AtomicBoolean closeWhileWriting = new AtomicBoolean();
		willAnswer(i -> {
			writing.set(true);
			Thread.sleep(500);
			writing.set(false);
			return null;
		}).given(out).write(any(byte[].class), anyInt(), anyInt());
		willAnswer(i -> {
			closeWhileWriting.compareAndSet(false, writing.get());
			return null;
		}).given(out).close();
		handler.handleMessage(new GenericMessage<>("foo".getBytes()));
		verify(out).write(any(byte[].class), anyInt(), anyInt());
		assertThat(closeWhileWriting.get()).isFalse();
		handler.stop();
	}

	@Test
	public void replaceIfDifferent() throws IOException {
		QueueChannel output = new QueueChannel();
		this.handler.setOutputChannel(output);
		this.handler.setPreserveTimestamp(true);
		this.handler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
		this.handler.handleMessage(MessageBuilder.withPayload("foo")
				.setHeader(FileHeaders.FILENAME, "replaceIfDifferent.txt")
				.setHeader(FileHeaders.SET_MODIFIED, 42_000_000)
				.build());
		Message<?> result = output.receive(0);
		assertFileContentIs(result, "foo");
		assertLastModifiedIs(result, 42_000_000);
		this.handler.handleMessage(MessageBuilder.withPayload("bar")
				.setHeader(FileHeaders.FILENAME, "replaceIfDifferent.txt")
				.setHeader(FileHeaders.SET_MODIFIED, 42_000_000)
				.build());
		result = output.receive(0);
		assertFileContentIs(result, "foo"); // no overwrite - timestamp same
		assertLastModifiedIs(result, 42_000_000);
		this.handler.handleMessage(MessageBuilder.withPayload("bar")
				.setHeader(FileHeaders.FILENAME, "replaceIfDifferent.txt")
				.setHeader(FileHeaders.SET_MODIFIED, 43_000_000)
				.build());
		result = output.receive(0);
		assertFileContentIs(result, "bar");
		assertLastModifiedIs(result, 43_000_000);
	}

	@Test
	public void replaceIfDifferentFile() throws IOException {
		File file = new File(this.tempDir, "foo.txt");
		FileCopyUtils.copy("foo".getBytes(), new FileOutputStream(file));
		file.setLastModified(42_000_000);
		QueueChannel output = new QueueChannel();
		this.handler.setOutputChannel(output);
		this.handler.setPreserveTimestamp(true);
		this.handler.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
		this.handler.handleMessage(MessageBuilder.withPayload(file).build());
		Message<?> result = output.receive(0);
		assertFileContentIs(result, "foo");
		assertLastModifiedIs(result, 42_000_000);
		FileCopyUtils.copy("bar".getBytes(), new FileOutputStream(file));
		file.setLastModified(42_000_000);
		this.handler.handleMessage(MessageBuilder.withPayload(file).build());
		result = output.receive(0);
		assertFileContentIs(result, "foo"); // no overwrite - timestamp same
		assertLastModifiedIs(result, 42_000_000);
		file.setLastModified(43_000_000);
		this.handler.handleMessage(MessageBuilder.withPayload(file).build());
		result = output.receive(0);
		assertFileContentIs(result, "bar");
		assertLastModifiedIs(result, 43_000_000);
	}

	@Test
	public void newFileCallback() throws Exception {
		long lastModified = 1234000L;
		Message<?> message = MessageBuilder.withPayload("bar")
				.setHeader(FileHeaders.SET_MODIFIED, lastModified)
				.build();
		QueueChannel output = new QueueChannel();
		handler.setFileExistsMode(FileExistsMode.APPEND);
		handler.setCharset(DEFAULT_ENCODING);
		handler.setOutputChannel(output);
		handler.setPreserveTimestamp(true);
		handler.setNewFileCallback((file, msg) -> {
			try {
				FileCopyUtils.copy(("foo" + System.lineSeparator()).getBytes(DEFAULT_ENCODING),
						new FileOutputStream(file, false));
			}
			catch (IOException e) {
				fail("unexpected copy exception");
			}
		});
		handler.handleMessage(message);
		Message<?> result = output.receive(0);
		assertFileContentIs(result, "foo" + System.lineSeparator() + "bar");
		assertLastModifiedIs(result, lastModified);
		handler.handleRequestMessage(message);
		assertFileContentIs(result, "foo" + System.lineSeparator() + "barbar");
	}

	void assertFileContentIsMatching(Message<?> result) throws IOException {
		assertFileContentIs(result, SAMPLE_CONTENT);
	}

	void assertFileContentIs(Message<?> result, String expected) throws IOException {
		assertFileContentIs(messageToFile(result), expected);
	}

	void assertLastModifiedIs(Message<?> result, long expected) {
		assertThat(messageToFile(result).lastModified()).isEqualTo(expected);
	}

	void assertFileContentIs(File destFile, String expected) throws IOException {
		assertThat(sourceFile).isNotSameAs(destFile);
		assertThat(destFile.exists()).isTrue();
		byte[] destFileContent = FileCopyUtils.copyToByteArray(destFile);
		assertThat(new String(destFileContent, DEFAULT_ENCODING)).isEqualTo(expected);
	}

	protected File messageToFile(Message<?> result) {
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isInstanceOf(File.class);
		return (File) result.getPayload();
	}

}
