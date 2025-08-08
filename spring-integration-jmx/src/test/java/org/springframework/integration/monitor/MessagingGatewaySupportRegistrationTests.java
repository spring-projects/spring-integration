/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.monitor;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 4.2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class MessagingGatewaySupportRegistrationTests {

	@Autowired
	private MBeanServer server;

	@Test
	public void testHandlerMBeanRegistration() throws Exception {
		Set<ObjectName> names = this.server
				.queryNames(new ObjectName("org.springframework.integration:*,name=testGateway"), null);
		assertThat(names.size()).isEqualTo(1);
		names = this.server.queryNames(new ObjectName("org.springframework.integration:*,type=MessageSource,name=foo"),
				null);
		assertThat(names.size()).isEqualTo(1);
		names = this.server.queryNames(new ObjectName("org.springframework.integration:*,name=\"foo#2\""), null);
		assertThat(names.size()).isEqualTo(1);
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationMBeanExport(server = "mbeanServer")
	public static class ContextConfiguration {

		@Bean
		public MessagingGatewaySupport testGateway() {
			return new MessagingGatewaySupport() {

			};
		}

		@Bean
		public MessageChannel foo() {
			return new NullChannel();
		}

		@Bean(name = "org.springframework.integration.foo1")
		public MessagingGatewaySupport anonymous1() {
			MessagingGatewaySupport messagingGatewaySupport = new MessagingGatewaySupport() {

			};
			messagingGatewaySupport.setRequestChannel(foo());
			return messagingGatewaySupport;
		}

		@Bean(name = "org.springframework.integration.foo2")
		public MessagingGatewaySupport anonymous2() {
			MessagingGatewaySupport messagingGatewaySupport = new MessagingGatewaySupport() {

			};
			messagingGatewaySupport.setRequestChannelName("foo");
			return messagingGatewaySupport;
		}

		@Bean
		public static MBeanServerFactoryBean mbeanServer() {
			return new MBeanServerFactoryBean();
		}

	}

}
