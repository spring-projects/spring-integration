/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.file.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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
	EventDrivenConsumer adapterWithCharset;

	@Autowired
	EventDrivenConsumer adapterWithDirectoryExpression;

	@Autowired
	MessageChannel usageChannel;

	@Autowired
	MessageChannel usageChannelWithFailMode;

	@Autowired
	MessageChannel usageChannelWithIgnoreMode;

	@Autowired
	MessageChannel usageChannelConcurrent;

	private volatile static int adviceCalled;

	@Test
	public void simpleAdapter() {
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(simpleAdapter);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				adapterAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		File expected = new File(System.getProperty("java.io.tmpdir"));

		Expression destinationDirectoryExpression = (Expression)handlerAccessor.getPropertyValue("destinationDirectoryExpression");
		File actual = new File(destinationDirectoryExpression.getExpressionString());
		assertEquals(".foo", TestUtils.getPropertyValue(handler, "temporaryFileSuffix", String.class));
		assertThat(actual, is(expected));
		DefaultFileNameGenerator fileNameGenerator = (DefaultFileNameGenerator) handlerAccessor.getPropertyValue("fileNameGenerator");
		assertNotNull(fileNameGenerator);
		Expression expression = TestUtils.getPropertyValue(fileNameGenerator, "expression", Expression.class);
		assertNotNull(expression);
		assertEquals("'foo.txt'", expression.getExpressionString());
		assertEquals(Boolean.FALSE, handlerAccessor.getPropertyValue("deleteSourceFiles"));
	}

	@Test
	public void adapterWithCustomFileNameGenerator() {
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapterWithCustomNameGenerator);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				adapterAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		File expected = new File(System.getProperty("java.io.tmpdir"));

		Expression destinationDirectoryExpression = (Expression)handlerAccessor.getPropertyValue("destinationDirectoryExpression");
		File actual = new File(destinationDirectoryExpression.getExpressionString());

		assertEquals(expected, actual);
		assertTrue(handlerAccessor.getPropertyValue("fileNameGenerator") instanceof CustomFileNameGenerator);
		assertEquals(".writing", handlerAccessor.getPropertyValue("temporaryFileSuffix"));
	}

	@Test
	public void adapterWithDeleteFlag() {
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapterWithDeleteFlag);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				adapterAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(Boolean.TRUE, handlerAccessor.getPropertyValue("deleteSourceFiles"));
	}

	@Test
	public void adapterWithOrder() {
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapterWithOrder);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				adapterAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(555, handlerAccessor.getPropertyValue("order"));
	}

	@Test
	public void adapterWithAutoStartupFalse() {
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapterWithOrder);
		assertEquals(Boolean.FALSE, adapterAccessor.getPropertyValue("autoStartup"));
	}

	@Test
	public void adapterWithCharset() {
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapterWithCharset);
			FileWritingMessageHandler handler = (FileWritingMessageHandler)
				adapterAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(Charset.forName("UTF-8"), handlerAccessor.getPropertyValue("charset"));
	}

	@Test
	public void adapterWithDirectoryExpression() {

		FileWritingMessageHandler handler = TestUtils.getPropertyValue(adapterWithDirectoryExpression, "handler", FileWritingMessageHandler.class);
		Method m = ReflectionUtils.findMethod(FileWritingMessageHandler.class, "getTemporaryFileSuffix");
		ReflectionUtils.makeAccessible(m);
		assertEquals(".writing", ReflectionUtils.invokeMethod(m, handler));
		String expectedExpressionString = "'foo/bar'";
		String actualExpressionString = TestUtils.getPropertyValue(handler, "destinationDirectoryExpression", Expression.class).getExpressionString();
		assertEquals(expectedExpressionString, actualExpressionString);

	}

	@Test
	public void adapterUsageWithAppend() throws Exception{

		String expectedFileContent = "Initial File Content:String content:byte[] content:File content";

		File testFile = new File("test/fileToAppend.txt");
		if (testFile.exists()){
			testFile.delete();
		}
		usageChannel.send(new GenericMessage<String>("Initial File Content:"));
		usageChannel.send(new GenericMessage<String>("String content:"));
		usageChannel.send(new GenericMessage<byte[]>("byte[] content:".getBytes()));
		usageChannel.send(new GenericMessage<File>(new File("test/input.txt")));

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertEquals(expectedFileContent, actualFileContent);
		assertEquals(4, adviceCalled);
		testFile.delete();
	}

	@Test
	public void adapterUsageWithFailMode() throws Exception{

		File testFile = new File("test/fileToFail.txt");
		if (testFile.exists()){
			testFile.delete();
		}

		usageChannelWithFailMode.send(new GenericMessage<String>("Initial File Content:"));

		try {
			usageChannelWithFailMode.send(new GenericMessage<String>("String content:"));
		}
		catch (MessagingException e) {
			assertTrue(e.getMessage().contains("The destination file already exists at"));
			testFile.delete();
			return;
		}

		Assert.fail("Was expecting an Exception to be thrown.");
	}

	@Test
	public void adapterUsageWithIgnoreMode() throws Exception{


		String expectedFileContent = "Initial File Content:";

		File testFile = new File("test/fileToIgnore.txt");
		if (testFile.exists()){
			testFile.delete();
		}

		usageChannelWithIgnoreMode.send(new GenericMessage<String>("Initial File Content:"));
		usageChannelWithIgnoreMode.send(new GenericMessage<String>("String content:"));

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertEquals(expectedFileContent, actualFileContent);
		testFile.delete();

	}

	@Test
	public void adapterUsageWithAppendConcurrent() throws Exception{

		File testFile = new File("test/fileToAppendConcurrent.txt");
		if (testFile.exists()){
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

		for (int i = 0; i < 1; i	++) {
			usageChannelConcurrent.send(new GenericMessage<String>(aString));
			usageChannelConcurrent.send(new GenericMessage<String>(bString));
		}

		Thread.sleep(2000);
		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		int beginningIndex = 0;
		for (int i = 0; i < 2; i++) {
			assertAllCharactersAreSame(actualFileContent.substring(beginningIndex, beginningIndex+99999));
			beginningIndex += 100000;
		}

	}

	private void assertAllCharactersAreSame(String substring){
		char[] characters = substring.toCharArray();
		char c = characters[0];
		for (char character : characters) {
			assertEquals(c, character);
		}
    }

    public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}
	}
}
