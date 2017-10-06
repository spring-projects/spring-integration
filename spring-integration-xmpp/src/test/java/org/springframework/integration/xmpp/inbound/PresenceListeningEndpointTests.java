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

package org.springframework.integration.xmpp.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.core.XmppContextUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 */
public class PresenceListeningEndpointTests {

	@Test
	@SuppressWarnings("unchecked")
	public void testEndpointLifecycle() {
		XMPPConnection connection = mock(XMPPConnection.class);
		Roster roster = Roster.getInstanceFor(connection);

		Set<RosterListener> rosterSet = TestUtils.getPropertyValue(roster, "rosterListeners", Set.class);

		PresenceListeningEndpoint rosterEndpoint = new PresenceListeningEndpoint(connection);
		rosterEndpoint.setOutputChannel(new QueueChannel());
		rosterEndpoint.setBeanFactory(mock(BeanFactory.class));
		rosterEndpoint.afterPropertiesSet();
		assertEquals(0, rosterSet.size());
		rosterEndpoint.start();
		assertEquals(1, rosterSet.size());
		rosterEndpoint.stop();
		assertEquals(0, rosterSet.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonInitializedFailure() {
		PresenceListeningEndpoint rosterEndpoint = new PresenceListeningEndpoint(mock(XMPPConnection.class));
		rosterEndpoint.start();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRosterPresenceChangeEvent() {
		XMPPConnection connection = mock(XMPPConnection.class);
		PresenceListeningEndpoint rosterEndpoint = new PresenceListeningEndpoint(connection);
		QueueChannel channel = new QueueChannel();
		rosterEndpoint.setOutputChannel(channel);
		rosterEndpoint.setBeanFactory(mock(BeanFactory.class));
		rosterEndpoint.afterPropertiesSet();
		rosterEndpoint.start();
		RosterListener rosterListener = (RosterListener) TestUtils.getPropertyValue(rosterEndpoint, "rosterListener");
		Presence presence = new Presence(Type.available, "Hello", 1, Mode.chat);
		rosterListener.presenceChanged(presence);
		Message<?> message = channel.receive(10);
		assertEquals(presence, message.getPayload());
	}

	@Test
	public void testWithImplicitXmppConnection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, mock(XMPPConnection.class));
		PresenceListeningEndpoint endpoint = new PresenceListeningEndpoint();
		endpoint.setBeanFactory(bf);
		endpoint.setOutputChannel(new QueueChannel());
		endpoint.afterPropertiesSet();
		assertNotNull(TestUtils.getPropertyValue(endpoint, "xmppConnection"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoXmppConnection() {
		PresenceListeningEndpoint handler = new PresenceListeningEndpoint();
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
	}

	@Test
	public void testWithErrorChannel() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		XMPPConnection connection = mock(XMPPConnection.class);
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, connection);

		PresenceListeningEndpoint endpoint = new PresenceListeningEndpoint();

		DirectChannel outChannel = new DirectChannel();
		outChannel.subscribe(message -> {
			throw new RuntimeException("ooops");
		});
		PollableChannel errorChannel = new QueueChannel();
		endpoint.setBeanFactory(bf);
		endpoint.setOutputChannel(outChannel);
		endpoint.setErrorChannel(errorChannel);
		endpoint.afterPropertiesSet();
		RosterListener listener = (RosterListener) TestUtils.getPropertyValue(endpoint, "rosterListener");
		Presence presence = new Presence(Type.available);

		listener.presenceChanged(presence);

		ErrorMessage msg =
				(ErrorMessage) errorChannel.receive();
		assertSame(presence, ((MessagingException) msg.getPayload())
						.getFailedMessage()
						.getPayload());
	}

}
