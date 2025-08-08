/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.jmx.configuration;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class DslMBeanTests {

	@Autowired
	private MBeanServer server;

	@Test
	void testRuntimeBeanRegistration(@Autowired IntegrationFlowContext context) throws Exception {
		Set<ObjectName> query = this.server.queryNames(new ObjectName("dsl:type=MessageChannel,*"), null);
		assertThat(query).hasSize(3);

		query = this.server.queryNames(new ObjectName("dsl:type=MessageHandler,*"), null);
		assertThat(query).hasSize(2);

		query = this.server.queryNames(new ObjectName("dsl:type=MessageSource,*"), null);
		assertThat(query).hasSize(0);

		IntegrationFlow dynamicFlow =
				IntegrationFlow.fromSupplier(() -> "foo", e -> e.poller(p -> p.fixedDelay(1000)))
						.channel("channelTwo")
						.nullChannel();

		IntegrationFlowContext.IntegrationFlowRegistration registration =
				context.registration(dynamicFlow)
						.id("dynamic")
						.register();

		query = this.server.queryNames(new ObjectName("dsl:type=MessageChannel,*"), null);
		assertThat(query).hasSize(4);

		query = this.server.queryNames(new ObjectName("dsl:type=MessageHandler,*"), null);
		assertThat(query).hasSize(3);

		query = this.server.queryNames(new ObjectName("dsl:type=MessageSource,*"), null);
		assertThat(query).hasSize(1);

		registration.destroy();

		query = this.server.queryNames(new ObjectName("dsl:type=MessageChannel,*"), null);
		assertThat(query).hasSize(3);

		query = this.server.queryNames(new ObjectName("dsl:type=MessageHandler,*"), null);
		assertThat(query).hasSize(2);

		query = this.server.queryNames(new ObjectName("dsl:type=MessageSource,*"), null);
		assertThat(query).hasSize(0);
	}

	@Configuration
	@EnableIntegrationMBeanExport(defaultDomain = "dsl", server = "mbeanServer")
	@EnableIntegration
	public static class Config {

		@Bean
		public static MBeanServerFactoryBean mbeanServer() {
			return new MBeanServerFactoryBean();
		}

		@Bean
		public IntegrationFlow staticFlow() {
			return IntegrationFlow.from("channelOne")
					.nullChannel();
		}

	}

}
