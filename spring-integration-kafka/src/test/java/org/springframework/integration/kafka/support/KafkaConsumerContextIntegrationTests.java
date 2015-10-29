/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.integration.kafka.support;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.kafka.rule.KafkaRule;
import org.springframework.integration.kafka.serializer.common.StringDecoder;
import org.springframework.integration.kafka.util.TopicUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import kafka.admin.AdminUtils;
import kafka.consumer.ConsumerConfig;

/**
 * @author Gary Russell
 * @since 1.2.2
 */
@Deprecated
@SuppressWarnings("deprecation")
public class KafkaConsumerContextIntegrationTests {

	private static final String TOPIC = "springIntegrationTestInbound";

	@Rule
	public KafkaRule kafkaRule = new KafkaEmbedded(1);

	@After
	public void tearDown() {
		AdminUtils.deleteTopic(kafkaRule.getZkClient(), TOPIC);
	}

	@Test
	public void test() throws Exception {
		TopicUtils.ensureTopicCreated(kafkaRule.getZookeeperConnectionString(), TOPIC, 1, 1);

		final String suffix = UUID.randomUUID().toString();

		KafkaProducerContext producerContext = createProducerContext();
		KafkaProducerMessageHandler handler =
				new KafkaProducerMessageHandler(producerContext);

		Message<String> msg = MessageBuilder.withPayload("foo" + suffix)
				.setHeader(KafkaHeaders.MESSAGE_KEY, "3")
				.setHeader(KafkaHeaders.TOPIC, TOPIC)
				.build();
		handler.handleMessage(msg);

		KafkaConsumerContext<?, ?> kafkaConsumerContext = createConsumerContext();

		Message<?> received = kafkaConsumerContext.receive();
		assertNotNull(received);
		Map<?, ?> payload = (Map<?, ?>) received.getPayload();
		assertNotNull(payload);
		Map<?, ?> topicMap = (Map<?, ?>) payload.get(TOPIC);
		assertNotNull(topicMap);
		List<?> list = (List<?>) topicMap.get(0);
		assertNotNull(list);
		assertThat(list.size(), greaterThanOrEqualTo(1));
		assertEquals("foo" + suffix, list.get(0));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private KafkaConsumerContext createConsumerContext() {
		KafkaConsumerContext kafkaConsumerContext = new KafkaConsumerContext();
		Map<String, ConsumerConfiguration> map = new HashMap<>();

		ConsumerMetadata consumerMetadata = new ConsumerMetadata();
		consumerMetadata.setGroupId("foo");
		consumerMetadata.setValueDecoder(new StringDecoder());
		consumerMetadata.setKeyDecoder(new StringDecoder());

		Map<String, Integer> topicStreamMap = new HashMap<>();
		topicStreamMap.put(TOPIC, 1);
		consumerMetadata.setTopicStreamMap(topicStreamMap);

		Properties properties = new Properties();
		properties.put("zookeeper.connect", kafkaRule.getZookeeperConnectionString());
		properties.put("group.id", "foo");
		ConsumerConfig consumerConfig = new ConsumerConfig(properties);
		ConsumerConnectionProvider consumerConnectionProvider = new ConsumerConnectionProvider(consumerConfig);

		MessageLeftOverTracker messageLeftOverTracker = mock(MessageLeftOverTracker.class);
		ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration(consumerMetadata,
				consumerConnectionProvider, messageLeftOverTracker);
		map.put("config", consumerConfiguration);

		kafkaConsumerContext.setConsumerConfigurations(map);
		return kafkaConsumerContext;
	}

	private KafkaProducerContext createProducerContext() throws Exception {
		KafkaProducerContext kafkaProducerContext = new KafkaProducerContext();
		ProducerMetadata<String, String> producerMetadata = new ProducerMetadata<>(TOPIC, String.class, String.class,
				new StringSerializer(), new StringSerializer());

		Properties props = new Properties();
		props.put("linger.ms", "1000");
		ProducerFactoryBean<String, String> producer =
				new ProducerFactoryBean<>(producerMetadata, kafkaRule.getBrokersAsString(), props);
		ProducerConfiguration<String, String> config =
				new ProducerConfiguration<>(producerMetadata, producer.getObject());
		Map<String, ProducerConfiguration<?, ?>> producerConfigurationMap =
				Collections.<String, ProducerConfiguration<?, ?>>singletonMap(TOPIC, config);
		kafkaProducerContext.setProducerConfigurations(producerConfigurationMap);
		return kafkaProducerContext;
	}

}
