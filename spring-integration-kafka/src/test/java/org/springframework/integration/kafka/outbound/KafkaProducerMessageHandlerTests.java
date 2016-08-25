/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.kafka.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.kafka.test.assertj.KafkaConditions.key;
import static org.springframework.kafka.test.assertj.KafkaConditions.partition;
import static org.springframework.kafka.test.assertj.KafkaConditions.value;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.KafkaNull;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class KafkaProducerMessageHandlerTests {

	private static String topic1 = "testTopic1out";

	private static String topic2 = "testTopic2out";

	@ClassRule
	public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, topic1, topic2);

	private static Consumer<Integer, String> consumer;

	@BeforeClass
	public static void setUp() throws Exception {
		ConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(
				KafkaTestUtils.consumerProps("testOut", "true", embeddedKafka));
		consumer = cf.createConsumer();
		embeddedKafka.consumeFromAllEmbeddedTopics(consumer);
	}

	@Test
	public void testOutbound() {
		ProducerFactory<Integer, String> producerFactory = new DefaultKafkaProducerFactory<>(
				KafkaTestUtils.producerProps(embeddedKafka));
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(producerFactory);
		KafkaProducerMessageHandler<Integer, String> handler = new KafkaProducerMessageHandler<>(template);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<?> message = MessageBuilder.withPayload("foo")
				.setHeader(KafkaHeaders.TOPIC, topic1)
				.setHeader(KafkaHeaders.MESSAGE_KEY, 2)
				.setHeader(KafkaHeaders.PARTITION_ID, 1)
				.build();
		handler.handleMessage(message);

		ConsumerRecord<Integer, String> record = KafkaTestUtils.getSingleRecord(consumer, topic1);
		assertThat(record).has(key(2));
		assertThat(record).has(partition(1));
		assertThat(record).has(value("foo"));

		message = MessageBuilder.withPayload("bar")
				.setHeader(KafkaHeaders.TOPIC, topic1)
				.setHeader(KafkaHeaders.PARTITION_ID, 0)
				.build();
		handler.handleMessage(message);
		record = KafkaTestUtils.getSingleRecord(consumer, topic1);
		assertThat(record).has(key((Integer) null));
		assertThat(record).has(partition(0));
		assertThat(record).has(value("bar"));

		message = MessageBuilder.withPayload("baz")
				.setHeader(KafkaHeaders.TOPIC, topic1)
				.build();
		handler.handleMessage(message);
		record = KafkaTestUtils.getSingleRecord(consumer, topic1);
		assertThat(record).has(key((Integer) null));
		assertThat(record).has(value("baz"));

		handler.setPartitionIdExpression(new SpelExpressionParser().parseExpression("headers['kafka_partitionId']"));

		message = MessageBuilder.withPayload(KafkaNull.INSTANCE)
				.setHeader(KafkaHeaders.TOPIC, topic1)
				.setHeader(KafkaHeaders.MESSAGE_KEY, 2)
				.setHeader(KafkaHeaders.PARTITION_ID, "1")
				.build();
		handler.handleMessage(message);

		record = KafkaTestUtils.getSingleRecord(consumer, topic1);
		assertThat(record).has(key(2));
		assertThat(record).has(partition(1));
		assertThat(record.value()).isNull();
	}

}
