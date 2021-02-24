/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.integration.kafka.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerProperties;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Anshul Mehra
 *
 * @since 5.4
 *
 */
class MessageSourceIntegrationTests {

	private static final String TOPIC1 = "MessageSourceIntegrationTests1";

	private static EmbeddedKafkaBroker embeddedKafka;

	@BeforeAll
	static void setup() {
		embeddedKafka = new EmbeddedKafkaBroker(1, true, 1, TOPIC1);
		embeddedKafka.afterPropertiesSet();
	}

	@AfterAll
	static void tearDown() {
		embeddedKafka.destroy();
	}

	@Test
	void testSource() throws Exception {
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testSource", "false", embeddedKafka);
		consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 2);
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		consumerProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 42);
		DefaultKafkaConsumerFactory<Integer, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
		ConsumerProperties consumerProperties = new ConsumerProperties(TOPIC1);
		consumerProperties.getKafkaConsumerProperties()
				.setProperty(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "2");
		final CountDownLatch assigned = new CountDownLatch(1);
		consumerProperties.setConsumerRebalanceListener(new ConsumerRebalanceListener() {

			@Override
			public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
			}

			@Override
			public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
				assigned.countDown();
			}

		});

		consumerProperties.setPollTimeout(10);

		KafkaMessageSource<Integer, String> source = new KafkaMessageSource<>(consumerFactory, consumerProperties);

		Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
		DefaultKafkaProducerFactory<Object, Object> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
		KafkaTemplate<Object, Object> template = new KafkaTemplate<>(producerFactory);
		template.send(TOPIC1, "foo");
		template.send(TOPIC1, "bar");
		template.send(TOPIC1, "baz");
		template.send(TOPIC1, "qux");
		Message<Object> received = source.receive();
		assertThat(assigned.await(10, TimeUnit.SECONDS)).isTrue();
		int n = 0;
		while (n++ < 100 && received == null) {
			received = source.receive();
		}
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("foo");
		received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("bar");
		received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("baz");
		received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("qux");
		received = source.receive();
		assertThat(received).isNull();
		assertThat(KafkaTestUtils.getPropertyValue(source, "consumer.fetcher.minBytes")).isEqualTo(2);
		source.destroy();
		producerFactory.destroy();
	}

}
