/*
 * Copyright 2002-2021 the original author or authors.
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

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 */
public class Pop3MailReceiverTests {

	@Test
	public void receiveAndDelete() throws Exception {
		AbstractMailReceiver receiver = new Pop3MailReceiver();
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
		final Message[] messages = new Message[] { msg1, msg2 };
		doAnswer(invocation -> {
			DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
			int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
			if (folderOpenMode != Folder.READ_WRITE) {
				throw new IllegalArgumentException("Folder had to be open in READ_WRITE mode");
			}
			return null;
		}).when(receiver).openFolder();

		doAnswer(invocation -> messages).when(receiver).searchForNewMessages();

		doAnswer(invocation -> null).when(receiver).fetchMessages(messages);
		receiver.afterPropertiesSet();
		receiver.receive();
		verify(msg1, times(1)).setFlag(Flag.DELETED, true);
		verify(msg2, times(1)).setFlag(Flag.DELETED, true);
	}

	@Test
	public void receiveAndDontDelete() throws Exception {
		AbstractMailReceiver receiver = new Pop3MailReceiver();
		receiver.setShouldDeleteMessages(false);
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
		final Message[] messages = new Message[] { msg1, msg2 };
		doAnswer(invocation -> null).when(receiver).openFolder();

		doAnswer(invocation -> messages).when(receiver).searchForNewMessages();

		doAnswer(invocation -> null).when(receiver).fetchMessages(messages);
		receiver.afterPropertiesSet();
		receiver.receive();
		verify(msg1, times(0)).setFlag(Flag.DELETED, true);
		verify(msg2, times(0)).setFlag(Flag.DELETED, true);
	}

	@Test
	public void receiveAndDontSetDeleteWithUrl() throws Exception {
		AbstractMailReceiver receiver = new Pop3MailReceiver("pop3://some.host");
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
		final Message[] messages = new Message[] { msg1, msg2 };
		doAnswer(invocation -> null).when(receiver).openFolder();

		doAnswer(invocation -> messages).when(receiver).searchForNewMessages();

		doAnswer(invocation -> null).when(receiver).fetchMessages(messages);
		receiver.afterPropertiesSet();
		receiver.receive();
		verify(msg1, times(0)).setFlag(Flag.DELETED, true);
		verify(msg2, times(0)).setFlag(Flag.DELETED, true);
	}

	@Test
	public void receiveAndDontSetDeleteWithoutUrl() throws Exception {
		AbstractMailReceiver receiver = new Pop3MailReceiver();
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
		final Message[] messages = new Message[] { msg1, msg2 };
		doAnswer(invocation -> null).when(receiver).openFolder();

		doAnswer(invocation -> messages).when(receiver).searchForNewMessages();

		doAnswer(invocation -> null).when(receiver).fetchMessages(messages);
		receiver.afterPropertiesSet();
		receiver.receive();
		verify(msg1, times(0)).setFlag(Flag.DELETED, true);
		verify(msg2, times(0)).setFlag(Flag.DELETED, true);
	}

}
