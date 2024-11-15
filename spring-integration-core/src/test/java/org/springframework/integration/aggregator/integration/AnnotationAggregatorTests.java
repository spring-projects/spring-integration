/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.aggregator.integration;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.SimpleMessageGroupProcessor;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.CorrelationStrategy;
import org.springframework.integration.annotation.ReleaseStrategy;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.AggregatorFactoryBean;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig(classes = AnnotationAggregatorTests.TestConfiguration.class)
@DirtiesContext
public class AnnotationAggregatorTests {

	@Autowired
	DirectChannel input;

	@Autowired
	PollableChannel output;

	@Autowired
	DirectChannel input2;

	@Autowired
	BeanFactory beanFactory;

	@Test
	public void testAggregationWithAnnotationStrategies() {
		input.send(MessageBuilder.withPayload("a").build());
		input.send(MessageBuilder.withPayload("b").build());
		Message<?> result = output.receive(10_000);
		assertThat(result).isNotNull()
				.extracting(Message::getPayload)
				.asString()
				.matches(".*payload.*?=a.*")
				.matches(".*payload.*?=b.*");
	}

	@Test
	public void aggregatorFromFactoryBean() {
		assertThat(this.beanFactory.getBeanProvider(AggregatingMessageHandler.class).stream()).hasSize(2);

		this.input2.send(
				MessageBuilder.withPayload("test")
						.setCorrelationId(1)
						.setSequenceSize(1)
						.setSequenceNumber(0)
						.build());

		Message<?> result = this.output.receive(10_000);
		assertThat(result).isNotNull().extracting(Message::getPayload).isEqualTo("test");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	@ImportResource("org/springframework/integration/aggregator/integration/AnnotationAggregatorTests-context.xml")
	public static class TestConfiguration {

		@Bean
		@ServiceActivator(inputChannel = "input2")
		AggregatorFactoryBean aggregatorFactoryBean(QueueChannel output) {
			AggregatorFactoryBean aggregatorFactoryBean = new AggregatorFactoryBean();
			aggregatorFactoryBean.setProcessorBean(new SimpleMessageGroupProcessor());
			aggregatorFactoryBean.setOutputChannel(output);
			return aggregatorFactoryBean;
		}

	}

	@SuppressWarnings("unused")
	private static class TestAggregator {

		@Aggregator
		public Message<?> aggregate(final List<Message<?>> messages) {
			return MessageBuilder.withPayload(messages.toString()).build();
		}

		@ReleaseStrategy
		public boolean release(final List<Message<?>> messages) {
			return messages.size() > 1;
		}

		@CorrelationStrategy
		public Object getKey(Message<?> message) {
			return "1";
		}

	}

}
