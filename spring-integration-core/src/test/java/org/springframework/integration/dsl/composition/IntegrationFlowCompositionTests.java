/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.integration.dsl.composition;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Artem Bilan
 *
 * @since 5.5.4
 */
@SpringJUnitConfig
@DirtiesContext
public class IntegrationFlowCompositionTests {

	@Autowired
	IntegrationFlow templateFlow;

	@Autowired
	@Qualifier("mainFlow.input")
	DirectChannel mainFlowInput;

	@Autowired
	QueueChannel otherFlowResultChannel;

	@Test
	void testToOperator() {
		this.mainFlowInput.send(new GenericMessage<>("hello"));
		Message<?> receive = this.otherFlowResultChannel.receive(10_000);
		assertThat(receive).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("HELLO from other flow");
	}

	@Autowired
	@Qualifier("requestReplyMainFlow.input")
	DirectChannel requestReplyMainFlowInput;

	@Test
	void testToWithRequestReply() {
		QueueChannel replyChannel = new QueueChannel();
		this.requestReplyMainFlowInput.send(
				MessageBuilder.withPayload("TEST")
						.setReplyChannel(replyChannel)
						.build());
		Message<?> receive = replyChannel.receive(10_000);
		assertThat(receive).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("Reply for: test");
	}

	@Autowired
	QueueChannel compositionMainFlowResult;

	@Test
	void testFromComposition() {
		Message<?> receive = this.compositionMainFlowResult.receive(10_000);
		assertThat(receive).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("TEST DATA");

		receive = this.compositionMainFlowResult.receive(10_000);
		assertThat(receive).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("TEST DATA");
	}

	@Autowired
	@Qualifier("firstFlow.input")
	DirectChannel firstFlowInput;

	@Autowired
	QueueChannel lastFlowResult;

	@Test
	void testFromToComposition() {
		this.firstFlowInput.send(new GenericMessage<>("start"));

		Message<?> receive = this.lastFlowResult.receive(10_000);
		assertThat(receive).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("start, and first flow, and middle flow, and last flow");
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean(PollerMetadata.DEFAULT_POLLER)
		PollerMetadata defaultPoller() {
			return Pollers.fixedDelay(100).get();
		}

		@Bean
		IntegrationFlow mainFlow(IntegrationFlow otherFlow) {
			return f -> f
					.<String, String>transform(String::toUpperCase)
					.to(otherFlow);
		}

		@Bean
		IntegrationFlow otherFlow() {
			return f -> f
					.<String, String>transform(p -> p + " from other flow")
					.channel(c -> c.queue("otherFlowResultChannel"));
		}

		@Bean
		IntegrationFlow requestReplyMainFlow(IntegrationFlow templateFlow) {
			return f -> f
					.<String, String>transform(String::toLowerCase)
					.to(templateFlow);
		}

		@Bean
		IntegrationFlow templateFlow() {
			return f -> f
					.<String, String>transform("Reply for: "::concat);
		}

		@Bean
		IntegrationFlow templateSourceFlow() {
			return IntegrationFlows.fromSupplier(() -> "test data")
					.channel("sourceChannel")
					.get();
		}

		@Bean
		IntegrationFlow compositionMainFlow(IntegrationFlow templateSourceFlow) {
			return IntegrationFlows.from(templateSourceFlow)
					.<String, String>transform(String::toUpperCase)
					.channel(c -> c.queue("compositionMainFlowResult"))
					.get();
		}

		@Bean
		IntegrationFlow firstFlow() {
			return f -> f
					.<String, String>transform(p -> p + ", and first flow");
		}

		@Bean
		IntegrationFlow middleFlow(IntegrationFlow firstFlow, IntegrationFlow lastFlow) {
			return IntegrationFlows.from(firstFlow)
					.<String, String>transform(p -> p + ", and middle flow")
					.to(lastFlow);
		}

		@Bean
		IntegrationFlow lastFlow() {
			return f -> f
					.<String, String>transform(p -> p + ", and last flow")
					.channel(c -> c.queue("lastFlowResult"));
		}

	}

}
