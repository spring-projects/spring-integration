/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.support.mutable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MutableMessageBuilderFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stuart Williams
 * @author Artem Bilan
 *
 * @since 4.2
 */
@SpringJUnitConfig
@DirtiesContext
public class MutableMessageBuilderFactoryTests {

	@Autowired
	ContextConfiguration.TestGateway gateway;

	@Autowired
	CountDownLatch latch;

	@Test
	public void test() throws InterruptedException {

		this.gateway.input("hello!");

		boolean result = this.latch.await(2L, TimeUnit.SECONDS);

		assertThat(result).as("A failure means that that MMBF wasn't used").isTrue();
	}

	@Configuration
	@EnableIntegration
	@IntegrationComponentScan
	static class ContextConfiguration {

		@Bean
		public MutableMessageBuilderFactory messageBuilderFactory() {
			return new MutableMessageBuilderFactory();
		}

		@Bean
		public DirectChannel input() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel output() {
			return new DirectChannel();
		}

		@Bean
		public CountDownLatch latch() {
			return new CountDownLatch(1);
		}

		@MessagingGateway
		interface TestGateway {

			@Gateway(requestChannel = "input")
			void input(String payload);

		}

		@MessageEndpoint
		static class TestFilter {

			@Filter(inputChannel = "input", outputChannel = "output")
			public boolean filter(MessageHeaders headers) {
				// headers are immutable, so if this passes without exception,
				// the MutableMessageBuilderFactory *was* used...
				try {
					headers.put("foo", "bar");
					return true;
				}
				catch (UnsupportedOperationException e) {
					return false;
				}
			}

		}

		@MessageEndpoint
		static class Counter {

			@Autowired
			CountDownLatch latch;

			@ServiceActivator(inputChannel = "output")
			public void count(@Payload String message) {
				latch.countDown();
			}

		}

	}

}
