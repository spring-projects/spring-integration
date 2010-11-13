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

package org.springframework.integration.xmpp.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.XmppHeaders;

/**
 * @author Oleg Zhurakousky
 *
 */
public class XmppMessageOutboundEndpointParserTests {

	@Test
	public void testPollingConsumer(){
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("XmppMessageOutboundEndpointParserTests-context.xml", XmppMessageOutboundEndpointParserTests.class);
		Object pollingConsumer = context.getBean("outboundPollingAdapter");
		QueueChannel channel = (QueueChannel) TestUtils.getPropertyValue(pollingConsumer, "inputChannel");
		assertEquals("outboundPollingChannel", channel.getComponentName());
		assertTrue(pollingConsumer instanceof PollingConsumer);
	}
	
	@Test
	public void testEventConsumerWithNoChannel(){
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("XmppMessageOutboundEndpointParserTests-context.xml", XmppMessageOutboundEndpointParserTests.class);
		Object eventConsumer = context.getBean("outboundNoChannelAdapter");
		assertTrue(eventConsumer instanceof SubscribableChannel);
	}
	
	@Test
	public void testEventConsumer(){
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("XmppMessageOutboundEndpointParserTests-context.xml", XmppMessageOutboundEndpointParserTests.class);
		Object eventConsumer = context.getBean("outboundEventAdapter");
		assertTrue(eventConsumer instanceof EventDrivenConsumer);
	}
	
	@Test
	public void testPollingConsumerUsage() throws Exception{
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("XmppMessageOutboundEndpointParserTests-context.xml", XmppMessageOutboundEndpointParserTests.class);
		Object pollingConsumer = context.getBean("outboundPollingAdapter");
		assertTrue(pollingConsumer instanceof PollingConsumer);
		MessageChannel channel = context.getBean("outboundEventChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader(XmppHeaders.CHAT_TO, "oleg").build();
		
		XMPPConnection connection = context.getBean("testConnection", XMPPConnection.class);
		ChatManager chatManager = mock(ChatManager.class);
		when(connection.getChatManager()).thenReturn(chatManager);
		Chat chat = mock(Chat.class);
		when(chatManager.createChat(Mockito.anyString(), Mockito.any(MessageListener.class))).thenReturn(chat);
		channel.send(message);
		verify(chat, times(1)).sendMessage("hello");
	}
}
