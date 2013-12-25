/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.Expression;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileOutboundGatewayParserTests {

	@Autowired
	private EventDrivenConsumer ordered;

	@Autowired
	private EventDrivenConsumer gatewayWithDirectoryExpression;

	@Autowired
	MessageChannel gatewayWithIgnoreModeChannel;

	@Autowired
	MessageChannel gatewayWithFailModeChannel;

	@Autowired
	MessageChannel gatewayWithAppendModeChannel;

	@Autowired
	MessageChannel gatewayWithReplaceModeChannel;

	@Autowired
    @Qualifier("gatewayWithReplaceMode.handler")
	MessageHandler gatewayWithReplaceModeHandler;

	@Autowired
	MessageChannel gatewayWithFailModeLowercaseChannel;

	private volatile static int adviceCalled;

	@Test
	public void checkOrderedGateway() throws Exception {

		DirectFieldAccessor gatewayAccessor = new DirectFieldAccessor(ordered);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				gatewayAccessor.getPropertyValue("handler");
		assertEquals(Boolean.FALSE, gatewayAccessor.getPropertyValue("autoStartup"));
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(777, handlerAccessor.getPropertyValue("order"));
		assertEquals(Boolean.TRUE, handlerAccessor.getPropertyValue("requiresReply"));
		DefaultFileNameGenerator fileNameGenerator = (DefaultFileNameGenerator) handlerAccessor.getPropertyValue("fileNameGenerator");
		assertNotNull(fileNameGenerator);
		Expression expression = TestUtils.getPropertyValue(fileNameGenerator, "expression", Expression.class);
		assertNotNull(expression);
		assertEquals("'foo.txt'", expression.getExpressionString());

		Long sendTimeout = TestUtils.getPropertyValue(handler, "messagingTemplate.sendTimeout", Long.class);
		assertEquals(Long.valueOf(777), sendTimeout);

	}

	@Test
	public void testOutboundGatewayWithDirectoryExpression() throws Exception {
		FileWritingMessageHandler handler = TestUtils.getPropertyValue(gatewayWithDirectoryExpression, "handler", FileWritingMessageHandler.class);
		assertEquals("'build/foo'", TestUtils.getPropertyValue(handler, "destinationDirectoryExpression", Expression.class).getExpressionString());
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	/**
	 * Test uses the Ignore Mode of the File OutboundGateway. When persisting
	 * a payload using the File Outbound Gateway and the mode is set to IGNORE,
	 * then the destination file will be created and written if it does not yet exist,
	 * BUT if it exists it will not be overwritten. Instead the Message Payload will
	 * be silently ignored. The reply message will contain the pre-existing destination
	 * {@link File} as its payload.
	 *
	 */
	@Test
	public void gatewayWithIgnoreMode() throws Exception{

		final MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setDefaultDestination(this.gatewayWithIgnoreModeChannel);

		final String expectedFileContent = "Initial File Content:";
		final File testFile = new File("test/fileToAppend.txt");

		if (testFile.exists()){
			testFile.delete();
		}

		messagingTemplate.sendAndReceive(new GenericMessage<String>("Initial File Content:"));

		Message<?> replyMessage = messagingTemplate.sendAndReceive(new GenericMessage<String>("String content:"));

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertEquals(expectedFileContent, actualFileContent);

		assertTrue(replyMessage.getPayload() instanceof File);

		File replyPayload = (File) replyMessage.getPayload();

		assertEquals(expectedFileContent, new String(FileCopyUtils.copyToByteArray(replyPayload)));

	}

	/**
	 * Test uses the Fail mode of the File Outbound Gateway. When persisting
	 * a payload using the File Outbound Gateway and the mode is set to Fail,
	 * then the destination {@link File} will be created and written if it does
	 * not yet exist. BUT if the destination {@link File} already exists, a
	 * {@link MessageHandlingException} will be thrown.
	 *
	 */
	@Test
	public void gatewayWithFailMode() throws Exception{

		final MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setDefaultDestination(this.gatewayWithFailModeChannel);

		String expectedFileContent = "Initial File Content:";

		File testFile = new File("test/fileToAppend.txt");

		if (testFile.exists()){
			testFile.delete();
		}

		messagingTemplate.sendAndReceive(new GenericMessage<String>("Initial File Content:"));

		final String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertEquals(expectedFileContent, actualFileContent);

		try {

			messagingTemplate.sendAndReceive(new GenericMessage<String>("String content:"));

		} catch (MessageHandlingException e) {
			assertTrue(e.getMessage().startsWith("The destination file already exists at '"));
			return;
		}

		fail("Was expecting a MessageHandlingException to be thrown.");

	}

	/**
	 * Test is exactly the same as {@link #gatewayWithFailMode()}. However, the
	 * mode is provided in lower-case ensuring that the mode can be provided
	 * in an case-insensitive fashion.
	 *
	 * Instead a {@link MessageHandlingException} will be thrown.
	 *
	 */
	@Test
	public void gatewayWithFailModeLowercase() throws Exception{

		final MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setDefaultDestination(this.gatewayWithFailModeLowercaseChannel);

		String expectedFileContent = "Initial File Content:";

		File testFile = new File("test/fileToAppend.txt");

		if (testFile.exists()){
			testFile.delete();
		}

		messagingTemplate.sendAndReceive(new GenericMessage<String>("Initial File Content:"));

		final String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertEquals(expectedFileContent, actualFileContent);

		try {

			messagingTemplate.sendAndReceive(new GenericMessage<String>("String content:"));

		} catch (MessageHandlingException e) {
			assertTrue(e.getMessage().startsWith("The destination file already exists at '"));
			return;
		}

		fail("Was expecting a MessageHandlingException to be thrown.");

	}

	/**
	 * Test uses the Append Mode of the File Outbound Gateway. When persisting
	 * a payload using the File Outbound Gateway and the mode is set to APPEND,
	 * then the destination file will be created and written, if it does not yet
	 * exist. BUT if it exists it will be appended to the existing file.
	 *
	 * The reply message will contain the concatenated destination
	 * {@link File} as its payload.
	 *
	 */
	@Test
	public void gatewayWithAppendMode() throws Exception{

		final MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setDefaultDestination(this.gatewayWithAppendModeChannel);

		String expectedFileContent = "Initial File Content:String content:";

		File testFile = new File("test/fileToAppend.txt");

		if (testFile.exists()){
			testFile.delete();
		}

		messagingTemplate.sendAndReceive(new GenericMessage<String>("Initial File Content:"));
		Message<?> m = messagingTemplate.sendAndReceive(new GenericMessage<String>("String content:"));

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertEquals(expectedFileContent, actualFileContent);

		assertTrue(m.getPayload() instanceof File);

		File replyPayload = (File) m.getPayload();
		assertEquals(expectedFileContent, new String(FileCopyUtils.copyToByteArray(replyPayload)));

	}

	/**
	 * Test uses the Replace Mode of the File OutboundGateway. When persisting
	 * a payload using the File Outbound Gateway and the mode is set to REPLACE,
	 * then the destination file will be created and written if it does not yet exist.
	 * If the destination file exists, it will be replaced.
	 *
	 * The reply message will contain the concatenated destination
	 * {@link File} as its payload.
	 *
	 */
	@Test
	public void gatewayWithReplaceMode() throws Exception{

		assertFalse(TestUtils.getPropertyValue(this.gatewayWithReplaceModeHandler, "requiresReply", Boolean.class));

		final MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setDefaultDestination(this.gatewayWithReplaceModeChannel);

		String expectedFileContent = "String content:";

		File testFile = new File("test/fileToAppend.txt");

		if (testFile.exists()){
			testFile.delete();
		}

		messagingTemplate.sendAndReceive(new GenericMessage<String>("Initial File Content:"));
		Message<?> m = messagingTemplate.sendAndReceive(new GenericMessage<String>("String content:"));

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertEquals(expectedFileContent, actualFileContent);

		assertTrue(m.getPayload() instanceof File);

		File replyPayload = (File) m.getPayload();
		assertEquals(expectedFileContent, new String(FileCopyUtils.copyToByteArray(replyPayload)));

	}

    public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
