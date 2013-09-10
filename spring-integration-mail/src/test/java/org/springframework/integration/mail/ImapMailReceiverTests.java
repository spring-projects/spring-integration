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

package org.springframework.integration.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
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
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.mail.ImapIdleChannelAdapter.ImapIdleExceptionEvent;
import org.springframework.integration.mail.config.ImapIdleChannelAdapterParserTests;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.FileCopyUtils;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ImapMailReceiverTests {

	private final AtomicInteger failed = new AtomicInteger(0);

	@Test
	public void receiveAndMarkAsReadDontDelete() throws Exception{
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
		((ImapMailReceiver)receiver).setShouldMarkMessagesAsRead(true);
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
			public Object answer(InvocationOnMock invocation) throws Throwable {
				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
				if (folderOpenMode != Folder.READ_WRITE){
					throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
				}

				return null;
			}
		}).when(receiver).openFolder();

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);
		receiver.receive();
		return receiver;
	}

	@Test // INT-2991 Flag.SEEN was set twice when a filter is used
	public void receiveAndMarkAsReadDontDeletePassingFilter() throws Exception{
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
	public void receiveAndMarkAsReadDontDeleteFiltered() throws Exception{
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
	public void receiveMarkAsReadAndDelete() throws Exception{
		AbstractMailReceiver receiver = new ImapMailReceiver();
		((ImapMailReceiver)receiver).setShouldMarkMessagesAsRead(true);
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
			public Object answer(InvocationOnMock invocation) throws Throwable {
				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
				if (folderOpenMode != Folder.READ_WRITE){
					throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
				}
				return null;
			}
		}).when(receiver).openFolder();

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
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
	public void receiveAndDontMarkAsRead() throws Exception{
		AbstractMailReceiver receiver = new ImapMailReceiver();
		((ImapMailReceiver)receiver).setShouldMarkMessagesAsRead(false);
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
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).openFolder();

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
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
	public void receiveAndDontMarkAsReadButDelete() throws Exception{
		AbstractMailReceiver receiver = new ImapMailReceiver();
		((ImapMailReceiver)receiver).setShouldDeleteMessages(true);
		((ImapMailReceiver)receiver).setShouldMarkMessagesAsRead(false);
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
			public Object answer(InvocationOnMock invocation) throws Throwable {
				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
				if (folderOpenMode != Folder.READ_WRITE){
					throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
				}
				return null;
			}
		}).when(receiver).openFolder();

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
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
	public void receiveAndIgnoreMarkAsReadDontDelete() throws Exception{
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
			public Object answer(InvocationOnMock invocation) throws Throwable {
				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
				if (folderOpenMode != Folder.READ_WRITE){
					throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
				}
				return null;
			}
		}).when(receiver).openFolder();

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
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
	public void testMessageHistory() throws Exception{
		ApplicationContext context =
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
			public Object answer(InvocationOnMock invocation) throws Throwable {
				DirectFieldAccessor accesor = new DirectFieldAccessor((invocation.getMock()));
                IMAPFolder folder = mock(IMAPFolder.class);
				accesor.setPropertyValue("folder", folder);
				when(folder.hasNewMessages()).thenReturn(true);
				return null;
			}
		}).when(receiver).openFolder();

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
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
	}

	@Test
	public void testIdleChannelAdapterException() throws Exception{
		ApplicationContext context =
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
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return true;
			}
		}).when(folder).isOpen();

		doAnswer(new Answer<Object>() {
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
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);

		adapter.start();
		org.springframework.messaging.Message<?> replMessage = errorChannel.receive(10000);
		assertNotNull(replMessage);
		assertEquals("Failed", ((Exception) replMessage.getPayload()).getCause().getMessage());
		adapter.stop();
	}

	@Test
	public void testNoInitialIdleDelayWhenRecentNotSupported() throws Exception{
		ApplicationContext context =
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
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);

		doAnswer(new Answer<Object>() {
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
	}

	@Test
	public void testInitialIdleDelayWhenRecentIsSupported() throws Exception{
		ApplicationContext context =
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
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return folder;
			}
		}).when(receiver).getFolder();

		MimeMessage mailMessage = mock(MimeMessage.class);
		Flags flags = mock(Flags.class);
		when(mailMessage.getFlags()).thenReturn(flags);
		final Message[] messages = new Message[]{mailMessage};

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return messages;
			}
		}).when(receiver).searchForNewMessages();

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).fetchMessages(messages);

		final CountDownLatch idles = new CountDownLatch(2);
		doAnswer(new Answer<Object>() {
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
		});
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertTrue(theEvent.get().toString().endsWith("cause=java.lang.IllegalStateException: Failure in 'idle' task. Will resubmit.]"));
	}

	@Test // see INT-1801
	public void testImapLifecycleForRaceCondition() throws Exception{

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
				public void run(){
					try {
						receiver.receive();
					} catch (javax.mail.MessagingException e) {
						if (e.getCause() instanceof NullPointerException){
							e.printStackTrace();
							failed.getAndIncrement();
						}
					}

				}
			}).start();

			new Thread(new Runnable() {
				public void run(){
					try {
						receiver.destroy();
					} catch (Exception ignore) {
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
		Store store = mock(Store.class);
		Folder folder = mock(Folder.class);
		when(folder.exists()).thenReturn(true);
		when(folder.isOpen()).thenReturn(true);

		IMAPMessage message = mock(IMAPMessage.class);
		when(folder.search((SearchTerm) Mockito.any())).thenReturn(new Message[]{message});
		when(store.getFolder(Mockito.any(URLName.class))).thenReturn(folder);
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));
		DirectFieldAccessor df = new DirectFieldAccessor(receiver);
		df.setPropertyValue("store", store);
		receiver.setBeanFactory(mock(BeanFactory.class));
		receiver.afterPropertiesSet();

		doAnswer(new Answer<Object> () {

			public Object answer(InvocationOnMock invocation) throws Throwable {
				OutputStream os = (OutputStream) invocation.getArguments()[0];
				FileCopyUtils.copy(new ClassPathResource("test.mail").getInputStream(), os);
				return null;
			}
		}).when(message).writeTo(Mockito.any(OutputStream.class));
		Message[] messages = receiver.receive();
		Object content = messages[0].getContent();
		assertEquals("bar", ((Multipart) content).getBodyPart(0).getContent().toString().trim());

		assertSame(folder, messages[0].getFolder());
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

}
