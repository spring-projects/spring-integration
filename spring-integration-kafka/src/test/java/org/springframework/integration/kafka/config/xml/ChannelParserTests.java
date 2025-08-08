/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
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
		assertThat(TestUtils.getPropertyValue(this.ptp, "topic")).isEqualTo("ptpTopic");
		assertThat(TestUtils.getPropertyValue(this.pubSub, "topic")).isEqualTo("pubSubTopic");
		assertThat(TestUtils.getPropertyValue(this.ptp, "container")).isNotNull();
		assertThat(TestUtils.getPropertyValue(this.pubSub, "container")).isNotNull();
		assertThat(TestUtils.getPropertyValue(this.ptp, "template")).isSameAs(this.template);
		assertThat(TestUtils.getPropertyValue(this.pubSub, "template")).isSameAs(this.template);
		assertThat(TestUtils.getPropertyValue(this.pollable, "template")).isSameAs(this.template);
		assertThat(TestUtils.getPropertyValue(this.pollable, "source")).isSameAs(this.source);
		assertThat(TestUtils.getPropertyValue(this.ptp, "groupId")).isEqualTo("ptpGroup");
		assertThat(TestUtils.getPropertyValue(this.pubSub, "groupId")).isEqualTo("pubSubGroup");
		assertThat(TestUtils.getPropertyValue(this.pollable, "groupId")).isEqualTo("pollableGroup");
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
