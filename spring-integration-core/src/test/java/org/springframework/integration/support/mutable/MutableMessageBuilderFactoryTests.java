/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.support.mutable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stuart Williams
 * @since 4.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
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
