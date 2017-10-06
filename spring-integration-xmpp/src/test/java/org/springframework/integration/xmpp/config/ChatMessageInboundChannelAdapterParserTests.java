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

package org.springframework.integration.xmpp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.util.Map;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Florian Schmaus
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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
		MessageChannel errorChannel = (MessageChannel) TestUtils.getPropertyValue(adapter, "errorChannel");
		assertEquals(context.getBean("errorChannel"), errorChannel);
		assertFalse(adapter.isAutoStartup());
		QueueChannel channel = (QueueChannel) TestUtils.getPropertyValue(adapter, "outputChannel");
		assertEquals("xmppInbound", channel.getComponentName());
		XMPPConnection connection = (XMPPConnection) TestUtils.getPropertyValue(adapter, "xmppConnection");
		assertSame(connection, context.getBean("testConnection"));
		Object stanzaFilter = context.getBean("stanzaFilter");
		assertSame(stanzaFilter, TestUtils.getPropertyValue(adapter, "stanzaFilter"));
		assertEquals("#root", TestUtils.getPropertyValue(adapter, "payloadExpression.expression"));
		adapter.start();
		Map asyncRecvListeners = TestUtils.getPropertyValue(connection, "asyncRecvListeners", Map.class);
		assertEquals(6, asyncRecvListeners.size());
		Object lastListener = asyncRecvListeners.values().stream().reduce((first, second) -> second).get();
		assertSame(stanzaFilter, TestUtils.getPropertyValue(lastListener, "packetFilter"));
		adapter.stop();
	}

	@Test
	public void testInboundAdapterUsageWithHeaderMapper() throws Exception {
		XMPPConnection xmppConnection = Mockito.mock(XMPPConnection.class);

		ChatMessageListeningEndpoint adapter = context.getBean("xmppInboundAdapter", ChatMessageListeningEndpoint.class);

		Field xmppConnectionField = ReflectionUtils.findField(ChatMessageListeningEndpoint.class, "xmppConnection");
		xmppConnectionField.setAccessible(true);
		ReflectionUtils.setField(xmppConnectionField, adapter, xmppConnection);

		StanzaListener stanzaListener = TestUtils.getPropertyValue(adapter, "stanzaListener", StanzaListener.class);

		Message message = new Message();
		message.setBody("hello");
		message.setTo(JidCreate.from("oleg"));
		JivePropertiesManager.addProperty(message, "foo", "foo");
		JivePropertiesManager.addProperty(message, "bar", "bar");
		stanzaListener.processStanza(message);
		org.springframework.messaging.Message<?> siMessage = xmppInbound.receive(0);
		assertEquals("foo", siMessage.getHeaders().get("foo"));
		assertEquals("oleg", siMessage.getHeaders().get("xmpp_to"));
	}

	@Test
	public void testAutoChannel() {
		assertSame(autoChannel, TestUtils.getPropertyValue(autoChannelAdapter, "outputChannel"));
	}

}
