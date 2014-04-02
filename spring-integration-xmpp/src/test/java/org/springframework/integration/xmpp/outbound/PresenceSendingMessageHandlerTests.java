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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.core.XmppContextUtils;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 */
public class PresenceSendingMessageHandlerTests {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testPresencePayload(){
		PresenceSendingMessageHandler handler = new PresenceSendingMessageHandler(mock(XMPPConnection.class));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage(mock(Presence.class)));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test(expected=MessageHandlingException.class)
	public void testWrongPayload(){
		PresenceSendingMessageHandler handler = new PresenceSendingMessageHandler(mock(XMPPConnection.class));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage(new Object()));
	}

	@Test
	public void testWithImplicitXmppConnection(){
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, mock(XMPPConnection.class));
		PresenceSendingMessageHandler handler = new PresenceSendingMessageHandler();
		handler.setBeanFactory(bf);
		handler.afterPropertiesSet();
		assertNotNull(TestUtils.getPropertyValue(handler,"xmppConnection"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNoXmppConnection(){
		PresenceSendingMessageHandler handler = new PresenceSendingMessageHandler();
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
	}
}
