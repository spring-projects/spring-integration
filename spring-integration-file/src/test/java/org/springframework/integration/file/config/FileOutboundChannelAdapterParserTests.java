/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.file.config;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.outbound.FileWritingMessageHandler;
import org.springframework.integration.file.outbound.FileWritingMessageHandler.MessageFlushPredicate;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.file.support.FileUtils;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Tony Falabella
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class FileOutboundChannelAdapterParserTests {

	@Autowired
	EventDrivenConsumer simpleAdapter;

	@Autowired
	EventDrivenConsumer adapterWithCustomNameGenerator;

	@Autowired
	EventDrivenConsumer adapterWithDeleteFlag;

	@Autowired
	EventDrivenConsumer adapterWithOrder;

	@Autowired
	EventDrivenConsumer adapterWithFlushing;

	@Autowired
	EventDrivenConsumer adapterWithCharset;

	@Autowired
	EventDrivenConsumer adapterWithDirectoryExpression;

	@Autowired
	EventDrivenConsumer adapterWithAppendNewLine;

	@Autowired
	MessageChannel usageChannel;

	@Autowired
	MessageChannel adapterUsageWithAppendAndAppendNewLineTrue;

	@Autowired
	MessageChannel adapterUsageWithAppendAndAppendNewLineFalse;

	@Autowired
	MessageChannel usageChannelWithFailMode;

	@Autowired
	MessageChannel usageChannelWithIgnoreMode;

	@Autowired
	MessageChannel usageChannelConcurrent;

	@Autowired
	CountDownLatch fileWriteLatch;

	@Autowired
	MessageFlushPredicate predicate;

	private static volatile int adviceCalled;

	@Test
	public void simpleAdapter() {
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(simpleAdapter);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				adapterAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		File expected = new File(System.getProperty("java.io.tmpdir"));

		Expression destinationDirectoryExpression =
				(Expression) handlerAccessor.getPropertyValue("destinationDirectoryExpression");
		File actual = new File(destinationDirectoryExpression.getExpressionString());
		assertThat(TestUtils.<String>getPropertyValue(handler, "temporaryFileSuffix")).isEqualTo(".foo");
		assertThat(actual).isEqualTo(expected);
		DefaultFileNameGenerator fileNameGenerator =
				(DefaultFileNameGenerator) handlerAccessor.getPropertyValue("fileNameGenerator");
		assertThat(fileNameGenerator).isNotNull();
		Expression expression = TestUtils.getPropertyValue(fileNameGenerator, "expression");
		assertThat(expression).isNotNull();
		assertThat(expression.getExpressionString()).isEqualTo("'foo.txt'");
		assertThat(handlerAccessor.getPropertyValue("deleteSourceFiles")).isEqualTo(Boolean.FALSE);
		assertThat(handlerAccessor.getPropertyValue("flushWhenIdle")).isEqualTo(Boolean.TRUE);
		if (FileUtils.IS_POSIX) {
			assertThat(TestUtils.<Set<PosixFilePermission>>getPropertyValue(handler, "permissions").size())
					.isEqualTo(9);
		}
		assertThat(handlerAccessor.getPropertyValue("preserveTimestamp")).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void adapterWithCustomFileNameGenerator() {
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapterWithCustomNameGenerator);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				adapterAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		File expected = new File(System.getProperty("java.io.tmpdir"));

		Expression destinationDirectoryExpression =
				(Expression) handlerAccessor.getPropertyValue("destinationDirectoryExpression");
		File actual = new File(destinationDirectoryExpression.getExpressionString());

		assertThat(actual).isEqualTo(expected);
		assertThat(handlerAccessor.getPropertyValue("fileNameGenerator")).isInstanceOf(CustomFileNameGenerator.class);
		assertThat(handlerAccessor.getPropertyValue("temporaryFileSuffix")).isEqualTo(".writing");
		assertThat(handlerAccessor.getPropertyValue("flushWhenIdle")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void adapterWithDeleteFlag() {
		assertThat(TestUtils.<Boolean>getPropertyValue(adapterWithDeleteFlag, "handler.deleteSourceFiles"))
				.isEqualTo(Boolean.TRUE);
	}

	@Test
	public void adapterWithOrder() {
		assertThat(TestUtils.<Integer>getPropertyValue(adapterWithOrder, "handler.order")).isEqualTo(555);
	}

	@Test
	public void adapterWithFlushing() {
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapterWithFlushing);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				adapterAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertThat(handlerAccessor.getPropertyValue("bufferSize")).isEqualTo(4096);
		assertThat(handlerAccessor.getPropertyValue("flushInterval")).isEqualTo(12345L);
		assertThat(handlerAccessor.getPropertyValue("fileExistsMode")).isEqualTo(FileExistsMode.APPEND_NO_FLUSH);
		assertThat(handlerAccessor.getPropertyValue("flushPredicate")).isSameAs(this.predicate);
	}

	@Test
	public void adapterWithAutoStartupFalse() {
		assertThat(TestUtils.<Boolean>getPropertyValue(adapterWithOrder, "autoStartup"))
				.isEqualTo(Boolean.FALSE);
	}

	@Test
	public void adapterWithCharset() {
		assertThat(TestUtils.<Object>getPropertyValue(adapterWithCharset, "handler.charset")).
				isEqualTo(StandardCharsets.UTF_8);
	}

	@Test
	public void adapterWithDirectoryExpression() {
		FileWritingMessageHandler handler =
				TestUtils.<FileWritingMessageHandler>getPropertyValue(adapterWithDirectoryExpression, "handler");
		Method m = ReflectionUtils.findMethod(FileWritingMessageHandler.class, "getTemporaryFileSuffix");
		ReflectionUtils.makeAccessible(m);
		assertThat(ReflectionUtils.invokeMethod(m, handler)).isEqualTo(".writing");
		String expectedExpressionString = "'foo/bar'";
		String actualExpressionString =
				TestUtils.<Expression>getPropertyValue(handler, "destinationDirectoryExpression")
						.getExpressionString();
		assertThat(actualExpressionString).isEqualTo(expectedExpressionString);

	}

	@Test
	public void adapterUsageWithAppend() throws Exception {
		String expectedFileContent = "Initial File Content:String content:byte[] content:File content";

		File testFile = new File("test/fileToAppend.txt");
		if (testFile.exists()) {
			testFile.delete();
		}
		usageChannel.send(new GenericMessage<>("Initial File Content:"));
		usageChannel.send(new GenericMessage<>("String content:"));
		usageChannel.send(new GenericMessage<>("byte[] content:".getBytes()));
		usageChannel.send(new GenericMessage<>(new File("test/input.txt")));

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertThat(actualFileContent).isEqualTo(expectedFileContent);
		assertThat(adviceCalled).isEqualTo(4);
		testFile.delete();
	}

	@Test
	public void adapterUsageWithAppendAndAppendNewLineTrue() throws Exception {
		assertThat(TestUtils.<Boolean>getPropertyValue(this.adapterWithAppendNewLine, "handler.appendNewLine"))
				.isEqualTo(Boolean.TRUE);
		String newLine = System.getProperty("line.separator");
		String expectedFileContent = "Initial File Content:" + newLine + "String content:" + newLine +
				"byte[] content:" + newLine + "File content" + newLine;

		File testFile = new File("test/fileToAppend.txt");
		if (testFile.exists()) {
			testFile.delete();
		}
		adapterUsageWithAppendAndAppendNewLineTrue.send(new GenericMessage<>("Initial File Content:"));
		adapterUsageWithAppendAndAppendNewLineTrue.send(new GenericMessage<>("String content:"));
		adapterUsageWithAppendAndAppendNewLineTrue.send(new GenericMessage<>("byte[] content:".getBytes()));
		adapterUsageWithAppendAndAppendNewLineTrue.send(new GenericMessage<>(new File("test/input.txt")));

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertThat(actualFileContent).isEqualTo(expectedFileContent);
		testFile.delete();
	}

	@Test
	public void adapterUsageWithAppendAndAppendNewLineFalse() throws Exception {
		String expectedFileContent = "Initial File Content:String content:byte[] content:File content";

		File testFile = new File("test/fileToAppend.txt");
		if (testFile.exists()) {
			testFile.delete();
		}
		adapterUsageWithAppendAndAppendNewLineFalse.send(new GenericMessage<>("Initial File Content:"));
		adapterUsageWithAppendAndAppendNewLineFalse.send(new GenericMessage<>("String content:"));
		adapterUsageWithAppendAndAppendNewLineFalse.send(new GenericMessage<>("byte[] content:".getBytes()));
		adapterUsageWithAppendAndAppendNewLineFalse.send(new GenericMessage<>(new File("test/input.txt")));

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertThat(actualFileContent).isEqualTo(expectedFileContent);
		testFile.delete();
	}

	@Test
	public void adapterUsageWithFailMode() {
		File testFile = new File("test/fileToFail.txt");
		if (testFile.exists()) {
			testFile.delete();
		}

		usageChannelWithFailMode.send(new GenericMessage<>("Initial File Content:"));

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> usageChannelWithFailMode.send(new GenericMessage<>("String content:")))
				.withMessageContaining("The destination file already exists at");

		testFile.delete();
	}

	@Test
	public void adapterUsageWithIgnoreMode() throws Exception {
		String expectedFileContent = "Initial File Content:";

		File testFile = new File("test/fileToIgnore.txt");
		if (testFile.exists()) {
			testFile.delete();
		}

		usageChannelWithIgnoreMode.send(new GenericMessage<>("Initial File Content:"));
		usageChannelWithIgnoreMode.send(new GenericMessage<>("String content:"));

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertThat(actualFileContent).isEqualTo(expectedFileContent);
		testFile.delete();

	}

	@Test
	public void adapterUsageWithAppendConcurrent() throws Exception {
		File testFile = new File("test/fileToAppendConcurrent.txt");
		if (testFile.exists()) {
			testFile.delete();
		}

		StringBuffer aBuffer = new StringBuffer();
		StringBuffer bBuffer = new StringBuffer();
		for (int i = 0; i < 100000; i++) {
			aBuffer.append("a");
			bBuffer.append("b");
		}
		String aString = aBuffer.toString();
		String bString = bBuffer.toString();

		for (int i = 0; i < 1; i++) {
			usageChannelConcurrent.send(new GenericMessage<>(aString));
			usageChannelConcurrent.send(new GenericMessage<>(bString));
		}

		assertThat(this.fileWriteLatch.await(10, TimeUnit.SECONDS)).isTrue();

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		int beginningIndex = 0;
		for (int i = 0; i < 2; i++) {
			assertAllCharactersAreSame(actualFileContent.substring(beginningIndex, beginningIndex + 99999));
			beginningIndex += 100000;
		}

	}

	private static void assertAllCharactersAreSame(String substring) {
		char[] characters = substring.toCharArray();
		char c = characters[0];
		for (char character : characters) {
			assertThat(character).isEqualTo(c);
		}
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

}
