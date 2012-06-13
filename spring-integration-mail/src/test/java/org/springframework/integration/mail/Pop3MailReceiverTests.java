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

package org.springframework.integration.mail;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.mail.MailReceiver.MailReceiverContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 */
public class Pop3MailReceiverTests {
	@Test
	public void receiveAndDelete() throws Exception{
		AbstractMailReceiver receiver = new Pop3MailReceiver();
		((Pop3MailReceiver)receiver).setShouldDeleteMessages(true);
		receiver = spy(receiver);
		receiver.afterPropertiesSet();

		MailReceiverContext context = MailTestsHelper.setupContextHolder(receiver);
		Folder folder = context.getFolder();
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));

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
		receiver.closeContextAfterSuccess(context);
		verify(msg1, times(1)).setFlag(Flag.DELETED, true);
		verify(msg2, times(1)).setFlag(Flag.DELETED, true);
	}
	@Test
	public void receiveAndDontDelete() throws Exception{
		AbstractMailReceiver receiver = new Pop3MailReceiver();
		((Pop3MailReceiver)receiver).setShouldDeleteMessages(false);
		receiver = spy(receiver);
		receiver.afterPropertiesSet();

		MailReceiverContext context = MailTestsHelper.setupContextHolder(receiver);
		Folder folder = context.getFolder();
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));

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
		receiver.closeContextAfterFailure(context);
		verify(msg1, times(0)).setFlag(Flag.DELETED, true);
		verify(msg2, times(0)).setFlag(Flag.DELETED, true);
	}
	@Test
	public void receiveAndDontSetDeleteWithUrl() throws Exception{
		AbstractMailReceiver receiver = new Pop3MailReceiver("pop3://some.host");
		receiver = spy(receiver);
		receiver.afterPropertiesSet();

		MailReceiverContext context = MailTestsHelper.setupContextHolder(receiver);
		Folder folder = context.getFolder();
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));

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
		receiver.closeContextAfterFailure(context);
		verify(msg1, times(0)).setFlag(Flag.DELETED, true);
		verify(msg2, times(0)).setFlag(Flag.DELETED, true);
	}
	@Test
	public void receiveAndDontSetDeleteWithoutUrl() throws Exception{
		AbstractMailReceiver receiver = new Pop3MailReceiver();
		receiver = spy(receiver);
		receiver.afterPropertiesSet();

		MailReceiverContext context = MailTestsHelper.setupContextHolder(receiver);
		Folder folder = context.getFolder();
		when(folder.getPermanentFlags()).thenReturn(new Flags(Flags.Flag.USER));

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
		receiver.closeContextAfterFailure(context);
		verify(msg1, times(0)).setFlag(Flag.DELETED, true);
		verify(msg2, times(0)).setFlag(Flag.DELETED, true);
	}

	@Test
	public void testCommit() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		Pop3MailReceiver receiver = new Pop3MailReceiver("pop3://some.host");
		receiver.setShouldDeleteMessages(true);
		receiver = spy(receiver);
		MailReceiverContext context = MailTestsHelper.setupContextHolder(receiver);
		Folder folder = context.getFolder();
		when(folder.isOpen()).thenReturn(true);
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).openFolder();
		adapter.setSource(new MailReceivingMessageSource(receiver));

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		final AtomicReference<Method> doPollMethod = new AtomicReference<Method>();
		ReflectionUtils.doWithMethods(SourcePollingChannelAdapter.class, new MethodCallback() {

			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				if (method.getName() == "doPoll") {
					doPollMethod.set(method);
					method.setAccessible(true);
				}
			}
		});
		doPollMethod.get().invoke(adapter, (Object[]) null);
		TransactionSynchronizationUtils.triggerAfterCommit();
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		TransactionSynchronizationManager.clearSynchronization();
		verify(folder).close(true);
	}

	@Test
	public void testRollback() throws Exception {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		Pop3MailReceiver receiver = new Pop3MailReceiver("pop3://some.host");
		receiver.setShouldDeleteMessages(true);
		receiver = spy(receiver);
		MailReceiverContext context = MailTestsHelper.setupContextHolder(receiver);
		Folder folder = context.getFolder();
		when(folder.isOpen()).thenReturn(true);
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return null;
			}
		}).when(receiver).openFolder();
		adapter.setSource(new MailReceivingMessageSource(receiver));

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		final AtomicReference<Method> doPollMethod = new AtomicReference<Method>();
		ReflectionUtils.doWithMethods(SourcePollingChannelAdapter.class, new MethodCallback() {

			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				if (method.getName() == "doPoll") {
					doPollMethod.set(method);
					method.setAccessible(true);
				}
			}
		});
		doPollMethod.get().invoke(adapter, (Object[]) null);
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		TransactionSynchronizationManager.clearSynchronization();
		verify(folder).close(false);
	}

}
