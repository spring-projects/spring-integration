/*
 * Copyright 2020-present the original author or authors.
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

package org.springframework.integration.kafka.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.kafka.channel.PollableKafkaChannel;
import org.springframework.integration.kafka.channel.SubscribableKafkaChannel;
import org.springframework.integration.kafka.inbound.KafkaMessageSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ConsumerProperties;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 5.4
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class ChannelParserTests {

	@Autowired
	KafkaListenerContainerFactory<?> containerFactory;

	@Autowired
	KafkaOperations<?, ?> template;

	@Autowired
	KafkaMessageSource<?, ?> source;

	@Autowired
	SubscribableKafkaChannel ptp;

	@Autowired
	PollableKafkaChannel pollable;

	@Autowired
	SubscribableKafkaChannel pubSub;

	@Test
	void testParser() {
		assertThat(TestUtils.<String>getPropertyValue(this.ptp, "topic")).isEqualTo("ptpTopic");
		assertThat(TestUtils.<String>getPropertyValue(this.pubSub, "topic")).isEqualTo("pubSubTopic");
		assertThat(TestUtils.<Object>getPropertyValue(this.ptp, "container")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(this.pubSub, "container")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(this.ptp, "template")).isSameAs(this.template);
		assertThat(TestUtils.<Object>getPropertyValue(this.pubSub, "template")).isSameAs(this.template);
		assertThat(TestUtils.<Object>getPropertyValue(this.pollable, "template")).isSameAs(this.template);
		assertThat(TestUtils.<Object>getPropertyValue(this.pollable, "source")).isSameAs(this.source);
		assertThat(TestUtils.<String>getPropertyValue(this.ptp, "groupId")).isEqualTo("ptpGroup");
		assertThat(TestUtils.<String>getPropertyValue(this.pubSub, "groupId")).isEqualTo("pubSubGroup");
		assertThat(TestUtils.<String>getPropertyValue(this.pollable, "groupId")).isEqualTo("pollableGroup");
	}

	@Configuration
	@ImportResource("org/springframework/integration/kafka/config/xml/channels-context.xml")
	public static class Config {

		@SuppressWarnings("unchecked")
		@Bean
		public KafkaListenerContainerFactory<?> containerFactory() {
			ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
					new ConcurrentKafkaListenerContainerFactory<>();
			factory.setConsumerFactory(mock(ConsumerFactory.class));
			return factory;
		}

		@Bean
		public KafkaOperations<?, ?> template() {
			return mock(KafkaOperations.class);
		}

		@SuppressWarnings("unchecked")
		@Bean
		public KafkaMessageSource<?, ?> source() {
			ConsumerProperties properties = new ConsumerProperties("test");
			return new KafkaMessageSource<Object, Object>(mock(ConsumerFactory.class), properties, true);
		}

	}

}
