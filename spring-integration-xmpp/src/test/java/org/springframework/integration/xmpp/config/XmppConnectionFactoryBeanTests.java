/*
 * Copyright 2002-2016 the original author or authors.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class XmppConnectionFactoryBeanTests {

	@Test
	public void testXmppConnectionFactoryBean() throws Exception {
		XmppConnectionFactoryBean xmppConnectionFactoryBean = new XmppConnectionFactoryBean();
		xmppConnectionFactoryBean.setConnectionConfiguration(mock(XMPPTCPConnectionConfiguration.class));
		XMPPConnection connection = xmppConnectionFactoryBean.createInstance();
		assertNotNull(connection);
	}

	@Test
	public void testXmppConnectionFactoryBeanViaConfig() throws Exception {
		new ClassPathXmlApplicationContext("XmppConnectionFactoryBeanTests-context.xml", this.getClass()).close();
		// the fact that no exception was thrown satisfies this test
	}

	@Test
	public void testXmppConnectionFactoryBeanNoRoster() throws Exception {
		XmppConnectionFactoryBean xmppConnectionFactoryBean = new XmppConnectionFactoryBean() {
			@Override
			protected XMPPConnection createInstance() throws Exception {
				this.connection = mock(XMPPTCPConnection.class);
				return this.connection;
			}
		};
		xmppConnectionFactoryBean.setConnectionConfiguration(mock(XMPPTCPConnectionConfiguration.class));
		xmppConnectionFactoryBean.setSubscriptionMode(null);
		XMPPConnection connection = xmppConnectionFactoryBean.createInstance();
		assertNotNull(connection);
		assertTrue(Roster.getInstanceFor(connection).isRosterLoadedAtLogin());
		xmppConnectionFactoryBean.start();
		assertFalse(Roster.getInstanceFor(connection).isRosterLoadedAtLogin());
	}

	@Test
	public void testXmppConnectionFactoryBeanWithRoster() throws Exception {
		XmppConnectionFactoryBean xmppConnectionFactoryBean = new XmppConnectionFactoryBean() {
			@Override
			protected XMPPConnection createInstance() throws Exception {
				this.connection = mock(XMPPTCPConnection.class);
				return this.connection;
			}
		};
		xmppConnectionFactoryBean.setConnectionConfiguration(mock(XMPPTCPConnectionConfiguration.class));
		xmppConnectionFactoryBean.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
		XMPPConnection connection = xmppConnectionFactoryBean.createInstance();
		assertNotNull(connection);
		assertTrue(Roster.getInstanceFor(connection).isRosterLoadedAtLogin());
		xmppConnectionFactoryBean.start();
		assertTrue(Roster.getInstanceFor(connection).isRosterLoadedAtLogin());
	}

}
