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
import static junit.framework.Assert.assertFalse;

import org.jivesoftware.smack.XMPPConnection;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.inbound.ChatMessageListeningEndpoint;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ChatMessageInboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testInboundAdapter(){
		ChatMessageListeningEndpoint adapter = context.getBean("xmppInboundAdapter", ChatMessageListeningEndpoint.class);
		MessageChannel errorChannel = (MessageChannel) TestUtils.getPropertyValue(adapter, "errorChannel");
		assertEquals(context.getBean("errorChannel"), errorChannel);
		assertFalse(adapter.isAutoStartup());
		DirectChannel channel = (DirectChannel) TestUtils.getPropertyValue(adapter, "outputChannel");
		assertEquals("xmppInbound", channel.getComponentName());
		XMPPConnection connection = (XMPPConnection)TestUtils.getPropertyValue(adapter, "xmppConnection");
		assertEquals(connection, context.getBean("testConnection"));
	}

}
