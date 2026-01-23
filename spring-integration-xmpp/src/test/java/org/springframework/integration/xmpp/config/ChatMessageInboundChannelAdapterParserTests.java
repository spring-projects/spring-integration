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

package org.springframework.integration.xmpp.config;

import java.lang.reflect.Field;
import java.util.Map;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.inbound.ChatMessageListeningEndpoint;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Florian Schmaus
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ChatMessageInboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private QueueChannel xmppInbound;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired
	@Qualifier("autoChannel.adapter")
	private ChatMessageListeningEndpoint autoChannelAdapter;

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void testInboundAdapter() {
		ChatMessageListeningEndpoint adapter = context.getBean("xmppInboundAdapter", ChatMessageListeningEndpoint.class);
		MessageChannel errorChannel = TestUtils.getPropertyValue(adapter, "errorChannel");
		assertThat(errorChannel).isEqualTo(context.getBean("errorChannel"));
		assertThat(adapter.isAutoStartup()).isFalse();
		QueueChannel channel = TestUtils.getPropertyValue(adapter, "outputChannel");
		assertThat(channel.getComponentName()).isEqualTo("xmppInbound");
		XMPPConnection connection = TestUtils.getPropertyValue(adapter, "xmppConnection");
		assertThat(context.getBean("testConnection")).isSameAs(connection);
		Object stanzaFilter = context.getBean("stanzaFilter");
		assertThat(TestUtils.<Object>getPropertyValue(adapter, "stanzaFilter")).isSameAs(stanzaFilter);
		assertThat(TestUtils.<String>getPropertyValue(adapter, "payloadExpression.expression")).isEqualTo("#root");
		adapter.start();
		Map asyncRecvListeners = TestUtils.getPropertyValue(connection, "asyncRecvListeners");
		assertThat(asyncRecvListeners.size()).isEqualTo(5);
		Object lastListener = asyncRecvListeners.values().stream().reduce((first, second) -> second).get();
		assertThat(TestUtils.<Object>getPropertyValue(lastListener, "packetFilter")).isSameAs(stanzaFilter);
		adapter.stop();
	}

	@Test
	public void testInboundAdapterUsageWithHeaderMapper() throws Exception {
		XMPPConnection xmppConnection = Mockito.mock(XMPPConnection.class);

		ChatMessageListeningEndpoint adapter = context.getBean("xmppInboundAdapter", ChatMessageListeningEndpoint.class);

		Field xmppConnectionField = ReflectionUtils.findField(ChatMessageListeningEndpoint.class, "xmppConnection");
		xmppConnectionField.setAccessible(true);
		ReflectionUtils.setField(xmppConnectionField, adapter, xmppConnection);

		StanzaListener stanzaListener = TestUtils.getPropertyValue(adapter, "stanzaListener");

		MessageBuilder message = StanzaBuilder.buildMessage();
		message.setBody("hello");
		message.to(JidCreate.from("oleg"));
		JivePropertiesManager.addProperty(message, "foo", "foo");
		JivePropertiesManager.addProperty(message, "bar", "bar");
		stanzaListener.processStanza(message.build());
		org.springframework.messaging.Message<?> siMessage = xmppInbound.receive(0);
		assertThat(siMessage.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(siMessage.getHeaders().get("xmpp_to")).isEqualTo("oleg");
	}

	@Test
	public void testAutoChannel() {
		assertThat(TestUtils.<MessageChannel>getPropertyValue(autoChannelAdapter, "outputChannel"))
				.isSameAs(autoChannel);
	}

}
