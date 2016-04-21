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

package org.springframework.integration.mail;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
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
import javax.mail.search.SearchTerm;

import org.apache.commons.logging.Log;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.mail.ImapIdleChannelAdapter.ImapIdleExceptionEvent;
import org.springframework.integration.mail.PoorMansMailServer.ImapServer;
import org.springframework.integration.mail.config.ImapIdleChannelAdapterParserTests;
import org.springframework.integration.mail.support.DefaultMailHeaderMapper;
import org.springframework.integration.test.support.LongRunningIntegrationTest;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.sun.mail.imap.IMAPFolder;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ImapMailReceiverTests {

	@Rule
	public final LongRunningIntegrationTest longRunningIntegrationTest = new LongRunningIntegrationTest();

	private final AtomicInteger failed = new AtomicInteger(0);

	private final static ImapServer imapIdleServer = PoorMansMailServer.imap(0);


	@BeforeClass
	public static void setup() throws InterruptedException {
		int n = 0;
		while (n++ < 100 && (!imapIdleServer.isListening())) {
			Thread.sleep(100);
		}
		assertTrue(n < 100);
	}

	@AfterClass
	public static void tearDown() {
		imapIdleServer.stop();
	}

	@Test
	public void testIdleWithServerCustomSearch() throws Exception {
		ImapMailReceiver receiver = new ImapMailReceiver("imap://user:pw@localhost:" + imapIdleServer.getPort()
				+ "/INBOX");
		receiver.setSearchTermStrategy(new SearchTermStrategy() {

			@Override
			public SearchTerm generateSearchTerm(Flags supportedFlags, Folder folder) {
				try {
					FromTerm fromTerm = new FromTerm(new InternetAddress("bar@baz"));
					return new AndTerm(fromTerm, new FlagTerm(new Flags(Flag.SEEN), false));
				}
				catch (AddressException e) {
					throw new RuntimeException(e);
				}
			}
		});
		testIdleWithServerGuts(receiver, false);
	}

	@Test
	public void testIdleWithServerDefaultSearch() throws Exception {
		ImapMailReceiver receiver = new ImapMailReceiver("imap://user:pw@localhost:" + imapIdleServer.getPort()
				+ "/INBOX");
		testIdleWithServerGuts(receiver, false);
		assertTrue(imapIdleServer.assertReceived("searchWithUserFlag"));
	}

	@Test
	public void testIdleWithMessageMapping() throws Exception {
		ImapMailReceiver receiver = new ImapMailReceiver("imap://user:pw@localhost:" + imapIdleServer.getPort()
				+ "/INBOX");
		receiver.setHeaderMapper(new DefaultMailHeaderMapper());
		testIdleWithServerGuts(receiver, true);
	}

	public void testIdleWithServerGuts(ImapMailReceiver receiver, boolean mapped) throws MessagingException {
		imapIdleServer.resetServer();
		Properties mailProps = new Properties();
		mailProps.put("mail.debug", "true");
		mailProps.put("mail.imap.connectionpool.debug", "true");
		receiver.setJavaMailProperties(mailProps);
		receiver.setMaxFetchSize(1);
		receiver.setShouldDeleteMessages(false);
		receiver.setShouldMarkMessagesAsRead(true);
		receiver.setCancelIdleInterval(8);
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
		adapter.start();
		if (!mapped) {
			@SuppressWarnings("unchecked")
			org.springframework.messaging.Message<MimeMessage> received =
					(org.springframework.messaging.Message<MimeMessage>) channel.receive(10000);
			assertNotNull(received);
			assertNotNull(received.getPayload().getReceivedDate());
			assertTrue(received.getPayload().getLineCount() > -1);
		}
		else {
			org.springframework.messaging.Message<?> received = channel.receive(10000);
			assertNotNull(received);
			assertNotNull(received.getHeaders().get(MailHeaders.RAW_HEADERS));
			assertThat((String) received.getHeaders().get(MailHeaders.CONTENT_TYPE),
					equalTo("TEXT/PLAIN; charset=ISO-8859-1"));
			assertThat((String) received.getHeaders().get(MessageHeaders.CONTENT_TYPE),
					equalTo("TEXT/PLAIN; charset=ISO-8859-1"));
		}
		assertNotNull(channel.receive(10000)); // new message after idle
		assertNull(channel.receive(10000)); // no new message after second and third idle
		verify(logger).debug("Canceling IDLE");
		taskScheduler.shutdown();
		assertTrue(imapIdleServer.assertReceived("storeUserFlag"));
	}

	@Test
	public void receiveAndMarkAsReadDontDelete() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		receiver = receiveAndMarkAsReadDontDeleteGuts(receiver, msg1, msg2);
		verify(msg1, times(1)).setFlag(Flag.SEEN, true);
		verify(msg2, times(1)).setFlag(Flag.SEEN, true);
		verify(receiver, times(0)).deleteMessages((Message[]) Mockito.any());
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
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);

		final Message[] messages = new Message[]{msg1, msg2};

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
				if (folderOpenMode != Folder.READ_WRITE) {
					throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
				}

				return null;
			}
		}).when(receiver).openFolder();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);
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
		verify(receiver, times(0)).deleteMessages((Message[]) Mockito.any());
	}

	@Test // INT-2991 filtered messages were marked SEEN
	public void receiveAndMarkAsReadDontDeleteFiltered() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		when(msg2.getSubject()).thenReturn("foo"); // should not be marked seen
		Expression selectorExpression = new SpelExpressionParser()
				.parseExpression("subject == null OR !subject.equals('foo')");
		receiver.setSelectorExpression(selectorExpression);
		receiver = receiveAndMarkAsReadDontDeleteGuts(receiver, msg1, msg2);
		verify(msg1, times(1)).setFlag(Flag.SEEN, true);
		verify(msg2, never()).setFlag(Flag.SEEN, true);
		verify(receiver, times(0)).deleteMessages((Message[]) Mockito.any());
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
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);

		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		final Message[] messages = new Message[]{msg1, msg2};
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
				if (folderOpenMode != Folder.READ_WRITE) {
					throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
				}
				return null;
			}
		}).when(receiver).openFolder();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);
		receiver.receive();
		verify(msg1, times(1)).setFlag(Flag.SEEN, true);
		verify(msg2, times(1)).setFlag(Flag.SEEN, true);
		verify(receiver, times(1)).deleteMessages((Message[]) Mockito.any());
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
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);


		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		final Message[] messages = new Message[]{msg1, msg2};
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).openFolder();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);
		receiver.afterPropertiesSet();
		receiver.receive();
		verify(msg1, times(0)).setFlag(Flag.SEEN, true);
		verify(msg2, times(0)).setFlag(Flag.SEEN, true);
	}
	@Test
	public void receiveAndDontMarkAsReadButDelete() throws Exception {
		AbstractMailReceiver receiver = new ImapMailReceiver();
		((ImapMailReceiver) receiver).setShouldDeleteMessages(true);
		((ImapMailReceiver) receiver).setShouldMarkMessagesAsRead(false);
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(Folder.class);
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);

		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		final Message[] messages = new Message[]{msg1, msg2};
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
				if (folderOpenMode != Folder.READ_WRITE) {
					throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
				}
				return null;
			}
		}).when(receiver).openFolder();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);
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
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);

		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		final Message[] messages = new Message[]{msg1, msg2};
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
				if (folderOpenMode != Folder.READ_WRITE) {
					throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
				}
				return null;
			}
		}).when(receiver).openFolder();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);
		receiver.receive();
		verify(msg1, times(1)).setFlag(Flag.SEEN, true);
		verify(msg2, times(1)).setFlag(Flag.SEEN, true);
		verify(receiver, times(0)).deleteMessages((Message[]) Mockito.any());
	}
	@Test
	@Ignore
	public void testMessageHistory() throws Exception {
		ConfigurableApplicationContext context =
			new ClassPathXmlApplicationContext("ImapIdleChannelAdapterParserTests-context.xml", ImapIdleChannelAdapterParserTests.class);
		ImapIdleChannelAdapter adapter = context.getBean("simpleAdapter", ImapIdleChannelAdapter.class);

		AbstractMailReceiver receiver = new ImapMailReceiver();
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		adapterAccessor.setPropertyValue("mailReceiver", receiver);

		MimeMessage mailMessage = mock(MimeMessage.class);
		Flags flags = mock(Flags.class);
		when(mailMessage.getFlags()).thenReturn(flags);
		final Message[] messages = new Message[]{mailMessage};

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				DirectFieldAccessor accessor = new DirectFieldAccessor((invocation.getMock()));
                IMAPFolder folder = mock(IMAPFolder.class);
				accessor.setPropertyValue("folder", folder);
				when(folder.hasNewMessages()).thenReturn(true);
				return null;
			}
		}).when(receiver).openFolder();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);

		PollableChannel channel = context.getBean("channel", PollableChannel.class);

		adapter.start();
		org.springframework.messaging.Message<?> replMessage = channel.receive(10000);
		MessageHistory history = MessageHistory.read(replMessage);
		assertNotNull(history);
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "simpleAdapter", 0);
		assertNotNull(componentHistoryRecord);
		assertEquals("mail:imap-idle-channel-adapter", componentHistoryRecord.get("type"));
		adapter.stop();
		context.close();
	}

	@Test
	public void testIdleChannelAdapterException() throws Exception {
		ConfigurableApplicationContext context =
			new ClassPathXmlApplicationContext("ImapIdleChannelAdapterParserTests-context.xml", ImapIdleChannelAdapterParserTests.class);
		ImapIdleChannelAdapter adapter = context.getBean("simpleAdapter", ImapIdleChannelAdapter.class);

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

		AbstractMailReceiver receiver = new ImapMailReceiver();
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		Field folderField = AbstractMailReceiver.class.getDeclaredField("folder");
		folderField.setAccessible(true);
		Folder folder = mock(IMAPFolder.class);
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
		folderField.set(receiver, folder);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return true;
			}
		}).when(folder).isOpen();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).openFolder();

		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		adapterAccessor.setPropertyValue("mailReceiver", receiver);

		MimeMessage mailMessage = mock(MimeMessage.class);
		Flags flags = mock(Flags.class);
		when(mailMessage.getFlags()).thenReturn(flags);
		final Message[] messages = new Message[]{mailMessage};

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);

		adapter.start();
		org.springframework.messaging.Message<?> replMessage = errorChannel.receive(10000);
		assertNotNull(replMessage);
		assertEquals("Failed", ((Exception) replMessage.getPayload()).getCause().getMessage());
		adapter.stop();
		context.close();
	}

	@Test
	public void testNoInitialIdleDelayWhenRecentNotSupported() throws Exception {
		ConfigurableApplicationContext context =
			new ClassPathXmlApplicationContext("ImapIdleChannelAdapterParserTests-context.xml", ImapIdleChannelAdapterParserTests.class);
		ImapIdleChannelAdapter adapter = context.getBean("simpleAdapter", ImapIdleChannelAdapter.class);

		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);

		ImapMailReceiver receiver = new ImapMailReceiver("imap:foo");
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		final IMAPFolder folder = mock(IMAPFolder.class);
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
		when(folder.isOpen()).thenReturn(false).thenReturn(true);
		when(folder.exists()).thenReturn(true);

		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		adapterAccessor.setPropertyValue("mailReceiver", receiver);

		Field storeField = AbstractMailReceiver.class.getDeclaredField("store");
		storeField.setAccessible(true);
		Store store = mock(Store.class);
		when(store.isConnected()).thenReturn(true);
		when(store.getFolder(Mockito.any(URLName.class))).thenReturn(folder);
		storeField.set(receiver, store);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return folder;
			}
		}).when(receiver).getFolder();

		MimeMessage mailMessage = mock(MimeMessage.class);
		Flags flags = mock(Flags.class);
		when(mailMessage.getFlags()).thenReturn(flags);
		final Message[] messages = new Message[]{mailMessage};

		final AtomicInteger shouldFindMessagesCounter = new AtomicInteger(2);
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
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
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(5000);
				shouldFindMessagesCounter.set(1);
				return null;
			}
		}).when(folder).idle();

		adapter.start();

		/*
		 * Idle takes 5 seconds; if all is well, we should receive the first message
		 * before then.
		 */
		assertNotNull(channel.receive(3000));
		// We should not receive any more until the next idle elapses
		assertNull(channel.receive(3000));
		assertNotNull(channel.receive(6000));
		adapter.stop();
		context.close();
	}

	@Test
	public void testInitialIdleDelayWhenRecentIsSupported() throws Exception {
		ConfigurableApplicationContext context =
			new ClassPathXmlApplicationContext("ImapIdleChannelAdapterParserTests-context.xml", ImapIdleChannelAdapterParserTests.class);
		ImapIdleChannelAdapter adapter = context.getBean("simpleAdapter", ImapIdleChannelAdapter.class);

		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);

		ImapMailReceiver receiver = new ImapMailReceiver("imap:foo");
		receiver = spy(receiver);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		final IMAPFolder folder = mock(IMAPFolder.class);
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.RECENT));
		when(folder.isOpen()).thenReturn(false).thenReturn(true);
		when(folder.exists()).thenReturn(true);

		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		adapterAccessor.setPropertyValue("mailReceiver", receiver);

		Field storeField = AbstractMailReceiver.class.getDeclaredField("store");
		storeField.setAccessible(true);
		Store store = mock(Store.class);
		when(store.isConnected()).thenReturn(true);
		when(store.getFolder(Mockito.any(URLName.class))).thenReturn(folder);
		storeField.set(receiver, store);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return folder;
			}
		}).when(receiver).getFolder();

		MimeMessage mailMessage = mock(MimeMessage.class);
		Flags flags = mock(Flags.class);
		when(mailMessage.getFlags()).thenReturn(flags);
		final Message[] messages = new Message[]{mailMessage};

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);

		final CountDownLatch idles = new CountDownLatch(2);
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				idles.countDown();
				Thread.sleep(5000);
				return null;
			}
		}).when(folder).idle();

		adapter.start();

		/*
		 * Idle takes 5 seconds; since this server supports RECENT, we should
		 * not receive any early messages.
		 */
		assertNull(channel.receive(3000));
		assertNotNull(channel.receive(5000));
		assertTrue(idles.await(5, TimeUnit.SECONDS));
		adapter.stop();
		context.close();
	}

	@Test
	public void testConnectionException() throws Exception {
		ImapMailReceiver mailReceiver = new ImapMailReceiver("imap:foo");
		ImapIdleChannelAdapter adapter = new ImapIdleChannelAdapter(mailReceiver);
		final AtomicReference<ImapIdleExceptionEvent> theEvent = new AtomicReference<ImapIdleExceptionEvent>();
		final CountDownLatch latch = new CountDownLatch(1);
		adapter.setApplicationEventPublisher(new ApplicationEventPublisher() {

			@Override
			public void publishEvent(ApplicationEvent event) {
				assertNull("only one event expected", theEvent.get());
				theEvent.set((ImapIdleExceptionEvent) event);
				latch.countDown();
			}

			@Override
			public void publishEvent(Object event) {

			}

		});
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertTrue(theEvent.get().toString().endsWith("cause=java.lang.IllegalStateException: Failure in 'idle' task. Will resubmit.]"));
	}

	@Test // see INT-1801
	public void testImapLifecycleForRaceCondition() throws Exception {

		for (int i = 0; i < 1000; i++) {
			final ImapMailReceiver receiver = new ImapMailReceiver("imap://foo");
			Store store = mock(Store.class);
			Folder folder = mock(Folder.class);
			when(folder.exists()).thenReturn(true);
			when(folder.isOpen()).thenReturn(true);
			when(folder.search((SearchTerm) Mockito.any())).thenReturn(new Message[]{});
			when(store.getFolder(Mockito.any(URLName.class))).thenReturn(folder);
			when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));


			DirectFieldAccessor df = new DirectFieldAccessor(receiver);
			df.setPropertyValue("store", store);
			receiver.setBeanFactory(mock(BeanFactory.class));
			receiver.afterPropertiesSet();

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						receiver.receive();
					}
					catch (javax.mail.MessagingException e) {
						if (e.getCause() instanceof NullPointerException) {
							e.printStackTrace();
							failed.getAndIncrement();
						}
					}

				}
			}).start();

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						receiver.destroy();
					}
					catch (Exception ignore) {
						// ignore
						ignore.printStackTrace();
					}
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
		assertThat((String) received.getHeaders().get(MailHeaders.CONTENT_TYPE),
				equalTo("multipart/mixed;\r\n boundary=\"------------040903000701040401040200\""));
		assertThat((String) received.getHeaders().get(MessageHeaders.CONTENT_TYPE),
				equalTo("application/octet-stream"));
	}

	@Test
	public void testAttachmentsWithMapping() throws Exception {
		final ImapMailReceiver receiver = new ImapMailReceiver("imap://foo");
		receiver.setHeaderMapper(new DefaultMailHeaderMapper());
		receiver.setEmbeddedPartsAsBytes(false);
		testAttachmentsGuts(receiver);
		org.springframework.messaging.Message<?>[] messages = (org.springframework.messaging.Message<?>[]) receiver
				.receive();
		Object content = messages[0].getPayload();
		assertThat(content, instanceOf(Multipart.class));
		assertEquals("bar", ((Multipart) content).getBodyPart(0).getContent().toString().trim());
		assertEquals("foo", ((Multipart) content).getBodyPart(1).getContent().toString().trim());
	}

	private Folder testAttachmentsGuts(final ImapMailReceiver receiver) throws MessagingException, IOException {
		Store store = mock(Store.class);
		Folder folder = mock(Folder.class);
		when(folder.exists()).thenReturn(true);
		when(folder.isOpen()).thenReturn(true);

		Message message = new MimeMessage(null, new ClassPathResource("test.mail").getInputStream());
		when(folder.search((SearchTerm) Mockito.any())).thenReturn(new Message[]{message});
		when(store.getFolder(Mockito.any(URLName.class))).thenReturn(folder);
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
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
		adapter.start();
		ExecutorService exec = TestUtils.getPropertyValue(adapter, "sendingTaskExecutor", ExecutorService.class);
		adapter.stop();
		assertTrue(exec.isShutdown());
		adapter.start();
		exec = TestUtils.getPropertyValue(adapter, "sendingTaskExecutor", ExecutorService.class);
		adapter.stop();
		assertTrue(exec.isShutdown());
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

			TestReceiver() {
				setSearchTermStrategy(searchTermStrategy);
			}

			@Override
			protected Folder getFolder() {
				Folder folder = mock(Folder.class);
				when(folder.isOpen()).thenReturn(true);
				try {
					when(folder.getMessages())
							.thenReturn(!this.firstDone ? messages1 : messages2);
				}
				catch (MessagingException e) {
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

	private void setUpScheduler(ImapMailReceiver mailReceiver, ThreadPoolTaskScheduler taskScheduler) {
		taskScheduler.setPoolSize(5);
		taskScheduler.initialize();
		BeanFactory bf = mock(BeanFactory.class);
		when(bf.containsBean("taskScheduler")).thenReturn(true);
		when(bf.getBean("taskScheduler", TaskScheduler.class)).thenReturn(taskScheduler);
		mailReceiver.setBeanFactory(bf);
	}

}
