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
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.messages.XmppMessageDrivenEndpoint;

/**
 * @author Oleg Zhurakousky
 *
 */
public class InboundXmppEndpointParserTests {

	@Test
	public void testInboundAdapter(){
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("InboundXmppEndpointParserTests-context.xml", this.getClass());
		XmppMessageDrivenEndpoint xmde = context.getBean("xmppInboundAdapter", XmppMessageDrivenEndpoint.class);
		assertFalse(xmde.isAutoStartup());
		DirectChannel channel = (DirectChannel) TestUtils.getPropertyValue(xmde, "requestChannel");
		assertEquals("xmppInbound", channel.getComponentName());
		XMPPConnection connection = (XMPPConnection)TestUtils.getPropertyValue(xmde, "xmppConnection");
		assertEquals(connection, context.getBean("testConnection"));
	}
}
