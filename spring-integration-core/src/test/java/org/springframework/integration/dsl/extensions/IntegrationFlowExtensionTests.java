/*
 * Copyright 2020-2025 the original author or authors.
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

package org.springframework.integration.dsl.extensions;

import java.util.Arrays;
import java.util.function.Consumer;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.AggregatorSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowExtension;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 5.3
 */
@SpringJUnitConfig
@DirtiesContext
public class IntegrationFlowExtensionTests {

	@Autowired
	@Qualifier("customFlowDefinition.input")
	SubscribableChannel customFlowDefinitionInput;

	@Test
	public void testCustomFlowDefinition() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> testMessage =
				MessageBuilder.withPayload(Arrays.asList("one", "two", "three"))
						.setReplyChannel(replyChannel)
						.build();
		this.customFlowDefinitionInput.send(testMessage);

		Message<?> replyMessage = replyChannel.receive(10_000);

		assertThat(replyMessage)
				.isNotNull()
				.extracting(Message::getPayload)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.containsOnly("ONE", "TWO", "THREE");
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow customFlowDefinition() {
			return
					new CustomIntegrationFlowDefinition()
							.log()
							.upperCaseAfterSplit()
							.channel("innerChannel")
							.customAggregate(customAggregatorSpec ->
									customAggregatorSpec.expireGroupsUponCompletion(true))
							.log()
							.get();
		}

	}

	public static class CustomIntegrationFlowDefinition
			extends IntegrationFlowExtension<CustomIntegrationFlowDefinition> {

		public CustomIntegrationFlowDefinition upperCaseAfterSplit() {
			return split()
					.transform("payload.toUpperCase()");
		}

		public CustomIntegrationFlowDefinition customAggregate(Consumer<CustomAggregatorSpec> aggregator) {
			return register(new CustomAggregatorSpec(), aggregator);
		}

	}

	public static class CustomAggregatorSpec extends AggregatorSpec {

		CustomAggregatorSpec() {
			outputProcessor((group) ->
					group.getMessages()
							.stream()
							.map(Message::getPayload)
							.toList());
		}

	}

}
