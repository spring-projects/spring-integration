/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.kafka.channnel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.kafka.channel.PollableKafkaChannel;
import org.springframework.integration.kafka.channel.PublishSubscribeKafkaChannel;
import org.springframework.integration.kafka.channel.SubscribableKafkaChannel;
import org.springframework.integration.kafka.inbound.KafkaMessageSource;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConsumerProperties;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
@SpringJUnitConfig
@EmbeddedKafka(topics = { "channel.1", "channel.2", "channel.3" }, partitions = 1)
public class ChannelTests {

	@Test
	void subscribablePtp(@Autowired SubscribableChannel ptp) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Message<?>> message = new AtomicReference<>();
		ptp.subscribe(msg -> {
			message.set(msg);
			latch.countDown();
		});
		Message<?> msg = new GenericMessage<>("foo");
		ptp.send(msg, 10_000L);
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(message.get().getPayload()).isEqualTo("foo");
		assertThat(message.get().getHeaders().get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo("channel.1");
	}

	@Test
	void pubSub(@Autowired SubscribableChannel pubSub) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(2);
		AtomicReference<Message<?>> message = new AtomicReference<>();
		pubSub.subscribe(msg -> {
			message.set(msg);
			latch.countDown();
		});
		pubSub.subscribe(msg -> {
			message.set(msg);
			latch.countDown();
		});
		Message<?> msg = new GenericMessage<>("foo");
		pubSub.send(msg, 10_000L);
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(message.get().getPayload()).isEqualTo("foo");
		assertThat(message.get().getHeaders().get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo("channel.2");
	}

	@Test
	void pollable(@Autowired PollableChannel pollable) {
		Message<?> msg = new GenericMessage<>("foo");
		pollable.send(msg, 10_000L);
		Message<?> message = pollable.receive(10_000L);
		assertThat(message.getPayload()).isEqualTo("foo");
		assertThat(message.getHeaders().get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo("channel.3");
	}

	@Configuration
	public static class Config {

		@Autowired
		private EmbeddedKafkaBroker broker;

		@Bean
		public ProducerFactory<Integer, String> pf() {
			return new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(this.broker));
		}

		@Bean
		public ConsumerFactory<Integer, String> cf() {
			Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("channelTests", "false", this.broker);
			consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
			return new DefaultKafkaConsumerFactory<>(consumerProps);
		}

		@Bean
		public KafkaTemplate<Integer, String> template(ProducerFactory<Integer, String> pf) {
			return new KafkaTemplate<>(pf);
		}

		@Bean
		public ConcurrentKafkaListenerContainerFactory<Integer, String> factory(ConsumerFactory<Integer, String> cf) {
			ConcurrentKafkaListenerContainerFactory<Integer, String> factory =
					new ConcurrentKafkaListenerContainerFactory<>();
			factory.setConsumerFactory(cf);
			return factory;
		}

		@Bean
		public SubscribableKafkaChannel ptp(KafkaTemplate<Integer, String> template,
				KafkaListenerContainerFactory<?> factory) {

			SubscribableKafkaChannel channel = new SubscribableKafkaChannel(template, factory, "channel.1");
			channel.setGroupId("channel.1");
			return channel;
		}

		@Bean
		public SubscribableKafkaChannel pubSub(KafkaTemplate<Integer, String> template,
				KafkaListenerContainerFactory<?> factory) {

			SubscribableKafkaChannel channel = new PublishSubscribeKafkaChannel(template, factory, "channel.2");
			channel.setGroupId("channel.2");
			return channel;
		}

		@Bean
		public KafkaMessageSource<Integer, String> source(ConsumerFactory<Integer, String> cf) {
			return new KafkaMessageSource<>(cf, new ConsumerProperties("channel.3"));
		}

		@Bean
		public PollableKafkaChannel pollable(KafkaTemplate<Integer, String> template, KafkaMessageSource<?, ?> source) {
			return new PollableKafkaChannel(template, source);
		}

	}

}
