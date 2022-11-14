/*
 * Copyright 2002-2022 the original author or authors.
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
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Philipp Etschel
 */
public class XmppConnectionFactoryBeanTests {

	@Test
	public void testXmppConnectionFactoryBean() throws Exception {
		XmppConnectionFactoryBean xmppConnectionFactoryBean = new XmppConnectionFactoryBean();
		xmppConnectionFactoryBean.setConnectionConfiguration(
				XMPPTCPConnectionConfiguration.builder()
						.setXmppDomain("foo")
						.build());
		XMPPConnection connection = xmppConnectionFactoryBean.createInstance();
		assertThat(connection).isNotNull();
	}

	@Test
	public void testXmppConnectionFactoryBeanViaConfig() {
		assertThatNoException()
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("XmppConnectionFactoryBeanTests-context.xml",
								this.getClass())
								.close());
	}

	@Test
	public void testXmppConnectionFactoryBeanNoRoster() throws Exception {
		XmppConnectionFactoryBean xmppConnectionFactoryBean =
				new XmppConnectionFactoryBean() {

					@Override
					protected XMPPConnection createInstance() {
						return mock(XMPPTCPConnection.class);
					}

				};

		xmppConnectionFactoryBean.setSubscriptionMode(null);
		xmppConnectionFactoryBean.afterPropertiesSet();
		xmppConnectionFactoryBean.start();
		XMPPConnection connection = xmppConnectionFactoryBean.getObject();

		assertThat(Roster.getInstanceFor(connection).isRosterLoadedAtLogin()).isFalse();
	}

	@Test
	public void testXmppConnectionFactoryBeanWithRoster() throws Exception {
		XmppConnectionFactoryBean xmppConnectionFactoryBean =
				new XmppConnectionFactoryBean() {

					@Override
					protected XMPPConnection createInstance() {
						return mock(XMPPTCPConnection.class);
					}

				};

		xmppConnectionFactoryBean.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
		xmppConnectionFactoryBean.afterPropertiesSet();
		xmppConnectionFactoryBean.start();
		XMPPConnection connection = xmppConnectionFactoryBean.getObject();

		assertThat(Roster.getInstanceFor(connection).isRosterLoadedAtLogin()).isTrue();
	}

}
