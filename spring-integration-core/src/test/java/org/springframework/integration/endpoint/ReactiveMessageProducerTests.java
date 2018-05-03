/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import reactor.core.publisher.Flux;

/**
 * @author Gary Russell
 * @since 5.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class ReactiveMessageProducerTests {

	@Autowired
	public Config config;

	@Test
	public void test() throws Exception {
		for (int i = 0; i < 5; i++) {
			this.config.producer().produce();
		}
		assertThat(this.config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.config.received.get(0).getPayload()).isEqualTo("FOO");
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		private final List<Message<?>> received = new ArrayList<>();

		private final CountDownLatch latch = new CountDownLatch(5);

		@Bean
		public MyProducer producer() {
			MyProducer producer = new MyProducer();
			producer.setReactive(true);
			producer.setOutputChannelName("in");
			return producer;
		}

		@ServiceActivator(inputChannel = "in", outputChannel = "out")
		public Flux<Message<?>> handle1(Flux<Message<?>> flux) {
			return flux.map(m -> MessageBuilder.withPayload(((String) m.getPayload()).toUpperCase()).build());
		}

		@ServiceActivator(inputChannel = "out")
		public void handle2(final Flux<Message<?>> flux) {
			Executors.newSingleThreadExecutor().execute(() -> {
				flux.map(m -> {
					this.received.add(m);
					System.out.println(m);
					latch.countDown();
					return m;
				})
					.subscribe();
			});
		}

	}

	private static class MyProducer extends MessageProducerSupport {

		MyProducer() {
			super();
		}

		void produce() {
			sendMessage(new GenericMessage<>("foo"));
		}

	}

}
