/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.config.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 5.0.8
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = BridgeFromIntegrationTests.RootTestConfiguration.class)
public class BridgeFromIntegrationTests {

	@Autowired
	private MessageChannel gatewayChannel;

	@Autowired
	private PollableChannel outputChannel;

	@Test
	public void testBridgeFromConfiguration() {
		this.gatewayChannel.send(new GenericMessage<>("world"));

		Message<?> receive = this.outputChannel.receive(10_000);

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("hello world");
	}

	@Configuration
	@EnableIntegration
	@ComponentScan(
			basePackageClasses = BridgeFromIntegrationTests.class,
			useDefaultFilters = false,
			includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
					classes = {AnnotatedTestService.class, ScannedTestConfiguration.class}))
	public static class RootTestConfiguration {

	}

	@Configuration
	public static class ScannedTestConfiguration {

		@Bean
		@BridgeFrom("gatewayChannel")
		public DirectChannel inputChannel() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel gatewayChannel() {
			return new DirectChannel();
		}

		@Bean
		public PollableChannel outputChannel() {
			return new QueueChannel();
		}

	}

}
