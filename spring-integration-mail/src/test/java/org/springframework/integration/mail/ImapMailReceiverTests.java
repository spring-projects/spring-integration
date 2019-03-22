/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.mail;

import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromTerm;

import org.apache.commons.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.mail.ImapIdleChannelAdapter.ImapIdleExceptionEvent;
import org.springframework.integration.mail.support.DefaultMailHeaderMapper;
import org.springframework.integration.test.mail.TestMailServer;
import org.springframework.integration.test.mail.TestMailServer.ImapServer;
import org.springframework.integration.test.support.LongRunningIntegrationTest;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.sun.mail.imap.IMAPFolder;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
		"classpath:org/springframework/integration/mail/config/ImapIdleChannelAdapterParserTests-context.xml")
@DirtiesContext
public class ImapMailReceiverTests {

	private final ImapServer imapIdleServer = TestMailServer.imap(0);

	@Rule
	public final LongRunningIntegrationTest longRunningIntegrationTest = new LongRunningIntegrationTest();

	private final AtomicInteger failed = new AtomicInteger(0);


	@Autowired
	private ApplicationContext context;

	@Before
	public void setup() throws InterruptedException {
		int n = 0;
		while (n++ < 100 && (!this.imapIdleServer.isListening())) {
			Thread.sleep(100);
		}
		assertTrue(n < 100);
	}

	@After
	public void tearDown() {
		this.imapIdleServer.stop();
	}

