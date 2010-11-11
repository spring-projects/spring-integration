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
package org.springframework.integration.xmpp.presence;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.XmppContextUtils;

/**
 * @author Oleg Zhurakousky
 *
 */
public class XmppRosterEventMessageDrivenEndpointTests {

	@Test
	public void testEndpointLifecycle(){
		final Set<RosterListener> rosterSet = new HashSet<RosterListener>();
		XMPPConnection connection = mock(XMPPConnection.class);
		Roster roster = mock(Roster.class);
		when(connection.getRoster()).thenReturn(roster);
		
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				rosterSet.add((RosterListener) invocation.getArguments()[0]);
				return null;
			}
		}).when(roster).addRosterListener(Mockito.any(RosterListener.class));
		
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				rosterSet.remove(invocation.getArguments()[0]);
				return null;
			}
		}).when(roster).removeRosterListener(Mockito.any(RosterListener.class));
		XmppRosterEventMessageDrivenEndpoint rosterEndpoint = new XmppRosterEventMessageDrivenEndpoint(connection);
		rosterEndpoint.afterPropertiesSet();
		assertEquals(0, rosterSet.size());
		rosterEndpoint.start();
		assertEquals(1, rosterSet.size());
		rosterEndpoint.stop();
		assertEquals(0, rosterSet.size());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNonInitializedFailure(){
		XmppRosterEventMessageDrivenEndpoint rosterEndpoint = new XmppRosterEventMessageDrivenEndpoint(mock(XMPPConnection.class));
		rosterEndpoint.start();
	}
	
	@Test
	public void testRosterPresenceChangeEvent(){
		XMPPConnection connection = mock(XMPPConnection.class);
		Roster roster = mock(Roster.class);
		when(connection.getRoster()).thenReturn(roster);
		XmppRosterEventMessageDrivenEndpoint rosterEndpoint = new XmppRosterEventMessageDrivenEndpoint(connection);
		QueueChannel channel = new QueueChannel();
		rosterEndpoint.setRequestChannel(channel);
		rosterEndpoint.afterPropertiesSet();
		rosterEndpoint.start();
		RosterListener rosterListener = (RosterListener) TestUtils.getPropertyValue(rosterEndpoint, "rosterListener");
		Presence presence = new Presence(Type.available, "Hello", 1, Mode.chat);
		rosterListener.presenceChanged(presence);
		Message<?> message = channel.receive(10);
		assertEquals(presence, message.getPayload());
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testRosterEntriesEvents(){
		XMPPConnection connection = mock(XMPPConnection.class);
		Roster roster = mock(Roster.class);
		when(connection.getRoster()).thenReturn(roster);
		XmppRosterEventMessageDrivenEndpoint rosterEndpoint = new XmppRosterEventMessageDrivenEndpoint(connection);
		QueueChannel channel = new QueueChannel();
		rosterEndpoint.setRequestChannel(channel);
		rosterEndpoint.afterPropertiesSet();
		rosterEndpoint.start();
		RosterListener rosterListener = (RosterListener) TestUtils.getPropertyValue(rosterEndpoint, "rosterListener");
		List entries = Arrays.asList(new String[]{"many", "moe", "jack"});
		rosterListener.entriesUpdated(entries);
		Message<?> message = channel.receive(10);
		assertEquals(entries, message.getPayload());
	}
	
	@Test
	public void testWithImplicitXmppConnection(){
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, mock(XMPPConnection.class));
		XmppRosterEventMessageDrivenEndpoint endpoint = new XmppRosterEventMessageDrivenEndpoint();
		endpoint.setBeanFactory(bf);
		endpoint.afterPropertiesSet();
		assertNotNull(TestUtils.getPropertyValue(endpoint,"xmppConnection"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNoXmppConnection(){
		XmppRosterEventMessageDrivenEndpoint handler = new XmppRosterEventMessageDrivenEndpoint();
		handler.afterPropertiesSet();
	}
}
