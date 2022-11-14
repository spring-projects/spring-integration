/*
 * Copyright 2002-2022 the original author or authors.
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

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.Expression;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Tony Falabella
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class FileOutboundGatewayParserTests {

	@ClassRule
	public static final TemporaryFolder tempFolder = new TemporaryFolder();

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

	@Autowired
	EventDrivenConsumer gatewayWithAppendNewLine;

	private static volatile int adviceCalled;

	@Before
	public void setup() {
		tempFolder.delete();
	}

	@Test
	public void checkOrderedGateway() {
		DirectFieldAccessor gatewayAccessor = new DirectFieldAccessor(ordered);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				gatewayAccessor.getPropertyValue("handler");
		assertThat(gatewayAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertThat(handlerAccessor.getPropertyValue("order")).isEqualTo(777);
		assertThat(handlerAccessor.getPropertyValue("requiresReply")).isEqualTo(Boolean.TRUE);
		DefaultFileNameGenerator fileNameGenerator =
				(DefaultFileNameGenerator) handlerAccessor.getPropertyValue("fileNameGenerator");
		assertThat(fileNameGenerator).isNotNull();
		Expression expression = TestUtils.getPropertyValue(fileNameGenerator, "expression", Expression.class);
		assertThat(expression).isNotNull();
		assertThat(expression.getExpressionString()).isEqualTo("'foo.txt'");

		Long sendTimeout = TestUtils.getPropertyValue(handler, "messagingTemplate.sendTimeout", Long.class);
		assertThat(sendTimeout).isEqualTo(Long.valueOf(777));

	}

	@Test
	public void testOutboundGatewayWithDirectoryExpression() {
		FileWritingMessageHandler handler =
				TestUtils.getPropertyValue(gatewayWithDirectoryExpression, "handler", FileWritingMessageHandler.class);
		assertThat(TestUtils.getPropertyValue(handler, "destinationDirectoryExpression", Expression.class)
				.getExpressionString()).isEqualTo("temporaryFolder.root");
		handler.handleMessage(new GenericMessage<>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
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
	public void gatewayWithIgnoreMode() throws Exception {
		final MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setDefaultDestination(this.gatewayWithIgnoreModeChannel);

		final String expectedFileContent = "Initial File Content:";
		final File testFile = new File(tempFolder.getRoot(), "fileToAppend.txt");

		messagingTemplate.sendAndReceive(new GenericMessage<>("Initial File Content:"));

		Message<?> replyMessage = messagingTemplate.sendAndReceive(new GenericMessage<>("String content:"));

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertThat(actualFileContent).isEqualTo(expectedFileContent);

		assertThat(replyMessage.getPayload() instanceof File).isTrue();

		File replyPayload = (File) replyMessage.getPayload();

		assertThat(new String(FileCopyUtils.copyToByteArray(replyPayload))).isEqualTo(expectedFileContent);

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
	public void gatewayWithFailMode() throws Exception {
		final MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setDefaultDestination(this.gatewayWithFailModeChannel);

		String expectedFileContent = "Initial File Content:";

		File testFile = new File(tempFolder.getRoot(), "fileToAppend.txt");

		messagingTemplate.sendAndReceive(new GenericMessage<>("Initial File Content:"));

		final String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertThat(actualFileContent).isEqualTo(expectedFileContent);

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> messagingTemplate.sendAndReceive(new GenericMessage<>("String content:")))
				.withMessageContaining("The destination file already exists at '");
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
	public void gatewayWithFailModeLowercase() throws Exception {
		final MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setDefaultDestination(this.gatewayWithFailModeLowercaseChannel);

		String expectedFileContent = "Initial File Content:";

		File testFile = new File(tempFolder.getRoot(), "fileToAppend.txt");

		messagingTemplate.sendAndReceive(new GenericMessage<>("Initial File Content:"));

		final String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertThat(actualFileContent).isEqualTo(expectedFileContent);

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> messagingTemplate.sendAndReceive(new GenericMessage<>("String content:")))
				.withMessageContaining("The destination file already exists at '");
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
	public void gatewayWithAppendMode() throws Exception {
		final MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setDefaultDestination(this.gatewayWithAppendModeChannel);

		String expectedFileContent = "Initial File Content:String content:";

		File testFile = new File(tempFolder.getRoot(), "fileToAppend.txt");

		messagingTemplate.sendAndReceive(new GenericMessage<>("Initial File Content:"));
		Message<?> m = messagingTemplate.sendAndReceive(new GenericMessage<>("String content:"));

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertThat(actualFileContent).isEqualTo(expectedFileContent);

		assertThat(m.getPayload() instanceof File).isTrue();

		File replyPayload = (File) m.getPayload();
		assertThat(new String(FileCopyUtils.copyToByteArray(replyPayload))).isEqualTo(expectedFileContent);

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
	public void gatewayWithReplaceMode() throws Exception {
		assertThat(TestUtils.getPropertyValue(this.gatewayWithReplaceModeHandler, "requiresReply", Boolean.class))
				.isFalse();

		final MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setDefaultDestination(this.gatewayWithReplaceModeChannel);

		String expectedFileContent = "String content:";

		File testFile = new File(tempFolder.getRoot(), "fileToAppend.txt");

		messagingTemplate.sendAndReceive(new GenericMessage<>("Initial File Content:"));
		Message<?> m = messagingTemplate.sendAndReceive(new GenericMessage<>("String content:"));

		String actualFileContent = new String(FileCopyUtils.copyToByteArray(testFile));
		assertThat(actualFileContent).isEqualTo(expectedFileContent);

		assertThat(m.getPayload() instanceof File).isTrue();

		File replyPayload = (File) m.getPayload();
		assertThat(new String(FileCopyUtils.copyToByteArray(replyPayload))).isEqualTo(expectedFileContent);

	}

	/**
	 * Test that the underlying {@link FileWritingMessageHandler} bean of the File Outbound Gateway
	 * gets it's {@link FileWritingMessageHandler#setAppendNewLine(boolean)} called when XML
	 * config file has <code>append-new-line="true"</code>.
	 */
	@Test
	public void gatewayWithAppendNewLine() {
		assertThat(TestUtils.getPropertyValue(this.gatewayWithAppendNewLine, "handler.appendNewLine"))
				.isEqualTo(Boolean.TRUE);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
