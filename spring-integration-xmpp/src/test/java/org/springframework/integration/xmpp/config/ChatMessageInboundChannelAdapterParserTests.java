/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.xmpp.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.inbound.ChatMessageListeningEndpoint;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ChatMessageInboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private QueueChannel xmppInbound;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired @Qualifier("autoChannel.adapter")
	private ChatMessageListeningEndpoint autoChannelAdapter;

	@Test
	public void testInboundAdapter(){
		ChatMessageListeningEndpoint adapter = context.getBean("xmppInboundAdapter", ChatMessageListeningEndpoint.class);
		MessageChannel errorChannel = (MessageChannel) TestUtils.getPropertyValue(adapter, "errorChannel");
		assertEquals(context.getBean("errorChannel"), errorChannel);
		assertFalse(adapter.isAutoStartup());
		QueueChannel channel = (QueueChannel) TestUtils.getPropertyValue(adapter, "outputChannel");
		assertEquals("xmppInbound", channel.getComponentName());
		XMPPConnection connection = (XMPPConnection)TestUtils.getPropertyValue(adapter, "xmppConnection");
		assertEquals(connection, context.getBean("testConnection"));
	}
	
	@Test
	public void testInboundAdapterUsageWithHeaderMapper() {
		XMPPConnection xmppConnection = Mockito.mock(XMPPConnection.class);
		ChatManager chatManager = Mockito.mock(ChatManager.class);
		Mockito.when(xmppConnection.getChatManager()).thenReturn(chatManager);
		Chat chat = Mockito.mock(Chat.class);
		Mockito.when(chatManager.getThreadChat(Mockito.any(String.class))).thenReturn(chat);
		
		ChatMessageListeningEndpoint adapter = context.getBean("xmppInboundAdapter", ChatMessageListeningEndpoint.class);
		
		Field xmppConnectionField = ReflectionUtils.findField(ChatMessageListeningEndpoint.class, "xmppConnection");
		xmppConnectionField.setAccessible(true);
		ReflectionUtils.setField(xmppConnectionField, adapter, xmppConnection);
		
		PacketListener packetListener = TestUtils.getPropertyValue(adapter, "packetListener", PacketListener.class);
		
		Message message = new Message();
		message.setBody("hello");
		message.setTo("oleg");
		message.setProperty("foo", "foo");
		message.setProperty("bar", "bar");
		packetListener.processPacket(message);
		org.springframework.integration.Message<?> siMessage = xmppInbound.receive(0);
		assertEquals("foo", siMessage.getHeaders().get("foo"));
		assertEquals("oleg", siMessage.getHeaders().get("xmpp_to"));
	}

	@Test
	public void testAutoChannel() {
		assertSame(autoChannel, TestUtils.getPropertyValue(autoChannelAdapter, "outputChannel"));
	}
}
