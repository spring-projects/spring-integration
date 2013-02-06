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
package org.springframework.integration.xmpp.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.core.XmppContextUtils;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 */
public class ChatMessageListeningEndpointTests {


	@Test
	/**
	 * Should add/remove PacketListener when endpoint started/stopped
	 */
	public void testLifecycle(){
		final Set<PacketListener> packetListSet = new HashSet<PacketListener>();
		XMPPConnection connection = mock(XMPPConnection.class);
		ChatMessageListeningEndpoint endpoint = new ChatMessageListeningEndpoint(connection);

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				packetListSet.add((PacketListener) invocation.getArguments()[0]);
				return null;
			}
		}).when(connection).addPacketListener(Mockito.any(PacketListener.class), (PacketFilter) Mockito.any());

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				packetListSet.remove(invocation.getArguments()[0]);
				return null;
			}
		}).when(connection).removePacketListener(Mockito.any(PacketListener.class));

		assertEquals(0, packetListSet.size());
		endpoint.setOutputChannel(new QueueChannel());
		endpoint.afterPropertiesSet();
		endpoint.start();
		assertEquals(1, packetListSet.size());
		endpoint.stop();
		assertEquals(0, packetListSet.size());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNonInitializationFailure(){
		ChatMessageListeningEndpoint endpoint = new ChatMessageListeningEndpoint(mock(XMPPConnection.class));
		endpoint.start();
	}

	@Test
	public void testWithImplicitXmppConnection(){
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, mock(XMPPConnection.class));
		ChatMessageListeningEndpoint endpoint = new ChatMessageListeningEndpoint();
		endpoint.setBeanFactory(bf);
		endpoint.setOutputChannel(new QueueChannel());
		endpoint.afterPropertiesSet();
		assertNotNull(TestUtils.getPropertyValue(endpoint,"xmppConnection"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNoXmppConnection(){
		ChatMessageListeningEndpoint endpoint = new ChatMessageListeningEndpoint();
		endpoint.afterPropertiesSet();
	}

	@Test
	public void testWithErrorChannel(){
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XMPPConnection connection = mock(XMPPConnection.class);
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, connection);

		ChatManager cm = mock(ChatManager.class);
		when(connection.getChatManager()).thenReturn(cm);
		Chat chat = mock(Chat.class);
		when(cm.getThreadChat(Mockito.anyString())).thenReturn(chat);

		ChatMessageListeningEndpoint endpoint = new ChatMessageListeningEndpoint();

		DirectChannel outChannel = new DirectChannel();
		outChannel.subscribe(new MessageHandler() {
			public void handleMessage(org.springframework.integration.Message<?> message)
					throws MessagingException {
				throw new RuntimeException("ooops");
			}
		});
		PollableChannel errorChannel = new QueueChannel();
		endpoint.setBeanFactory(bf);
		endpoint.setOutputChannel(outChannel);
		endpoint.setErrorChannel(errorChannel);
		endpoint.afterPropertiesSet();
		PacketListener listener = (PacketListener) TestUtils.getPropertyValue(endpoint, "packetListener");
		Message smackMessage = new Message("kermit@frog.com");
		smackMessage.setBody("hello");
		smackMessage.setThread("1234");
		listener.processPacket(smackMessage);

		ErrorMessage msg =
			(ErrorMessage) errorChannel.receive();
		assertEquals("hello", ((MessagingException)msg.getPayload()).getFailedMessage().getPayload());
	}
}
