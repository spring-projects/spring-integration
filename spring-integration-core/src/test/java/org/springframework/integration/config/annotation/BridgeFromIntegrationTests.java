/*
 * Copyright 2018-2024 the original author or authors.
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
