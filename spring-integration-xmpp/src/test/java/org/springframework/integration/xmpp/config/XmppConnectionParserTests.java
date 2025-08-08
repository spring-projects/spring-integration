/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.config;

import org.jivesoftware.smack.XMPPConnection;
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
		assertThat(TestUtils.getPropertyValue(xmppFb, "user")).isEqualTo("happy.user@my.domain");
		assertThat(TestUtils.getPropertyValue(xmppFb, "password")).isEqualTo("blah");
		assertThat(TestUtils.getPropertyValue(xmppFb, "resource")).isNull();
		assertThat(TestUtils.getPropertyValue(xmppFb, "subscriptionMode").toString()).isEqualTo("accept_all");

		xmppFb = ac.getBean("&connectionWithResource", XmppConnectionFactoryBean.class);
		assertThat(TestUtils.getPropertyValue(xmppFb, "resource")).isEqualTo("Smack");
		assertThat(TestUtils.getPropertyValue(xmppFb, "subscriptionMode")).isNull();
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
		assertThat(TestUtils.getPropertyValue(xmppFb, "user")).isEqualTo("happy.user");
		assertThat(TestUtils.getPropertyValue(xmppFb, "password")).isEqualTo("blah");
		assertThat(TestUtils.getPropertyValue(xmppFb, "resource")).isEqualTo("SpringSource");
		assertThat(TestUtils.getPropertyValue(xmppFb, "subscriptionMode").toString()).isEqualTo("reject_all");
		ac.close();
	}

}
