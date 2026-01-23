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

package org.springframework.integration.mail.inbound;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Flags;
import jakarta.mail.Flags.Flag;
import jakarta.mail.Folder;
import jakarta.mail.FolderClosedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Store;
import jakarta.mail.URLName;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.FromTerm;
import jakarta.mail.search.SearchTerm;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.protocol.IMAPProtocol;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.RetryingTest;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.log.LogAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mail.support.DefaultMailHeaderMapper;
import org.springframework.integration.test.condition.LogLevels;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Alexander Pinske
 * @author Dominik Simmen
 * @author Filip Hrisafov
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@ContextConfiguration(
		"classpath:org/springframework/integration/mail/config/ImapIdleChannelAdapterParserTests-context.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@LogLevels(level = "debug",
		categories = {
				"org.springframework.integration.mail",
				"com.icegreen.greenmail",
				"jakarta.mail"
		})
public class ImapMailReceiverTests implements TestApplicationContextAware {

	private static final ImapSearchLoggingHandler imapSearches = new ImapSearchLoggingHandler();

	private GreenMail imapIdleServer;

	private GreenMailUser user;

	@Autowired
	private ApplicationContext context;

	static {
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
	}

	@BeforeAll
	static void setup() {
		Logger logger = LogManager.getLogManager().getLogger("");
		logger.setLevel(Level.ALL);
		logger.addHandler(imapSearches);
	}

	@AfterAll
	static void teardown() {
		LogManager.getLogManager().getLogger("").removeHandler(imapSearches);
	}

	@BeforeEach
	void startImapServer() {
		imapSearches.searches.clear();
		imapSearches.stores.clear();
		ServerSetup imap = ServerSetupTest.IMAP.verbose(true).dynamicPort();
		imap.setServerStartupTimeout(10000);
		imap.setReadTimeout(10000);
		imapIdleServer = new GreenMail(imap);
		user = imapIdleServer.setUser("user", "pw");
		imapIdleServer.start();
	}

	@AfterEach
	void stopImapServer() {
		imapIdleServer.stop();
	}

