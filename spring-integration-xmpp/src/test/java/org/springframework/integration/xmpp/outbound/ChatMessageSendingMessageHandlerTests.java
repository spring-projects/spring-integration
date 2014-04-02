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

package org.springframework.integration.xmpp.outbound;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.jivesoftware.smack.XMPPConnection;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.integration.xmpp.core.XmppContextUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 */
public class ChatMessageSendingMessageHandlerTests {


	@Test
	public void validateMessagePostAsString() throws Exception{
		XMPPConnection connection = mock(XMPPConnection.class);
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler(connection);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("Test Message").
					setHeader(XmppHeaders.TO, "kermit@frog.com").
					build();
		// first Message new
		handler.handleMessage(message);

		class EqualSmackMessage extends ArgumentMatcher<org.jivesoftware.smack.packet.Message> {
		      @Override
			public boolean matches(Object msg) {
		    	  org.jivesoftware.smack.packet.Message smackMessage = (org.jivesoftware.smack.packet.Message) msg;
		    	  boolean bodyMatches = smackMessage.getBody().equals("Test Message");
		    	  boolean toMatches = smackMessage.getTo().equals("kermit@frog.com");
		          return bodyMatches & toMatches;
		      }
		}

		verify(connection, times(1)).sendPacket(Mockito.argThat(new EqualSmackMessage()));

		// assuming we know thread ID although currently we do not provide this capability
		message = MessageBuilder.withPayload("Hello Kitty").
			setHeader(XmppHeaders.TO, "kermit@frog.com").
			setHeader(XmppHeaders.THREAD, "123").
			build();

		class EqualSmackMessageWithThreadId extends ArgumentMatcher<org.jivesoftware.smack.packet.Message> {
		      @Override
			public boolean matches(Object msg) {
		    	  org.jivesoftware.smack.packet.Message smackMessage = (org.jivesoftware.smack.packet.Message) msg;
		    	  boolean bodyMatches = smackMessage.getBody().equals("Hello Kitty");
		    	  boolean toMatches = smackMessage.getTo().equals("kermit@frog.com");
		    	  boolean threadIdMatches = smackMessage.getThread().equals("123");
		          return bodyMatches & toMatches & threadIdMatches;
		      }
		}
		reset(connection);
		handler.handleMessage(message);

		// in threaded conversation we need to look for existing chat
		verify(connection, times(1)).sendPacket(Mockito.argThat(new EqualSmackMessageWithThreadId()));
	}

	@Test
	public void validateMessagePostAsSmackMessage() throws Exception{
		XMPPConnection connection = mock(XMPPConnection.class);
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler(connection);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		org.jivesoftware.smack.packet.Message smackMessage = new org.jivesoftware.smack.packet.Message("kermit@frog.com");
		smackMessage.setBody("Test Message");


		Message<?> message = MessageBuilder.withPayload(smackMessage).build();
		// first Message new
		handler.handleMessage(message);

		verify(connection, times(1)).sendPacket(smackMessage);

		// assuming we know thread ID although currently we do not provide this capability
		smackMessage = new org.jivesoftware.smack.packet.Message("kermit@frog.com");
		smackMessage.setBody("Hello Kitty");
		smackMessage.setThread("123");
		message = MessageBuilder.withPayload(smackMessage).build();

		reset(connection);
		handler.handleMessage(message);

		// in threaded conversation we need to look for existing chat
		verify(connection, times(1)).sendPacket(smackMessage);
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
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
	}
}
