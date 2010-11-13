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

package org.springframework.integration.xmpp.outbound;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.integration.xmpp.core.XmppContextUtils;
import org.springframework.integration.xmpp.outbound.ChatMessageSendingMessageHandler;

/**
 * @author Oleg Zhurakousky
 *
 */
public class XmppMessageSendingMessageHandlerTests {
	

	@Test
	public void validateMessagePost() throws Exception{
		XMPPConnection connection = mock(XMPPConnection.class);
		ChatManager chantManager = mock(ChatManager.class);
		when(connection.getChatManager()).thenReturn(chantManager);
		Chat chat = mock(Chat.class);
		when(chantManager.createChat(Mockito.any(String.class), Mockito.any(MessageListener.class))).thenReturn(chat);
		
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler(connection);
		handler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("Test Message").
					setHeader(XmppHeaders.CHAT_TO, "kermit@frog.com").
					build();
		// first Message
		handler.handleMessage(message);
		
		verify(chantManager, times(1)).createChat(Mockito.any(String.class), Mockito.any(MessageListener.class));
		verify(chat, times(1)).sendMessage("Test Message");
		
		// assuming we know thread ID although currently we do not provide this capability
		message = MessageBuilder.withPayload("Hello Kitty").
			setHeader(XmppHeaders.CHAT_TO, "kermit@frog.com").
			setHeader(XmppHeaders.CHAT_THREAD_ID, "123").
			build();
		reset(chat, chantManager);
		when(chantManager.getThreadChat("123")).thenReturn(chat);
		
		handler.handleMessage(message);
		// in threaded conversation we need to look for existing chat
		verify(chantManager, times(0)).createChat(Mockito.any(String.class), Mockito.any(MessageListener.class));
		verify(chantManager, times(1)).getThreadChat("123");
		verify(chat, times(1)).sendMessage("Hello Kitty");
	}
	
	@Test(expected=MessageHandlingException.class)
	public void validateFailureNoChatToUser() throws Exception{	
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler(mock(XMPPConnection.class));
		handler.handleMessage(new GenericMessage<String>("hello"));
	}
	
	@Test(expected=MessageHandlingException.class)
	public void validateMessageWithUnsupportedPayload() throws Exception{	
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler(mock(XMPPConnection.class));
		handler.handleMessage(new GenericMessage<Integer>(123));
	}
	@Test
	public void testWithImplicitXmppConnection(){
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, mock(XMPPConnection.class));
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler();
		handler.setBeanFactory(bf);
		handler.afterPropertiesSet();
		assertNotNull(TestUtils.getPropertyValue(handler,"xmppConnection"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNoXmppConnection(){
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler();
		handler.afterPropertiesSet();
	}
}
