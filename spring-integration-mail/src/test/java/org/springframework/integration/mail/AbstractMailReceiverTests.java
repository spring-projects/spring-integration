/*
 * Copyright 2002-2010 the original author or authors.
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

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.DirectFieldAccessor;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Oleg Zhurakousky
 *
 */
public class AbstractMailReceiverTests {
	
	@Test
	public void receieveAndMarkAsReadDontDelete() throws Exception{
		AbstractMailReceiver receiver = new ImapMailReceiver();
		((ImapMailReceiver)receiver).setShouldMarkMessagesAsRead(true);
		receiver = spy(receiver);
		receiver.afterPropertiesSet();
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
	public void receieveMarkAsReadAndDelete() throws Exception{
		AbstractMailReceiver receiver = new ImapMailReceiver();
		((ImapMailReceiver)receiver).setShouldMarkMessagesAsRead(true);
		receiver.setShouldDeleteMessages(true);
		receiver = spy(receiver);
		receiver.afterPropertiesSet();
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
	public void receieveAndDontMarkAsRead() throws Exception{
		AbstractMailReceiver receiver = new ImapMailReceiver();
		((ImapMailReceiver)receiver).setShouldMarkMessagesAsRead(false);
		receiver = spy(receiver);
		receiver.afterPropertiesSet();
		Message msg1 = mock(MimeMessage.class);
		Message msg2 = mock(MimeMessage.class);
		final Message[] messages = new Message[]{msg1, msg2};
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				DirectFieldAccessor accessor = new DirectFieldAccessor(invocation.getMock());
				int folderOpenMode = (Integer) accessor.getPropertyValue("folderOpenMode");
				if (folderOpenMode == Folder.READ_WRITE){
					throw new IllegalArgumentException("Folder had to be open in READ_ONLY mode");
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
	}
	@Test
	public void receieveAndIgnoreMarkAsReadDontDelete() throws Exception{
		AbstractMailReceiver receiver = new ImapMailReceiver();
		receiver = spy(receiver);
		receiver.afterPropertiesSet();
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
}
