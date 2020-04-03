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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 *
 * @since 5.3
 */
@SpringJUnitConfig
@DirtiesContext
public class ReactiveMessageProducerTests {

	@Autowired
	public FluxMessageChannel fluxMessageChannel;

	@Autowired
	public MessageProducerSupport producer;

	@Test
	public void test() {
		assertThat(this.producer.isRunning()).isTrue();

		StepVerifier.create(
				Flux.from(this.fluxMessageChannel)
						.map(Message::getPayload)
						.cast(String.class))
				.expectNext("test1", "test2")
				.thenCancel()
				.verify();

		assertThat(this.producer.isRunning()).isFalse();
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public FluxMessageChannel fluxMessageChannel() {
			return new FluxMessageChannel();
		}

		@Bean
		public MessageProducerSupport producer() {
			MessageProducerSupport producer =
					new MessageProducerSupport() {

						@Override
						protected void doStart() {
							subscribeToPublisher(Flux.just("test1", "test2").map(GenericMessage::new));
						}

					};
			producer.setOutputChannel(fluxMessageChannel());
			return producer;
		}

	}

}
