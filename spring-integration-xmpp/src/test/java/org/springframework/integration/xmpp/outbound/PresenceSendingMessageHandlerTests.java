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
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.core.XmppContextUtils;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Florian Schmaus
 * @author Glenn Renfro
 */
public class PresenceSendingMessageHandlerTests {

	@Test
	public void testPresencePayload() {
		PresenceSendingMessageHandler handler = new PresenceSendingMessageHandler(mock(XMPPConnection.class));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.handleMessage(new GenericMessage<>(StanzaBuilder.buildPresence().build()));
	}

	@Test
	public void testWrongPayload() {
		PresenceSendingMessageHandler handler = new PresenceSendingMessageHandler(mock(XMPPConnection.class));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> handler.handleMessage(new GenericMessage<>(new Object())));
	}

	@Test
	public void testWithImplicitXmppConnection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, mock(XMPPConnection.class));
		PresenceSendingMessageHandler handler = new PresenceSendingMessageHandler();
		handler.setBeanFactory(bf);
		handler.afterPropertiesSet();
		assertThat(TestUtils.<Object>getPropertyValue(handler, "xmppConnection")).isNotNull();
	}

	@Test
	public void testNoXmppConnection() {
		PresenceSendingMessageHandler handler = new PresenceSendingMessageHandler();
		handler.setBeanFactory(mock(BeanFactory.class));
		assertThatIllegalArgumentException()
				.isThrownBy(handler::afterPropertiesSet);
	}

}
