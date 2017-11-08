/*
 * Copyright 2002-2017 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.gcm.packet.GcmPacketExtension;
import org.jivesoftware.smackx.gcm.provider.GcmExtensionProvider;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.mockito.ArgumentCaptor;
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
 * @author Artem Bilan
 */
public class ChatMessageSendingMessageHandlerTests {


	@Test
	public void testSendMessages() throws Exception {
		XMPPConnection connection = mock(XMPPConnection.class);
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler(connection);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("Test Message").
				setHeader(XmppHeaders.TO, "kermit@frog.com").
				build();
		// first Message new
		handler.handleMessage(message);

		verify(connection, times(1))
				.sendStanza(Mockito.argThat((org.jivesoftware.smack.packet.Message smackMessage) -> {
					boolean bodyMatches = smackMessage.getBody().equals("Test Message");
					boolean toMatches = smackMessage.getTo().equals("kermit@frog.com");
					return bodyMatches & toMatches;
				}));

		// assuming we know thread ID although currently we do not provide this capability
		message = MessageBuilder.withPayload("Hello Kitty").
				setHeader(XmppHeaders.TO, "kermit@frog.com").
				setHeader(XmppHeaders.THREAD, "123").
				build();

		reset(connection);
		handler.handleMessage(message);

		// in threaded conversation we need to look for existing chat
		verify(connection, times(1))
				.sendStanza(Mockito.argThat((org.jivesoftware.smack.packet.Message smackMessage) -> {
					boolean bodyMatches = smackMessage.getBody().equals("Hello Kitty");
					boolean toMatches = smackMessage.getTo().equals("kermit@frog.com");
					boolean threadIdMatches = smackMessage.getThread().equals("123");
					return bodyMatches & toMatches & threadIdMatches;
				}));

		reset(connection);
		final String json = "{\"foo\": \"bar\"}";
		message = MessageBuilder.withPayload(new GcmPacketExtension(json))
				.setHeader(XmppHeaders.TO, "kermit@frog.com")
				.build();
		handler.handleMessage(message);

		verify(connection, times(1))
				.sendStanza(Mockito.argThat((org.jivesoftware.smack.packet.Message smackMessage) -> {
					boolean bodyMatches = smackMessage.getBody() == null;
					boolean toMatches = smackMessage.getTo().equals("kermit@frog.com");
					GcmPacketExtension gcmPacketExtension = GcmPacketExtension.from(smackMessage);
					boolean jsonMatches = gcmPacketExtension != null && gcmPacketExtension.getJson().equals(json);
					return bodyMatches & toMatches & jsonMatches;
				}));
	}

	@Test
	public void validateMessagePostAsSmackMessage() throws Exception {
		XMPPConnection connection = mock(XMPPConnection.class);
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler(connection);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		org.jivesoftware.smack.packet.Message smackMessage =
				new org.jivesoftware.smack.packet.Message(JidCreate.from("kermit@frog.com"));
		smackMessage.setBody("Test Message");


		Message<?> message = MessageBuilder.withPayload(smackMessage).build();
		// first Message new
		handler.handleMessage(message);

		verify(connection, times(1)).sendStanza(smackMessage);

		// assuming we know thread ID although currently we do not provide this capability
		smackMessage = new org.jivesoftware.smack.packet.Message(JidCreate.from("kermit@frog.com"));
		smackMessage.setBody("Hello Kitty");
		smackMessage.setThread("123");
		message = MessageBuilder.withPayload(smackMessage).build();

		reset(connection);
		handler.handleMessage(message);

		// in threaded conversation we need to look for existing chat
		verify(connection, times(1)).sendStanza(smackMessage);
	}

	@Test
	public void testExtensionProvider() throws Exception {
		XMPPConnection connection = mock(XMPPConnection.class);
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler(connection);
		GcmExtensionProvider extensionElementProvider = spy(new GcmExtensionProvider());
		handler.setExtensionProvider(extensionElementProvider);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		final String json = "{\"foo\": \"bar\"}";
		Message<?> message = MessageBuilder.withPayload("  <f foo='foo'>" + json + "</f>  ")
				.setHeader(XmppHeaders.TO, "kermit@frog.com")
				.build();

		handler.handleMessage(message);

		ArgumentCaptor<org.jivesoftware.smack.packet.Message> argumentCaptor =
				ArgumentCaptor.forClass(org.jivesoftware.smack.packet.Message.class);

		verify(connection).sendStanza(argumentCaptor.capture());

		org.jivesoftware.smack.packet.Message smackMessage = argumentCaptor.getValue();

		assertNull(smackMessage.getBody());
		assertEquals("kermit@frog.com", smackMessage.getTo().toString());
		GcmPacketExtension gcmPacketExtension = GcmPacketExtension.from(smackMessage);
		assertNotNull(gcmPacketExtension);
		assertEquals(json, gcmPacketExtension.getJson());

		verify(extensionElementProvider).from(eq(json));
	}


	@Test(expected = MessageHandlingException.class)
	public void validateFailureNoChatToUser() throws Exception {
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler(mock(XMPPConnection.class));
		handler.handleMessage(new GenericMessage<>("hello"));
	}

	@Test(expected = MessageHandlingException.class)
	public void validateMessageWithUnsupportedPayload() throws Exception {
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler(mock(XMPPConnection.class));
		handler.handleMessage(new GenericMessage<>(123));
	}

	@Test
	public void testWithImplicitXmppConnection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, mock(XMPPConnection.class));
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler();
		handler.setBeanFactory(bf);
		handler.afterPropertiesSet();
		assertNotNull(TestUtils.getPropertyValue(handler, "xmppConnection"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoXmppConnection() {
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler();
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
	}

}
