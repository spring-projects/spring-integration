/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.xmpp.outbound;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smackx.gcm.packet.GcmPacketExtension;
import org.jivesoftware.smackx.gcm.provider.GcmExtensionProvider;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Florian Schmaus
 * @author Glenn Renfro
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

		org.jivesoftware.smack.packet.Message smackMessage = StanzaBuilder.buildMessage()
				.to("kermit@frog.com")
				.setBody("Test Message")
				.build();

		Message<?> message = MessageBuilder.withPayload(smackMessage).build();
		// first Message new
		handler.handleMessage(message);

		verify(connection).isConnected();
		verify(connection).sendStanza(Mockito.argThat((org.jivesoftware.smack.packet.Message m) -> {
			boolean bodyMatches = "Test Message".equals(m.getBody());
			boolean toMatches = m.getTo().toString().equals("kermit@frog.com");
			return bodyMatches && toMatches;
		}));

		// assuming we know thread ID although currently we do not provide this capability
		smackMessage = StanzaBuilder.buildMessage()
				.ofType(org.jivesoftware.smack.packet.Message.Type.normal)
				.to("kermit@frog.com")
				.setBody("Hello Kitty")
				.setThread("123")
				.build();
		message = MessageBuilder.withPayload(smackMessage).build();

		reset(connection);
		handler.handleMessage(message);

		// in threaded conversation we need to look for existing chat
		verify(connection).isConnected();
		verify(connection).sendStanza(Mockito.argThat((org.jivesoftware.smack.packet.Message m) -> {
			boolean bodyMatches = "Hello Kitty".equals(m.getBody());
			boolean toMatches = "kermit@frog.com".equals(m.getTo().toString());
			boolean threadMatches = "123".equals(m.getThread());
			return bodyMatches && toMatches && threadMatches;
		}));
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

		assertThat(smackMessage.getBody()).isNull();
		assertThat(smackMessage.getTo().toString()).isEqualTo("kermit@frog.com");
		GcmPacketExtension gcmPacketExtension = GcmPacketExtension.from(smackMessage);
		assertThat(gcmPacketExtension).isNotNull();
		assertThat(gcmPacketExtension.getJson()).isEqualTo(json);

		verify(extensionElementProvider).from(eq(json));
	}

	@Test
	public void validateFailureNoChatToUser() {
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler(mock(XMPPConnection.class));
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>("hello")));
	}

	@Test
	public void validateMessageWithUnsupportedPayload() {
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler(mock(XMPPConnection.class));
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>(123)));
	}

	@Test
	public void testWithImplicitXmppConnection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, mock(XMPPConnection.class));
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler();
		handler.setBeanFactory(bf);
		handler.afterPropertiesSet();
		assertThat(TestUtils.<Object>getPropertyValue(handler, "xmppConnection")).isNotNull();
	}

	@Test
	public void testNoXmppConnection() {
		ChatMessageSendingMessageHandler handler = new ChatMessageSendingMessageHandler();
		handler.setBeanFactory(mock(BeanFactory.class));
		assertThatIllegalArgumentException()
				.isThrownBy(handler::afterPropertiesSet);
	}

}