	@RetryingTest(10)
	public void testIdleWithServerCustomSearch() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + imapIdleServer.getImap().getPort() + "/INBOX");
		receiver.setSearchTermStrategy((supportedFlags, folder) -> {
			try {
				FromTerm fromTerm = new FromTerm(new InternetAddress("bar@baz"));
				return new AndTerm(fromTerm, new FlagTerm(new Flags(Flag.SEEN), false));
			}
			catch (AddressException e) {
				throw new RuntimeException(e);
			}
		});
		testIdleWithServerGuts(receiver, false);
	}

	@Test
	public void testIdleWithServerDefaultSearch() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + imapIdleServer.getImap().getPort() + "/INBOX");
		testIdleWithServerGuts(receiver, false);
		assertThat(imapSearches.searches.get(0)).contains("testSIUserFlag");
	}

	@Test
	public void testIdleWithMessageMapping() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + imapIdleServer.getImap().getPort() + "/INBOX");
		receiver.setHeaderMapper(new DefaultMailHeaderMapper());
		testIdleWithServerGuts(receiver, true);
	}

	@Test
	@Disabled
	public void testIdleWithServerDefaultSearchSimple() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + imapIdleServer.getImap().getPort() + "/INBOX");
		receiver.setSimpleContent(true);
		testIdleWithServerGuts(receiver, false, true);
		assertThat(imapSearches.searches.get(0)).contains("testSIUserFlag");
	}

	@Test
	public void testIdleWithMessageMappingSimple() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + imapIdleServer.getImap().getPort() + "/INBOX");
		receiver.setSimpleContent(true);
		receiver.setHeaderMapper(new DefaultMailHeaderMapper());
		testIdleWithServerGuts(receiver, true, true);
	}

	public void testIdleWithServerGuts(ImapMailReceiver receiver, boolean mapped) throws Exception {
		testIdleWithServerGuts(receiver, mapped, false);
	}

	public void testIdleWithServerGuts(ImapMailReceiver receiver, boolean mapped, boolean simple) throws Exception {
		receiver.setMaxFetchSize(1);
		receiver.setShouldDeleteMessages(false);
		receiver.setShouldMarkMessagesAsRead(true);
		receiver.setUserFlag("testSIUserFlag");
		receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		receiver.afterPropertiesSet();
		ImapIdleChannelAdapter adapter = new ImapIdleChannelAdapter(receiver);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		adapter.setReconnectDelay(10);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();
		MimeMessage message =
				GreenMailUtil.createTextEmail("Foo <foo@bar>", "Bar <bar@baz>", "Test Email", "foo\r\n",
						imapIdleServer.getImap().getServerSetup());
		message.setRecipients(Message.RecipientType.CC, "a@b, c@d");
		message.setRecipients(Message.RecipientType.BCC, "e@f, g@h");
		user.deliver(message);
		if (!mapped) {
			@SuppressWarnings("unchecked")
			org.springframework.messaging.Message<MimeMessage> received =
					(org.springframework.messaging.Message<MimeMessage>) channel.receive(20000);
			assertThat(received).isNotNull();
			assertThat(received.getPayload().getReceivedDate()).isNotNull();
			assertThat(received.getPayload().getLineCount() > -1).isTrue();
			if (simple) {
				assertThat(received.getPayload().getContent())
						.isEqualTo("foo\r\n");
			}
			else {
				assertThat(received.getPayload().getContent())
						.isEqualTo("foo");
			}
		}
		else {
			org.springframework.messaging.Message<?> received = channel.receive(20000);
			assertThat(received).isNotNull();
			MessageHeaders headers = received.getHeaders();
			assertThat(headers.get(MailHeaders.RAW_HEADERS)).isNotNull();
			assertThat(headers.get(MailHeaders.CONTENT_TYPE)).isEqualTo("text/plain; charset=us-ascii");
			assertThat(headers.get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeTypeUtils.TEXT_PLAIN_VALUE);
			assertThat(headers.get(MailHeaders.FROM)).isEqualTo("Bar <bar@baz>");
			String[] toHeader = headers.get(MailHeaders.TO, String[].class);
			assertThat(toHeader).isNotEmpty();
			assertThat(toHeader[0]).isEqualTo("Foo <foo@bar>");
			assertThat(Arrays.toString(headers.get(MailHeaders.CC, String[].class))).isEqualTo("[a@b, c@d]");
			assertThat(Arrays.toString(headers.get(MailHeaders.BCC, String[].class))).isEqualTo("[e@f, g@h]");
			assertThat(headers.get(MailHeaders.SUBJECT)).isEqualTo("Test Email");
			if (simple) {
				assertThat(received.getPayload()).isEqualTo("foo\r\n");
			}
			else {
				assertThat(received.getPayload()).isEqualTo("foo");
			}
		}
		user.deliver(GreenMailUtil.createTextEmail("Foo <foo@bar>", "Bar <bar@baz>", "subject", "body\r\n",
				imapIdleServer.getImap().getServerSetup()));
		assertThat(channel.receive(30000)).isNotNull(); // new message after idle
		assertThat(channel.receive(100)).isNull(); // no new message after second and third idle

		adapter.stop();
		assertThat(imapSearches.stores.get(0)).contains("testSIUserFlag");
	}

	@Test
	public void receiveAndMarkAsReadDontDelete() throws Exception {
		user.deliver(GreenMailUtil.createTextEmail("user", "sender", "subject", "body",
				imapIdleServer.getImap().getServerSetup()));
		AbstractMailReceiver receiver = new ImapMailReceiver();
		Message msg1 = GreenMailUtil.newMimeMessage("test1");
		Message msg2 = GreenMailUtil.newMimeMessage("test2");
		receiver = receiveAndMarkAsReadDontDeleteGuts(receiver, msg1, msg2);
		assertThat(msg1.getFlags().contains(Flag.SEEN)).isTrue();
		assertThat(msg2.getFlags().contains(Flag.SEEN)).isTrue();
		verify(receiver, times(0)).deleteMessages(Mockito.any());
	}

	private AbstractMailReceiver receiveAndMarkAsReadDontDeleteGuts(AbstractMailReceiver receiver, Message msg1,
			Message msg2) throws NoSuchFieldException, IllegalAccessException, MessagingException {

		return receiveAndMarkAsReadDontDeleteGuts(receiver, msg1, msg2, true);
	}

	private AbstractMailReceiver receiveAndMarkAsReadDontDeleteGuts(AbstractMailReceiver receiver, Message msg1,
			Message msg2, boolean receive) throws NoSuchFieldException, IllegalAccessException, MessagingException {

		((ImapMailReceiver) receiver).setShouldMarkMessagesAsRead(true);
		receiver = spy(receiver);
		receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		receiver.afterPropertiesSet();
		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		given(folder.isOpen()).willReturn(true);
		folderField.set(receiver, folder);

		final Message[] messages = new Message[] {msg1, msg2};

		willAnswer(invocation -> {
			DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
			int folderOpenMode = (int) accessor.getPropertyValue("folderOpenMode");
			if (folderOpenMode != Folder.READ_WRITE) {
				throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
			}

			return null;
		}).given(receiver).openFolder();

		willAnswer(invocation -> messages).given(folder).search(any(SearchTerm.class));

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);
		if (receive) {
			receiver.receive();
		}
		return receiver;
	}

	@Test
	public void receiveAndMarkAsReadDontDeletePassingFilter() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		Message msg1 = GreenMailUtil.newMimeMessage("test1");
		Message msg2 = GreenMailUtil.newMimeMessage("test2");
		Expression selectorExpression = new SpelExpressionParser().parseExpression("true");
		receiver.setSelectorExpression(selectorExpression);
		receiver = receiveAndMarkAsReadDontDeleteGuts(receiver, msg1, msg2);
		assertThat(msg1.getFlags().contains(Flag.SEEN)).isTrue();
		assertThat(msg2.getFlags().contains(Flag.SEEN)).isTrue();
		verify(receiver, times(0)).deleteMessages(Mockito.any());
	}

	@Test
	public void receiveAndMarkAsReadDontDeleteFiltered() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		Message msg1 = GreenMailUtil.newMimeMessage("test1");
		Message msg2 = spy(GreenMailUtil.newMimeMessage("test2"));
		given(msg2.getSubject()).willReturn("foo"); // should not be marked seen
		Expression selectorExpression = new SpelExpressionParser()
				.parseExpression("subject == null OR !subject.equals('foo')");
		receiver.setSelectorExpression(selectorExpression);
		receiver = receiveAndMarkAsReadDontDeleteGuts(receiver, msg1, msg2);
		assertThat(msg1.getFlags().contains(Flag.SEEN)).isTrue();
		assertThat(msg2.getFlags().contains(Flag.SEEN)).isFalse();
		verify(receiver, times(0)).deleteMessages(Mockito.any());
	}

	@Test
	public void receiveAndDebugIsDisabledNotLogFiltered() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();

		LogAccessor logger = spy(TestUtils.<LogAccessor>getPropertyValue(receiver, "logger"));
		new DirectFieldAccessor(receiver).setPropertyValue("logger", logger);
		when(logger.isDebugEnabled()).thenReturn(false);

		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		Expression selectorExpression = new SpelExpressionParser().parseExpression("false");
		receiver.setSelectorExpression(selectorExpression);
		receiveAndMarkAsReadDontDeleteGuts(receiver, msg1, msg2);
		verify(msg1).isExpunged();
		verify(msg2).isExpunged();
		verify(msg1).getSubject();
		verify(msg2).getSubject();
		verify(logger, never()).debug(Mockito.startsWith("Expunged message received"));
		verify(logger, never()).debug(org.mockito.ArgumentMatchers.contains("will be discarded by the matching filter"));
	}

	@Test
	public void receiveExpungedAndNotExpungedLogFiltered() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();

		LogAccessor logger = spy(TestUtils.<LogAccessor>getPropertyValue(receiver, "logger"));
		new DirectFieldAccessor(receiver).setPropertyValue("logger", logger);
		when(logger.isDebugEnabled()).thenReturn(true);

		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		given(msg1.isExpunged()).willReturn(true);
		given(msg1.getSubject()).willReturn("msg1");
		given(msg2.getSubject()).willReturn("msg2");
		Expression selectorExpression = new SpelExpressionParser().parseExpression("false");
		receiver.setSelectorExpression(selectorExpression);
		receiveAndMarkAsReadDontDeleteGuts(receiver, msg1, msg2);
		verify(msg1).isExpunged();
		verify(msg2).isExpunged();
		verify(msg1, never()).getSubject();
		verify(msg2).getSubject();
		verify(logger).debug(Mockito.startsWith("Expunged message discarded"));
	}

	@Test
	public void receiveMarkAsReadAndDelete() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		((ImapMailReceiver) receiver).setShouldMarkMessagesAsRead(true);
		receiver.setShouldDeleteMessages(true);
		receiver = spy(receiver);
		receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		receiver.afterPropertiesSet();

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		given(folder.isOpen()).willReturn(true);
		folderField.set(receiver, folder);

		Message msg1 = GreenMailUtil.newMimeMessage("test1");
		Message msg2 = GreenMailUtil.newMimeMessage("test2");
		final Message[] messages = new Message[] {msg1, msg2};
		willAnswer(invocation -> {
			DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
			int folderOpenMode = (int) accessor.getPropertyValue("folderOpenMode");
			if (folderOpenMode != Folder.READ_WRITE) {
				throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
			}
			return null;
		}).given(receiver).openFolder();

		willAnswer(invocation -> messages).given(folder).search(any(SearchTerm.class));

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);
		receiver.receive();

		assertThat(msg1.getFlags().contains(Flag.SEEN)).isTrue();
		assertThat(msg2.getFlags().contains(Flag.SEEN)).isTrue();

		verify(receiver, times(2)).deleteMessages(Mockito.any());
	}

	@Test
	public void receiveAndDontMarkAsRead() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		((ImapMailReceiver) receiver).setShouldMarkMessagesAsRead(false);
		receiver = spy(receiver);
		receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		receiver.afterPropertiesSet();

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		given(folder.isOpen()).willReturn(true);
		folderField.set(receiver, folder);

		Message msg1 = GreenMailUtil.newMimeMessage("test1");
		Message msg2 = GreenMailUtil.newMimeMessage("test2");
		final Message[] messages = new Message[] {msg1, msg2};
		willAnswer(invocation -> null).given(receiver).openFolder();

		willAnswer(invocation -> messages).given(folder).search(any(SearchTerm.class));

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);
		receiver.afterPropertiesSet();
		receiver.receive();
		assertThat(msg1.getFlags().contains(Flag.SEEN)).isFalse();
		assertThat(msg2.getFlags().contains(Flag.SEEN)).isFalse();
	}

	@Test
	public void receiveAndDontMarkAsReadButDelete() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		receiver.setShouldDeleteMessages(true);
		((ImapMailReceiver) receiver).setShouldMarkMessagesAsRead(false);
		receiver = spy(receiver);
		receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		receiver.afterPropertiesSet();

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		given(folder.isOpen()).willReturn(true);
		folderField.set(receiver, folder);

		Message msg1 = GreenMailUtil.newMimeMessage("test1");
		Message msg2 = GreenMailUtil.newMimeMessage("test2");
		final Message[] messages = new Message[] {msg1, msg2};
		willAnswer(invocation -> {
			DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
			int folderOpenMode = (int) accessor.getPropertyValue("folderOpenMode");
			if (folderOpenMode != Folder.READ_WRITE) {
				throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
			}
			return null;
		}).given(receiver).openFolder();

		willAnswer(invocation -> messages).given(folder).search(any(SearchTerm.class));

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);
		receiver.afterPropertiesSet();
		receiver.receive();

		assertThat(msg1.getFlags().contains(Flag.SEEN)).isFalse();
		assertThat(msg2.getFlags().contains(Flag.SEEN)).isFalse();
		assertThat(msg1.getFlags().contains(Flag.DELETED)).isTrue();
		assertThat(msg2.getFlags().contains(Flag.DELETED)).isTrue();
	}

	@Test
	public void receiveAndIgnoreMarkAsReadDontDelete() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		receiver = spy(receiver);
		receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		receiver.afterPropertiesSet();

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		given(folder.isOpen()).willReturn(true);
		folderField.set(receiver, folder);

		Message msg1 = GreenMailUtil.newMimeMessage("test1");
		Message msg2 = GreenMailUtil.newMimeMessage("test2");
		final Message[] messages = new Message[] {msg1, msg2};
		willAnswer(invocation -> {
			DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
			int folderOpenMode = (int) accessor.getPropertyValue("folderOpenMode");
			if (folderOpenMode != Folder.READ_WRITE) {
				throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
			}
			return null;
		}).given(receiver).openFolder();

		willAnswer(invocation -> messages).given(folder).search(any(SearchTerm.class));

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);
		receiver.receive();
		assertThat(msg1.getFlags().contains(Flag.SEEN)).isTrue();
		assertThat(msg2.getFlags().contains(Flag.SEEN)).isTrue();
		verify(receiver, times(0)).deleteMessages(Mockito.any());
	}

	@Test
	public void testMessageHistory() throws Exception {
		ImapIdleChannelAdapter adapter = this.context.getBean("simpleAdapter", ImapIdleChannelAdapter.class);
		adapter.setReconnectDelay(10);

		AbstractMailReceiver receiver = new ImapMailReceiver();
		receiver = spy(receiver);
		receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		receiver.afterPropertiesSet();

		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		adapterAccessor.setPropertyValue("mailReceiver", receiver);

		Message mailMessage = GreenMailUtil.newMimeMessage("test1");
		final Message[] messages = new Message[] {mailMessage};

		IMAPFolder folder = mock(IMAPFolder.class);
		given(folder.isOpen()).willReturn(true);
		given(folder.hasNewMessages()).willReturn(true);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));

		willAnswer(invocation -> {
			DirectFieldAccessor accessor = new DirectFieldAccessor((invocation.getMock()));
			accessor.setPropertyValue("folder", folder);
			return null;
		}).given(receiver).openFolder();

		willAnswer(invocation -> messages).given(folder).search(any(SearchTerm.class));

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);

		PollableChannel channel = this.context.getBean("channel", PollableChannel.class);

		adapter.start();
		org.springframework.messaging.Message<?> replMessage = channel.receive(10000);
		MessageHistory history = MessageHistory.read(replMessage);
		assertThat(history).isNotNull();
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "simpleAdapter", 0);
		assertThat(componentHistoryRecord).isNotNull();
		assertThat(componentHistoryRecord.get("type")).isEqualTo("mail:imap-idle-channel-adapter");
		adapter.stop();
	}

	@Test
	public void testIdleChannelAdapterException() throws Exception {
		ImapIdleChannelAdapter adapter = this.context.getBean("simpleAdapter", ImapIdleChannelAdapter.class);

		//ImapMailReceiver receiver = (ImapMailReceiver) TestUtils.getPropertyValue(adapter, "mailReceiver");

		DirectChannel channel = new DirectChannel();
		channel.subscribe(new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(org.springframework.messaging.Message<?> requestMessage) {
				throw new RuntimeException("Failed");
			}
		});
		adapter.setOutputChannel(channel);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		adapter.setReconnectDelay(10);

		AbstractMailReceiver receiver = new ImapMailReceiver();
		receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		receiver.afterPropertiesSet();

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(IMAPFolder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		given(folder.isOpen()).willReturn(true);
		given(folder.exists()).willReturn(true);
		folderField.set(receiver, folder);

		Field storeField = AbstractMailReceiver.class.getDeclaredField("store");
		storeField.setAccessible(true);
		Store store = mock(Store.class);
		given(store.isConnected()).willReturn(true);
		storeField.set(receiver, store);

		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		adapterAccessor.setPropertyValue("mailReceiver", receiver);

		Message mailMessage = GreenMailUtil.newMimeMessage("test1");
		Message[] messages = new Message[] {mailMessage};

		willAnswer(invocation -> messages).given(folder).search(any(SearchTerm.class));

		adapter.start();
		org.springframework.messaging.Message<?> replMessage = errorChannel.receive(10000);
		assertThat(replMessage).isNotNull();
		assertThat(((Exception) replMessage.getPayload()).getCause().getMessage()).isEqualTo("Failed");
		adapter.stop();
	}

	@SuppressWarnings("resource")
	@Test
	public void testNoInitialIdleDelayWhenRecentNotSupported() throws Exception {
		ImapIdleChannelAdapter adapter = this.context.getBean("simpleAdapter", ImapIdleChannelAdapter.class);

		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		adapter.setReconnectDelay(10);

		ImapMailReceiver receiver = new ImapMailReceiver("imap:foo");

		final IMAPFolder folder = mock(IMAPFolder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		given(folder.isOpen()).willReturn(false).willReturn(true);
		given(folder.exists()).willReturn(true);

		Field storeField = AbstractMailReceiver.class.getDeclaredField("store");
		storeField.setAccessible(true);
		Store store = mock(Store.class);
		given(store.isConnected()).willReturn(true);
		given(store.getFolder(Mockito.any(URLName.class))).willReturn(folder);
		storeField.set(receiver, store);
		receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		receiver.afterPropertiesSet();

		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		adapterAccessor.setPropertyValue("mailReceiver", receiver);

		Message mailMessage = GreenMailUtil.newMimeMessage("test1");
		final Message[] messages = new Message[] {mailMessage};

		final AtomicInteger shouldFindMessagesCounter = new AtomicInteger(2);
		willAnswer(invocation -> {
			/*
			 * Return the message from first invocation of waitForMessages()
			 * and in receive(); then return false in the next call to
			 * waitForMessages() so we enter idle(); counter will be reset
			 * to 1 in the mocked idle().
			 */
			if (shouldFindMessagesCounter.decrementAndGet() >= 0) {
				return messages;
			}
			else {
				return new Message[0];
			}
		}).given(folder).search(any(SearchTerm.class));

		willAnswer(invocation -> {
			Thread.sleep(300);
			shouldFindMessagesCounter.set(1);
			return null;
		}).given(folder).idle(true);

		adapter.start();

		/*
		 * Idle takes 5 seconds; if all is well, we should receive the first message
		 * before then.
		 */
		assertThat(channel.receive(20000)).isNotNull();
		// We should not receive any more until the next idle elapses
		assertThat(channel.receive(100)).isNull();
		assertThat(channel.receive(10000)).isNotNull();
		adapter.stop();
	}

	@Test
	public void testInitialIdleDelayWhenRecentIsSupported() throws Exception {
		ImapIdleChannelAdapter adapter = this.context.getBean("simpleAdapter", ImapIdleChannelAdapter.class);

		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		adapter.setReconnectDelay(100);
		adapter.afterPropertiesSet();

		ImapMailReceiver receiver = new ImapMailReceiver("imap:foo");
		receiver.setCancelIdleInterval(10);
		IMAPFolder folder = mock(IMAPFolder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.RECENT));
		given(folder.isOpen()).willReturn(false).willReturn(true);
		given(folder.exists()).willReturn(true);

		Field storeField = AbstractMailReceiver.class.getDeclaredField("store");
		storeField.setAccessible(true);
		Store store = mock(Store.class);
		given(store.isConnected()).willReturn(true);
		given(store.getFolder(Mockito.any(URLName.class))).willReturn(folder);
		storeField.set(receiver, store);
		receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		receiver.afterPropertiesSet();

		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		adapterAccessor.setPropertyValue("mailReceiver", receiver);

		Message mailMessage = GreenMailUtil.newMimeMessage("test1");
		Message[] messages = new Message[] {mailMessage};

		willAnswer(invocation -> messages).given(folder).search(any(SearchTerm.class));

		CountDownLatch idles = new CountDownLatch(2);
		willAnswer(invocation -> {
			idles.countDown();
			Thread.sleep(500);
			return null;
		}).given(folder).idle(true);

		adapter.start();

		/*
		 * Idle takes 5 seconds; since this server supports RECENT, we should
		 * not receive any early messages.
		 */
		assertThat(channel.receive(100)).isNull();
		assertThat(channel.receive(20000)).isNotNull();
		assertThat(idles.await(10, TimeUnit.SECONDS)).isTrue();
		adapter.stop();
	}

	@Test
	public void testConnectionException() throws Exception {
		ImapMailReceiver mailReceiver = new ImapMailReceiver("imap:foo");
		ImapIdleChannelAdapter adapter = new ImapIdleChannelAdapter(mailReceiver);
		final AtomicReference<Object> theEvent = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		adapter.setApplicationEventPublisher(event -> {
			theEvent.set(event);
			latch.countDown();
		});
		adapter.setReconnectDelay(10);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(theEvent.get().toString())
				.endsWith("cause=java.lang.IllegalStateException: Failure in 'idle' task. Will resubmit.]");

		adapter.stop();
	}

	@Test // see INT-1801
	public void testImapLifecycleForRaceCondition() throws Exception {
		final AtomicInteger failed = new AtomicInteger(0);
		for (int i = 0; i < 100; i++) {
			final ImapMailReceiver receiver = new ImapMailReceiver("imap://foo");
			Store store = mock(Store.class);
			Folder folder = mock(Folder.class);
			given(folder.exists()).willReturn(true);
			given(folder.isOpen()).willReturn(true);
			given(folder.search(Mockito.any())).willReturn(new Message[] {});
			given(store.getFolder(Mockito.any(URLName.class))).willReturn(folder);
			given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));

			DirectFieldAccessor df = new DirectFieldAccessor(receiver);
			df.setPropertyValue("store", store);
			receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
			receiver.afterPropertiesSet();

			new Thread(() -> {
				try {
					receiver.receive();
				}
				catch (jakarta.mail.MessagingException e) {
					if (e.getCause() instanceof NullPointerException) {
						failed.getAndIncrement();
					}
				}

			}).start();

			new Thread(() -> {
				try {
					receiver.destroy();
				}
				catch (Exception ignore) {
					// ignore
				}
			}).start();
		}
		assertThat(failed.get()).isEqualTo(0);
	}

	@Test
	public void testAttachments() throws Exception {
		final ImapMailReceiver receiver = new ImapMailReceiver("imap://foo");
		Folder folder = testAttachmentsGuts(receiver);
		Message[] messages = (Message[]) receiver.receive();
		Object content = messages[0].getContent();
		assertThat(((Multipart) content).getBodyPart(0).getContent().toString().trim()).isEqualTo("bar");
		assertThat(((Multipart) content).getBodyPart(1).getContent().toString().trim()).isEqualTo("foo");

		assertThat(messages[0].getFolder()).isSameAs(folder);
	}

	@Test
	public void testAttachmentsWithMappingMultiAsBytes() throws Exception {
		final ImapMailReceiver receiver = new ImapMailReceiver("imap://foo");
		receiver.setHeaderMapper(new DefaultMailHeaderMapper());
		testAttachmentsGuts(receiver);
		org.springframework.messaging.Message<?>[] messages = (org.springframework.messaging.Message<?>[]) receiver
				.receive();
		org.springframework.messaging.Message<?> received = messages[0];
		Object content = received.getPayload();
		assertThat(content).isInstanceOf(byte[].class);
		assertThat(received.getHeaders().get(MailHeaders.CONTENT_TYPE))
				.isEqualTo("multipart/mixed;\r\n boundary=\"------------040903000701040401040200\"");
		assertThat(received.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo("application/octet-stream");
	}

	@Test
	public void testAttachmentsWithMapping() throws Exception {
		final ImapMailReceiver receiver = new ImapMailReceiver("imap://foo");
		receiver.setHeaderMapper(new DefaultMailHeaderMapper());
		receiver.setEmbeddedPartsAsBytes(false);
		testAttachmentsGuts(receiver);
		org.springframework.messaging.Message<?>[] messages =
				(org.springframework.messaging.Message<?>[]) receiver.receive();
		Object content = messages[0].getPayload();
		assertThat(content).isInstanceOf(Multipart.class);
		assertThat(((Multipart) content).getBodyPart(0).getContent().toString().trim()).isEqualTo("bar");
		assertThat(((Multipart) content).getBodyPart(1).getContent().toString().trim()).isEqualTo("foo");
	}

	private Folder testAttachmentsGuts(final ImapMailReceiver receiver) throws MessagingException, IOException {
		Store store = mock(Store.class);
		Folder folder = mock(Folder.class);
		given(folder.exists()).willReturn(true);
		given(folder.isOpen()).willReturn(true);

		Message message = GreenMailUtil.newMimeMessage(new ClassPathResource("test.mail").getInputStream());
		given(folder.search(Mockito.any())).willReturn(new Message[] {message});
		given(store.getFolder(Mockito.any(URLName.class))).willReturn(folder);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		DirectFieldAccessor df = new DirectFieldAccessor(receiver);
		df.setPropertyValue("store", store);
		receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		receiver.afterPropertiesSet();

		return folder;
	}

	@Test
	public void testNullMessages() throws Exception {
		Message message1 = GreenMailUtil.newMimeMessage("test1");
		Message message2 = GreenMailUtil.newMimeMessage("test2");
		final Message[] messages1 = new Message[] {null, null, message1};
		final Message[] messages2 = new Message[] {message2};
		final SearchTermStrategy searchTermStrategy = mock(SearchTermStrategy.class);
		class TestReceiver extends ImapMailReceiver {

			private boolean firstDone;

			private TestReceiver() {
				setSearchTermStrategy(searchTermStrategy);
			}

			@Override
			protected Folder getFolder() {
				Folder folder = mock(Folder.class);
				given(folder.isOpen()).willReturn(true);
				try {
					given(folder.getMessages())
							.willReturn(!this.firstDone ? messages1 : messages2);
				}
				catch (MessagingException ignored) {
				}
				return folder;
			}

			@Override
			public Message[] receive() throws MessagingException {
				Message[] messages = searchForNewMessages();
				this.firstDone = true;
				return messages;
			}

		}

		ImapMailReceiver receiver = new TestReceiver();
		Message[] received = (Message[]) receiver.receive();
		assertThat(received.length).isEqualTo(1);
		assertThat(received[0]).isSameAs(message1);
		received = (Message[]) receiver.receive();
		assertThat(received.length).isEqualTo(1);
		assertThat(received).isSameAs(messages2);
		assertThat(received[0]).isSameAs(message2);
	}

	@Test
	public void testIdleReconnects() throws Exception {
		ImapMailReceiver receiver = spy(new ImapMailReceiver("imap:foo"));
		receiver.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		receiver.afterPropertiesSet();
		IMAPFolder folder = mock(IMAPFolder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		given(folder.isOpen()).willReturn(false).willReturn(true);
		given(folder.exists()).willReturn(true);
		given(folder.hasNewMessages()).willReturn(true);
		Field storeField = AbstractMailReceiver.class.getDeclaredField("store");
		storeField.setAccessible(true);
		Store store = mock(Store.class);
		given(store.isConnected()).willReturn(false);
		given(store.getFolder(Mockito.any(URLName.class))).willReturn(folder);
		storeField.set(receiver, store);

		ImapIdleChannelAdapter adapter = new ImapIdleChannelAdapter(receiver);
		LogAccessor logger = spy(TestUtils.<LogAccessor>getPropertyValue(adapter, "logger"));
		new DirectFieldAccessor(adapter).setPropertyValue("logger", logger);
		willDoNothing().given(logger).warn(any(Throwable.class), anyString());
		willAnswer(i -> {
			i.callRealMethod();
			throw new FolderClosedException(folder, "test");
		}).given(receiver).waitForNewMessages();
		adapter.setReconnectDelay(10);
		CountDownLatch latch = new CountDownLatch(3);
		adapter.setApplicationEventPublisher(e -> latch.countDown());
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();
		assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
		verify(store, atLeast(3)).connect();

		adapter.stop();
	}

	@Test
	public void receiveAndMarkAsReadDontDeleteWithThrowingWhenCopying() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		MimeMessage msg1 = spy(GreenMailUtil.newMimeMessage("test1"));
		MimeMessage greenMailMsg2 = GreenMailUtil.newMimeMessage("test2");
		TestThrowingMimeMessage msg2 = new TestThrowingMimeMessage(greenMailMsg2);
		receiver = receiveAndMarkAsReadDontDeleteGuts(receiver, msg1, msg2, false);
		assertThatThrownBy(receiver::receive)
				.isInstanceOf(MessagingException.class)
				.hasMessage("IOException while copying message")
				.cause()
				.isInstanceOf(IOException.class)
				.hasMessage("Simulated exception");
		assertThat(msg1.getFlags().contains(Flag.SEEN)).isFalse();
		assertThat(msg2.getFlags().contains(Flag.SEEN)).isFalse();
		verify(msg1, times(0)).setFlags(Mockito.any(), Mockito.anyBoolean());

		receiver.receive();
		assertThat(msg1.getFlags().contains(Flag.SEEN)).isTrue();
		assertThat(msg2.getFlags().contains(Flag.SEEN)).isTrue();
		// msg2 is marked with the user and seen flags
		verify(msg1, times(2)).setFlags(Mockito.any(), Mockito.anyBoolean());
		verify(receiver, times(0)).deleteMessages(Mockito.any());
	}

	private static class ImapSearchLoggingHandler extends Handler {

		private final List<String> searches = new ArrayList<>();

		private final List<String> stores = new ArrayList<>();

		private static final String SEARCH = " SEARCH ";

		private static final String STORE = " STORE ";

		@Override
		public void publish(LogRecord record) {
			if (IMAPProtocol.class.getPackageName().equals(record.getLoggerName())) {
				String message = record.getMessage();
				if (!message.startsWith("*")) {
					if (message.contains(SEARCH) && !message.contains(" OK ")) {
						searches.add(message.substring(message.indexOf(SEARCH) + SEARCH.length()));
					}
					else if (message.contains(STORE) && !message.contains(" OK ")) {
						stores.add(message.substring(message.indexOf(STORE) + STORE.length()));
					}
				}
			}
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() throws SecurityException {
		}

	}

	private static class TestThrowingMimeMessage extends MimeMessage {

		protected final AtomicBoolean throwExceptionBeforeWrite = new AtomicBoolean(true);

		private TestThrowingMimeMessage(MimeMessage source) throws MessagingException {
			super(source);
		}

		@Override
		public void writeTo(OutputStream os) throws IOException, MessagingException {
			if (this.throwExceptionBeforeWrite.getAndSet(false)) {
				throw new IOException("Simulated exception");
			}
			super.writeTo(os);
		}

	}

}
