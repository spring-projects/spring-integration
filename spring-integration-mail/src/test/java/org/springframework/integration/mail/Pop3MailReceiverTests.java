/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.reflect.Field;

import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.mail.Flags;
import jakarta.mail.Flags.Flag;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
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

		Message msg1 = GreenMailUtil.newMimeMessage("test1");
		Message msg2 = GreenMailUtil.newMimeMessage("test2");
		final Message[] messages = new Message[] {msg1, msg2};
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

		assertThat(msg1.getFlags().contains(Flag.DELETED)).isTrue();
		assertThat(msg2.getFlags().contains(Flag.DELETED)).isTrue();
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

		Message msg1 = GreenMailUtil.newMimeMessage("test1");
		Message msg2 = GreenMailUtil.newMimeMessage("test2");
		final Message[] messages = new Message[] {msg1, msg2};
		doAnswer(invocation -> null).when(receiver).openFolder();

		doAnswer(invocation -> messages).when(receiver).searchForNewMessages();

		doAnswer(invocation -> null).when(receiver).fetchMessages(messages);
		receiver.afterPropertiesSet();
		receiver.receive();

		assertThat(msg1.getFlags().contains(Flag.DELETED)).isFalse();
		assertThat(msg2.getFlags().contains(Flag.DELETED)).isFalse();
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

		Message msg1 = GreenMailUtil.newMimeMessage("test1");
		Message msg2 = GreenMailUtil.newMimeMessage("test2");
		final Message[] messages = new Message[] {msg1, msg2};
		doAnswer(invocation -> null).when(receiver).openFolder();

		doAnswer(invocation -> messages).when(receiver).searchForNewMessages();

		doAnswer(invocation -> null).when(receiver).fetchMessages(messages);
		receiver.afterPropertiesSet();
		receiver.receive();

		assertThat(msg1.getFlags().contains(Flag.DELETED)).isFalse();
		assertThat(msg2.getFlags().contains(Flag.DELETED)).isFalse();
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

		Message msg1 = GreenMailUtil.newMimeMessage("test1");
		Message msg2 = GreenMailUtil.newMimeMessage("test2");
		final Message[] messages = new Message[] {msg1, msg2};
		doAnswer(invocation -> null).when(receiver).openFolder();

		doAnswer(invocation -> messages).when(receiver).searchForNewMessages();

		doAnswer(invocation -> null).when(receiver).fetchMessages(messages);
		receiver.afterPropertiesSet();
		receiver.receive();

		assertThat(msg1.getFlags().contains(Flag.DELETED)).isFalse();
		assertThat(msg2.getFlags().contains(Flag.DELETED)).isFalse();
	}

}
