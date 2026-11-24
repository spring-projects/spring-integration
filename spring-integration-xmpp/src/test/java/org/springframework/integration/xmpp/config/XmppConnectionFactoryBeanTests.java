/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xmpp.config;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.junit.jupiter.api.Test;
import org.jxmpp.stringprep.XmppStringprepException;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.mockito.Mockito.mock;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Philipp Etschel
 * @author Glenn Renfro
 */
class XmppConnectionFactoryBeanTests {

	@Test
	void testXmppConnectionFactoryBean() throws XmppStringprepException {
		XmppConnectionFactoryBean xmppConnectionFactoryBean = new XmppConnectionFactoryBean();
		xmppConnectionFactoryBean.setConnectionConfiguration(
				XMPPTCPConnectionConfiguration.builder()
						.setXmppDomain("testDomain")
						.build());
		XMPPConnection connection = xmppConnectionFactoryBean.createInstance();
		assertThat(connection).isNotNull();
	}

	@Test
	void testSecurityModeRequiredByDefault() throws XmppStringprepException {
		XmppConnectionFactoryBean xmppConnectionFactoryBean = new XmppConnectionFactoryBean();
		xmppConnectionFactoryBean.setHost("localhost");
		xmppConnectionFactoryBean.setUser("happy.user@my.domain");
		xmppConnectionFactoryBean.setPassword("testpass");
		XMPPConnection connection = xmppConnectionFactoryBean.createInstance();
		assertThatObject(TestUtils.getPropertyValue(connection, "config.securityMode"))
				.isEqualTo(SecurityMode.required);
	}

	@Test
	void testSecurityModeExplicitlyConfigured() throws XmppStringprepException {
		XmppConnectionFactoryBean xmppConnectionFactoryBean = new XmppConnectionFactoryBean();
		xmppConnectionFactoryBean.setHost("localhost");
		xmppConnectionFactoryBean.setUser("happy.user@my.domain");
		xmppConnectionFactoryBean.setPassword("testpass");
		xmppConnectionFactoryBean.setSecurityMode(SecurityMode.disabled);
		XMPPConnection connection = xmppConnectionFactoryBean.createInstance();
		assertThatObject(TestUtils.getPropertyValue(connection, "config.securityMode"))
				.isEqualTo(SecurityMode.disabled);
	}

	@Test
	void testSecurityModeNotAppliedToProvidedConnectionConfiguration() throws XmppStringprepException {
		XmppConnectionFactoryBean xmppConnectionFactoryBean = new XmppConnectionFactoryBean();
		xmppConnectionFactoryBean.setSecurityMode(SecurityMode.required);
		xmppConnectionFactoryBean.setConnectionConfiguration(
				XMPPTCPConnectionConfiguration.builder()
						.setXmppDomain("testDomain")
						.setSecurityMode(SecurityMode.ifpossible)
						.build());
		XMPPConnection connection = xmppConnectionFactoryBean.createInstance();
		assertThatObject(TestUtils.getPropertyValue(connection, "config.securityMode"))
				.isEqualTo(SecurityMode.ifpossible);
	}

	@Test
	void testXmppConnectionFactoryBeanViaConfig() {
		assertThatNoException()
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("XmppConnectionFactoryBeanTests-context.xml",
								this.getClass())
								.close());
	}

	@Test
	void testXmppConnectionFactoryBeanNoRoster() throws Exception {
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
	void testXmppConnectionFactoryBeanWithRoster() throws Exception {
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
