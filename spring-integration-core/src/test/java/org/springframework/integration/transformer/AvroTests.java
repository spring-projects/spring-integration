/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.Avro;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @since 5.2
 *
 */
@SpringJUnitConfig
public class AvroTests {

	@Test
	void testTransformers(@Autowired Config config) {
		AvroTestClass test = new AvroTestClass("baz", "fiz");
		config.in().send(new GenericMessage<>(test));
		assertThat(config.tapped().receive(0))
			.isNotNull()
			.extracting(msg -> msg.getPayload())
			.isInstanceOf(byte[].class);
		assertThat(config.out().receive(0))
			.isNotNull()
			.extracting(msg -> msg.getPayload())
			.isEqualTo(test)
			.isNotSameAs(test);
	}

	@Configuration
	@EnableIntegration
	public static class Config {


		@Bean
		public IntegrationFlow flow() {
			return IntegrationFlows.from(in())
					.transform(Avro.toAvro())
					.wireTap(tapped())
					.transform(Avro.fromAvro(AvroTestClass.class))
					.channel(out())
					.get();
		}

		@Bean
		public DirectChannel in() {
			return new DirectChannel();
		}

		@Bean
		public PollableChannel tapped() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel out() {
			return new QueueChannel();
		}

	}

}
