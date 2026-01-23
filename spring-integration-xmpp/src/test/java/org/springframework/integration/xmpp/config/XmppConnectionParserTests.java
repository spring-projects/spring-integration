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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.roster.Roster;
import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Florian Schmaus
 * @author Artem Bilan
 * @author Glenn Renfro
 */
public class XmppConnectionParserTests {

	@Test
	public void testSimpleConfiguration() {
		ConfigurableApplicationContext ac =
				new ClassPathXmlApplicationContext("XmppConnectionParserTests-simple.xml", this.getClass());
		XMPPConnection connection = ac.getBean("connection", XMPPConnection.class);
		assertThat(connection.getXMPPServiceDomain().toString()).isEqualTo("my.domain");
		assertThat(connection.isConnected()).isFalse();
		XmppConnectionFactoryBean xmppFb = ac.getBean("&connection", XmppConnectionFactoryBean.class);
		assertThat(TestUtils.<String>getPropertyValue(xmppFb, "user")).isEqualTo("happy.user@my.domain");
		assertThat(TestUtils.<String>getPropertyValue(xmppFb, "password")).isEqualTo("blah");
		assertThat(TestUtils.<String>getPropertyValue(xmppFb, "resource")).isNull();
		assertThat(TestUtils.<Roster.SubscriptionMode>getPropertyValue(xmppFb, "subscriptionMode").toString())
				.isEqualTo("accept_all");

		xmppFb = ac.getBean("&connectionWithResource", XmppConnectionFactoryBean.class);
		assertThat(TestUtils.<String>getPropertyValue(xmppFb, "resource")).isEqualTo("Smack");
		assertThat(TestUtils.<Object>getPropertyValue(xmppFb, "subscriptionMode")).isNull();
		ac.close();
	}

	@Test
	public void testCompleteConfiguration() {
		ConfigurableApplicationContext ac =
				new ClassPathXmlApplicationContext("XmppConnectionParserTests-complete.xml", this.getClass());
		XMPPConnection connection = ac.getBean("connection", XMPPConnection.class);
		assertThat(connection.getXMPPServiceDomain().toString()).isEqualTo("foogle.com");
		assertThat(connection.isConnected()).isFalse();
		XmppConnectionFactoryBean xmppFb = ac.getBean("&connection", XmppConnectionFactoryBean.class);
		assertThat(TestUtils.<String>getPropertyValue(xmppFb, "user")).isEqualTo("happy.user");
		assertThat(TestUtils.<String>getPropertyValue(xmppFb, "password")).isEqualTo("blah");
		assertThat(TestUtils.<String>getPropertyValue(xmppFb, "resource")).isEqualTo("SpringSource");
		assertThat(TestUtils.getPropertyValue(xmppFb, "subscriptionMode").toString()).isEqualTo("reject_all");
		ac.close();
	}

}
