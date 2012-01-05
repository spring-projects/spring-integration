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

import java.util.List;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.test.util.TestUtils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Oleg Zhurakousky
 */
public class XmppConnectionParserTests {
	
	@Test
	public void testSmackSasl() {
		/*
		 * Possible SASL mechanisms
		 * EXTERNAL, GSSAPI, DIGEST-MD5, CRAM-MD5, PLAIN, ANONYMOUS
		 */
		// values are set in META-INF/smack-config.xml
		List<String> saslMechNames = SmackConfiguration.getSaslMechs();
		assertEquals(2, saslMechNames.size());
		assertEquals("PLAIN", saslMechNames.get(0));
	}

	@Test
	public void testSimpleConfiguration() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("XmppConnectionParserTests-simple.xml", this.getClass());
		XMPPConnection connection  = ac.getBean("connection", XMPPConnection.class);
		assertEquals("localhost", connection.getServiceName());
		assertEquals("localhost", connection.getHost());
		assertEquals(5222, connection.getPort());
		assertFalse(connection.isConnected()); 
		XmppConnectionFactoryBean xmppFb = ac.getBean("&connection", XmppConnectionFactoryBean.class);
		assertEquals("happy.user", TestUtils.getPropertyValue(xmppFb, "user"));
		assertEquals("blah", TestUtils.getPropertyValue(xmppFb, "password"));
		assertEquals("Smack", TestUtils.getPropertyValue(xmppFb, "resource"));
		assertEquals("accept_all", TestUtils.getPropertyValue(xmppFb, "subscriptionMode"));
	}
	
	@Test
	public void testDefaultConnectionName() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("XmppConnectionParserTests-simple.xml", this.getClass());
		assertTrue(ac.containsBean("xmppConnection"));
	}

	@Test
	public void testCompleteConfiguration() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("XmppConnectionParserTests-complete.xml", this.getClass());
		XMPPConnection connection  = ac.getBean("connection", XMPPConnection.class);
		assertEquals("foogle.com", connection.getServiceName());
		assertEquals("localhost", connection.getHost());
		assertEquals(6222, connection.getPort());
		assertFalse(connection.isConnected()); 
		XmppConnectionFactoryBean xmppFb = ac.getBean("&connection", XmppConnectionFactoryBean.class);
		assertEquals("happy.user", TestUtils.getPropertyValue(xmppFb, "user"));
		assertEquals("blah", TestUtils.getPropertyValue(xmppFb, "password"));
		assertEquals("SpringSource", TestUtils.getPropertyValue(xmppFb, "resource"));
		assertEquals("reject_all", TestUtils.getPropertyValue(xmppFb, "subscriptionMode"));
	}

}
