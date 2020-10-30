/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @since 4.3.23
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class SourcePollingChannelAdapterTests {

	@Autowired
	Config config;

	@Test
	void undeclaredCheckedException() throws InterruptedException {
		assertThat(this.config.latchl.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.config.caught).isInstanceOf(UndeclaredThrowableException.class);
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		volatile Exception caught;

		volatile CountDownLatch latchl = new CountDownLatch(1);

		@Bean
		public IntegrationFlow flow() {
			return IntegrationFlows.from(new BadMessageSource(), e -> e.poller(Pollers.fixedDelay(500)
					.advice(exceptionCaptor())))
					.nullChannel();
		}

		private MethodInterceptor exceptionCaptor() {
			return invocation -> {
				try {
					return invocation.proceed();
				}
				catch (Exception e) {
					Config.this.caught = e;
					throw e;
				}
				finally {
					Config.this.latchl.countDown();
				}
			};
		}

	}

	public static class BadMessageSource implements MessageSource<String> {

		@Override
		@Nullable
		public Message<String> receive() {
			try {
				Foo.class.newInstance();
			}
			catch (InstantiationException | IllegalAccessException e) {
			}
			return null;
		}

	}

	public static class Foo {

		public Foo() throws IOException {
			throw new IOException();
		}

	}

}