	@Test
	public void testIdleWithServerCustomSearch() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + this.imapIdleServer.getPort() + "/INBOX");
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
				new ImapMailReceiver("imap://user:pw@localhost:" + this.imapIdleServer.getPort() + "/INBOX");
		testIdleWithServerGuts(receiver, false);
		assertTrue(this.imapIdleServer.assertReceived("searchWithUserFlag"));
	}

	@Test
	public void testIdleWithMessageMapping() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + this.imapIdleServer.getPort() + "/INBOX");
		receiver.setHeaderMapper(new DefaultMailHeaderMapper());
		testIdleWithServerGuts(receiver, true);
	}

	@Test
	public void testIdleWithServerDefaultSearchSimple() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + this.imapIdleServer.getPort() + "/INBOX");
		receiver.setSimpleContent(true);
		testIdleWithServerGuts(receiver, false, true);
		assertTrue(this.imapIdleServer.assertReceived("searchWithUserFlag"));
	}

	@Test
	public void testIdleWithMessageMappingSimple() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + this.imapIdleServer.getPort() + "/INBOX");
		receiver.setSimpleContent(true);
		receiver.setHeaderMapper(new DefaultMailHeaderMapper());
		testIdleWithServerGuts(receiver, true, true);
	}

	public void testIdleWithServerGuts(ImapMailReceiver receiver, boolean mapped) throws Exception {
		testIdleWithServerGuts(receiver, mapped, false);
	}

	public void testIdleWithServerGuts(ImapMailReceiver receiver, boolean mapped, boolean simple) throws Exception {
		this.imapIdleServer.resetServer();
		Properties mailProps = new Properties();
		mailProps.put("mail.debug", "true");
		mailProps.put("mail.imap.connectionpool.debug", "true");
		receiver.setJavaMailProperties(mailProps);
		receiver.setMaxFetchSize(1);
		receiver.setShouldDeleteMessages(false);
		receiver.setShouldMarkMessagesAsRead(true);
		receiver.setCancelIdleInterval(1);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		setUpScheduler(receiver, taskScheduler);
		receiver.setUserFlag("testSIUserFlag");
		receiver.afterPropertiesSet();
		Log logger = spy(TestUtils.getPropertyValue(receiver, "logger", Log.class));
		new DirectFieldAccessor(receiver).setPropertyValue("logger", logger);
		ImapIdleChannelAdapter adapter = new ImapIdleChannelAdapter(receiver);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		adapter.setTaskScheduler(taskScheduler);
		adapter.setReconnectDelay(1);
		adapter.start();
		if (!mapped) {
			@SuppressWarnings("unchecked")
			org.springframework.messaging.Message<MimeMessage> received =
					(org.springframework.messaging.Message<MimeMessage>) channel.receive(10000);
			assertNotNull(received);
			assertNotNull(received.getPayload().getReceivedDate());
			assertTrue(received.getPayload().getLineCount() > -1);
			if (simple) {
				assertThat(received.getPayload().getContent(),
						equalTo(TestMailServer.MailServer.MailHandler.BODY + "\r\n"));
			}
			else {
				assertThat(received.getPayload().getContent(),
						equalTo(TestMailServer.MailServer.MailHandler.MESSAGE + "\r\n"));
			}
		}
		else {
			org.springframework.messaging.Message<?> received = channel.receive(10000);
			assertNotNull(received);
			MessageHeaders headers = received.getHeaders();
			assertNotNull(headers.get(MailHeaders.RAW_HEADERS));
			assertThat(headers.get(MailHeaders.CONTENT_TYPE), equalTo("TEXT/PLAIN; charset=ISO-8859-1"));
			assertThat(headers.get(MessageHeaders.CONTENT_TYPE), equalTo("TEXT/PLAIN; charset=ISO-8859-1"));
			assertThat(headers.get(MailHeaders.FROM), equalTo("Bar <bar@baz>"));
			String[] toHeader = headers.get(MailHeaders.TO, String[].class);
			assertThat(toHeader, not(emptyArray()));
			assertThat(toHeader[0], equalTo("Foo <foo@bar>"));
			assertThat(Arrays.toString(headers.get(MailHeaders.CC, String[].class)), equalTo("[a@b, c@d]"));
			assertThat(Arrays.toString(headers.get(MailHeaders.BCC, String[].class)), equalTo("[e@f, g@h]"));
			assertThat(headers.get(MailHeaders.SUBJECT), equalTo("Test Email"));
			if (simple) {
				assertThat(received.getPayload(), equalTo(TestMailServer.MailServer.MailHandler.BODY + "\r\n"));
			}
			else {
				assertThat(received.getPayload(), equalTo(TestMailServer.MailServer.MailHandler.MESSAGE + "\r\n"));
			}
		}
		assertNotNull(channel.receive(20000)); // new message after idle
		assertNull(channel.receive(100)); // no new message after second and third idle
		verify(logger).debug("Canceling IDLE");

		adapter.stop();
		taskScheduler.shutdown();
		assertTrue(this.imapIdleServer.assertReceived("storeUserFlag"));
	}

	@Test
	public void receiveAndMarkAsReadDontDelete() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		receiver = receiveAndMarkAsReadDontDeleteGuts(receiver, msg1, msg2);
		verify(msg1, times(1)).setFlag(Flag.SEEN, true);
		verify(msg2, times(1)).setFlag(Flag.SEEN, true);
		verify(receiver, times(0)).deleteMessages(Mockito.any());
	}

	private AbstractMailReceiver receiveAndMarkAsReadDontDeleteGuts(AbstractMailReceiver receiver, Message msg1,
			Message msg2) throws NoSuchFieldException, IllegalAccessException, MessagingException {
		((ImapMailReceiver) receiver).setShouldMarkMessagesAsRead(true);
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();
		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);

		final Message[] messages = new Message[] { msg1, msg2 };

		willAnswer(invocation -> {
			DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
			int folderOpenMode = (int) accessor.getPropertyValue("folderOpenMode");
			if (folderOpenMode != Folder.READ_WRITE) {
				throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
			}

			return null;
		}).given(receiver).openFolder();

		willAnswer(invocation -> messages).given(receiver).searchForNewMessages();

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);
		receiver.receive();
		return receiver;
	}

	@Test // INT-2991 Flag.SEEN was set twice when a filter is used
	public void receiveAndMarkAsReadDontDeletePassingFilter() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		Expression selectorExpression = new SpelExpressionParser().parseExpression("true");
		receiver.setSelectorExpression(selectorExpression);
		receiver = receiveAndMarkAsReadDontDeleteGuts(receiver, msg1, msg2);
		verify(msg1, times(1)).setFlag(Flag.SEEN, true);
		verify(msg2, times(1)).setFlag(Flag.SEEN, true);
		verify(receiver, times(0)).deleteMessages(Mockito.any());
	}

	@Test // INT-2991 filtered messages were marked SEEN
	public void receiveAndMarkAsReadDontDeleteFiltered() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		given(msg2.getSubject()).willReturn("foo"); // should not be marked seen
		Expression selectorExpression = new SpelExpressionParser()
				.parseExpression("subject == null OR !subject.equals('foo')");
		receiver.setSelectorExpression(selectorExpression);
		receiver = receiveAndMarkAsReadDontDeleteGuts(receiver, msg1, msg2);
		verify(msg1, times(1)).setFlag(Flag.SEEN, true);
		verify(msg2, never()).setFlag(Flag.SEEN, true);
		verify(receiver, times(0)).deleteMessages(Mockito.any());
	}


	@Test
	public void receiveMarkAsReadAndDelete() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		((ImapMailReceiver) receiver).setShouldMarkMessagesAsRead(true);
		receiver.setShouldDeleteMessages(true);
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);

		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		final Message[] messages = new Message[] { msg1, msg2 };
		willAnswer(invocation -> {
			DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
			int folderOpenMode = (int) accessor.getPropertyValue("folderOpenMode");
			if (folderOpenMode != Folder.READ_WRITE) {
				throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
			}
			return null;
		}).given(receiver).openFolder();

		willAnswer(invocation -> messages).given(receiver).searchForNewMessages();

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);
		receiver.receive();
		verify(msg1, times(1)).setFlag(Flag.SEEN, true);
		verify(msg2, times(1)).setFlag(Flag.SEEN, true);
		verify(receiver, times(1)).deleteMessages(Mockito.any());
	}

	@Test
	public void receiveAndDontMarkAsRead() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		((ImapMailReceiver) receiver).setShouldMarkMessagesAsRead(false);
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);


		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		final Message[] messages = new Message[] { msg1, msg2 };
		willAnswer(invocation -> null).given(receiver).openFolder();

		willAnswer(invocation -> messages).given(receiver).searchForNewMessages();

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);
		receiver.afterPropertiesSet();
		receiver.receive();
		verify(msg1, times(0)).setFlag(Flag.SEEN, true);
		verify(msg2, times(0)).setFlag(Flag.SEEN, true);
	}

	@Test
	public void receiveAndDontMarkAsReadButDelete() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		receiver.setShouldDeleteMessages(true);
		((ImapMailReceiver) receiver).setShouldMarkMessagesAsRead(false);
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);

		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		final Message[] messages = new Message[] { msg1, msg2 };
		willAnswer(invocation -> {
			DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
			int folderOpenMode = (int) accessor.getPropertyValue("folderOpenMode");
			if (folderOpenMode != Folder.READ_WRITE) {
				throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
			}
			return null;
		}).given(receiver).openFolder();

		willAnswer(invocation -> messages).given(receiver).searchForNewMessages();

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);
		receiver.afterPropertiesSet();
		receiver.receive();
		verify(msg1, times(0)).setFlag(Flag.SEEN, true);
		verify(msg2, times(0)).setFlag(Flag.SEEN, true);
		verify(msg1, times(1)).setFlag(Flag.DELETED, true);
		verify(msg2, times(1)).setFlag(Flag.DELETED, true);
	}

	@Test
	public void receiveAndIgnoreMarkAsReadDontDelete() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);

		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		final Message[] messages = new Message[] { msg1, msg2 };
		willAnswer(invocation -> {
			DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
			int folderOpenMode = (int) accessor.getPropertyValue("folderOpenMode");
			if (folderOpenMode != Folder.READ_WRITE) {
				throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
			}
			return null;
		}).given(receiver).openFolder();

		willAnswer(invocation -> messages).given(receiver).searchForNewMessages();

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);
		receiver.receive();
		verify(msg1, times(1)).setFlag(Flag.SEEN, true);
		verify(msg2, times(1)).setFlag(Flag.SEEN, true);
		verify(receiver, times(0)).deleteMessages(Mockito.any());
	}

	@Test
	@Ignore
	public void testMessageHistory() throws Exception {
		ImapIdleChannelAdapter adapter = this.context.getBean("simpleAdapter", ImapIdleChannelAdapter.class);
		adapter.setReconnectDelay(1);

		AbstractMailReceiver receiver = new ImapMailReceiver();
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		adapterAccessor.setPropertyValue("mailReceiver", receiver);

		MimeMessage mailMessage = mock(MimeMessage.class);
		Flags flags = mock(Flags.class);
		given(mailMessage.getFlags()).willReturn(flags);
		final Message[] messages = new Message[] { mailMessage };

		willAnswer(invocation -> {
			DirectFieldAccessor accessor = new DirectFieldAccessor((invocation.getMock()));
			IMAPFolder folder = mock(IMAPFolder.class);
			accessor.setPropertyValue("folder", folder);
			given(folder.hasNewMessages()).willReturn(true);
			return null;
		}).given(receiver).openFolder();

		willAnswer(invocation -> messages).given(receiver).searchForNewMessages();

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);

		PollableChannel channel = this.context.getBean("channel", PollableChannel.class);

		adapter.start();
		org.springframework.messaging.Message<?> replMessage = channel.receive(10000);
		MessageHistory history = MessageHistory.read(replMessage);
		assertNotNull(history);
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "simpleAdapter", 0);
		assertNotNull(componentHistoryRecord);
		assertEquals("mail:imap-idle-channel-adapter", componentHistoryRecord.get("type"));
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
		adapter.setReconnectDelay(1);

		AbstractMailReceiver receiver = new ImapMailReceiver();
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(IMAPFolder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);

		willAnswer(invocation -> true).given(folder).isOpen();

		willAnswer(invocation -> null).given(receiver).openFolder();

		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		adapterAccessor.setPropertyValue("mailReceiver", receiver);

		MimeMessage mailMessage = mock(MimeMessage.class);
		Flags flags = mock(Flags.class);
		given(mailMessage.getFlags()).willReturn(flags);
		final Message[] messages = new Message[] { mailMessage };

		willAnswer(invocation -> messages).given(receiver).searchForNewMessages();

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);

		adapter.start();
		org.springframework.messaging.Message<?> replMessage = errorChannel.receive(10000);
		assertNotNull(replMessage);
		assertEquals("Failed", ((Exception) replMessage.getPayload()).getCause().getMessage());
		adapter.stop();
	}

	@SuppressWarnings("resource")
	@Test
	public void testNoInitialIdleDelayWhenRecentNotSupported() throws Exception {
		ImapIdleChannelAdapter adapter = this.context.getBean("simpleAdapter", ImapIdleChannelAdapter.class);

		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		adapter.setReconnectDelay(1);

		ImapMailReceiver receiver = new ImapMailReceiver("imap:foo");
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		final IMAPFolder folder = mock(IMAPFolder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		given(folder.isOpen()).willReturn(false).willReturn(true);
		given(folder.exists()).willReturn(true);

		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		adapterAccessor.setPropertyValue("mailReceiver", receiver);

		Field storeField = AbstractMailReceiver.class.getDeclaredField("store");
		storeField.setAccessible(true);
		Store store = mock(Store.class);
		given(store.isConnected()).willReturn(true);
		given(store.getFolder(Mockito.any(URLName.class))).willReturn(folder);
		storeField.set(receiver, store);

		willAnswer(invocation -> folder).given(receiver).getFolder();

		MimeMessage mailMessage = mock(MimeMessage.class);
		Flags flags = mock(Flags.class);
		given(mailMessage.getFlags()).willReturn(flags);
		final Message[] messages = new Message[] { mailMessage };

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
		}).given(receiver).searchForNewMessages();

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);

		willAnswer(invocation -> {
			Thread.sleep(300);
			shouldFindMessagesCounter.set(1);
			return null;
		}).given(folder).idle();

		adapter.start();

		/*
		 * Idle takes 5 seconds; if all is well, we should receive the first message
		 * before then.
		 */
		assertNotNull(channel.receive(3000));
		// We should not receive any more until the next idle elapses
		assertNull(channel.receive(100));
		assertNotNull(channel.receive(6000));
		adapter.stop();
	}

	@Test
	public void testInitialIdleDelayWhenRecentIsSupported() throws Exception {
		ImapIdleChannelAdapter adapter = this.context.getBean("simpleAdapter", ImapIdleChannelAdapter.class);

		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		adapter.setReconnectDelay(1);

		ImapMailReceiver receiver = new ImapMailReceiver("imap:foo");
		receiver.setCancelIdleInterval(1);
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		final IMAPFolder folder = mock(IMAPFolder.class);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.RECENT));
		given(folder.isOpen()).willReturn(false).willReturn(true);
		given(folder.exists()).willReturn(true);

		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		adapterAccessor.setPropertyValue("mailReceiver", receiver);

		Field storeField = AbstractMailReceiver.class.getDeclaredField("store");
		storeField.setAccessible(true);
		Store store = mock(Store.class);
		given(store.isConnected()).willReturn(true);
		given(store.getFolder(Mockito.any(URLName.class))).willReturn(folder);
		storeField.set(receiver, store);

		willAnswer(invocation -> folder).given(receiver).getFolder();

		MimeMessage mailMessage = mock(MimeMessage.class);
		Flags flags = mock(Flags.class);
		given(mailMessage.getFlags()).willReturn(flags);
		final Message[] messages = new Message[] { mailMessage };

		willAnswer(invocation -> messages).given(receiver).searchForNewMessages();

		willAnswer(invocation -> null).given(receiver).fetchMessages(messages);

		final CountDownLatch idles = new CountDownLatch(2);
		willAnswer(invocation -> {
			idles.countDown();
			Thread.sleep(1000);
			return null;
		}).given(folder).idle();

		adapter.start();

		/*
		 * Idle takes 5 seconds; since this server supports RECENT, we should
		 * not receive any early messages.
		 */
		assertNull(channel.receive(100));
		assertNotNull(channel.receive(5000));
		assertTrue(idles.await(5, TimeUnit.SECONDS));
		adapter.stop();
	}

	@Test
	public void testConnectionException() throws Exception {
		ImapMailReceiver mailReceiver = new ImapMailReceiver("imap:foo");
		ImapIdleChannelAdapter adapter = new ImapIdleChannelAdapter(mailReceiver);
		final AtomicReference<ImapIdleExceptionEvent> theEvent = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);
		adapter.setApplicationEventPublisher(event -> {
			assertNull("only one event expected", theEvent.get());
			theEvent.set((ImapIdleExceptionEvent) event);
			latch.countDown();
		});
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.setReconnectDelay(1);
		adapter.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertThat(theEvent.get().toString(),
				endsWith("cause=java.lang.IllegalStateException: Failure in 'idle' task. Will resubmit.]"));

		adapter.stop();
		taskScheduler.destroy();
	}

	@Test // see INT-1801
	public void testImapLifecycleForRaceCondition() throws Exception {

		for (int i = 0; i < 1000; i++) {
			final ImapMailReceiver receiver = new ImapMailReceiver("imap://foo");
			Store store = mock(Store.class);
			Folder folder = mock(Folder.class);
			given(folder.exists()).willReturn(true);
			given(folder.isOpen()).willReturn(true);
			given(folder.search(Mockito.any())).willReturn(new Message[] { });
			given(store.getFolder(Mockito.any(URLName.class))).willReturn(folder);
			given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));


			DirectFieldAccessor df = new DirectFieldAccessor(receiver);
			df.setPropertyValue("store", store);
			receiver.setBeanFactory(mock(BeanFactory.class));
			receiver.afterPropertiesSet();

			new Thread(() -> {
				try {
					receiver.receive();
				}
				catch (javax.mail.MessagingException e) {
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
		assertEquals(0, failed.get());
	}

	@Test
	public void testAttachments() throws Exception {
		final ImapMailReceiver receiver = new ImapMailReceiver("imap://foo");
		Folder folder = testAttachmentsGuts(receiver);
		Message[] messages = (Message[]) receiver.receive();
		Object content = messages[0].getContent();
		assertEquals("bar", ((Multipart) content).getBodyPart(0).getContent().toString().trim());
		assertEquals("foo", ((Multipart) content).getBodyPart(1).getContent().toString().trim());

		assertSame(folder, messages[0].getFolder());
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
		assertThat(content, instanceOf(byte[].class));
		assertThat(received.getHeaders().get(MailHeaders.CONTENT_TYPE),
				equalTo("multipart/mixed;\r\n boundary=\"------------040903000701040401040200\""));
		assertThat(received.getHeaders().get(MessageHeaders.CONTENT_TYPE),
				equalTo("application/octet-stream"));
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
		assertThat(content, instanceOf(Multipart.class));
		assertEquals("bar", ((Multipart) content).getBodyPart(0).getContent().toString().trim());
		assertEquals("foo", ((Multipart) content).getBodyPart(1).getContent().toString().trim());
	}

	private Folder testAttachmentsGuts(final ImapMailReceiver receiver) throws MessagingException, IOException {
		Store store = mock(Store.class);
		Folder folder = mock(Folder.class);
		given(folder.exists()).willReturn(true);
		given(folder.isOpen()).willReturn(true);

		Message message = new MimeMessage(null, new ClassPathResource("test.mail").getInputStream());
		given(folder.search(Mockito.any())).willReturn(new Message[] { message });
		given(store.getFolder(Mockito.any(URLName.class))).willReturn(folder);
		given(folder.getPermanentFlags()).willReturn(new Flags(Flags.Flag.USER));
		DirectFieldAccessor df = new DirectFieldAccessor(receiver);
		df.setPropertyValue("store", store);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		return folder;
	}

	@Test
	public void testExecShutdown() {
		ImapIdleChannelAdapter adapter = new ImapIdleChannelAdapter(new ImapMailReceiver());
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.setReconnectDelay(1);
		adapter.start();
		ExecutorService exec = TestUtils.getPropertyValue(adapter, "sendingTaskExecutor", ExecutorService.class);
		adapter.stop();
		assertTrue(exec.isShutdown());
		adapter.start();
		exec = TestUtils.getPropertyValue(adapter, "sendingTaskExecutor", ExecutorService.class);
		adapter.stop();
		assertTrue(exec.isShutdown());

		taskScheduler.shutdown();
	}

	@Test
	public void testNullMessages() throws Exception {
		Message message1 = mock(Message.class);
		Message message2 = mock(Message.class);
		final Message[] messages1 = new Message[] { null, null, message1 };
		final Message[] messages2 = new Message[] { message2 };
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
		assertEquals(1, received.length);
		assertSame(message1, received[0]);
		received = (Message[]) receiver.receive();
		assertEquals(1, received.length);
		assertSame(messages2, received);
		assertSame(message2, received[0]);
	}

	@Test
	public void testIdleReconnects() throws Exception {
		ImapMailReceiver receiver = spy(new ImapMailReceiver("imap:foo"));
		receiver.setBeanFactory(mock(BeanFactory.class));
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
		Log logger = spy(TestUtils.getPropertyValue(adapter, "logger", Log.class));
		new DirectFieldAccessor(adapter).setPropertyValue("logger", logger);
		willDoNothing().given(logger).warn(anyString(), any(Throwable.class));
		willAnswer(i -> {
			i.callRealMethod();
			throw new FolderClosedException(folder, "test");
		}).given(receiver).waitForNewMessages();
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.setReconnectDelay(1);
		adapter.afterPropertiesSet();
		final CountDownLatch latch = new CountDownLatch(3);
		adapter.setApplicationEventPublisher(e -> {
			latch.countDown();
		});
		adapter.start();
		assertTrue(latch.await(60, TimeUnit.SECONDS));
		verify(store, atLeast(3)).connect();

		adapter.stop();
		taskScheduler.shutdown();
	}

	private void setUpScheduler(ImapMailReceiver mailReceiver, ThreadPoolTaskScheduler taskScheduler) {
		taskScheduler.setPoolSize(5);
		taskScheduler.initialize();
		BeanFactory bf = mock(BeanFactory.class);
		given(bf.containsBean("taskScheduler")).willReturn(true);
		given(bf.getBean("taskScheduler", TaskScheduler.class)).willReturn(taskScheduler);
		mailReceiver.setBeanFactory(bf);
	}

}
