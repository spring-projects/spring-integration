/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.integration.kafka.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter.ListenerMode;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.KafkaNull;
import org.springframework.kafka.support.converter.BatchMessageConverter;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;

/**
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Biju Kunjummen
 *
 * @since 2.0
 *
 */
public class MessageDrivenAdapterTests {

	private static String topic1 = "testTopic1";

	private static String topic2 = "testTopic2";

	private static String topic3 = "testTopic3";

	@ClassRule
	public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, topic1, topic2, topic3);

	@Test
	public void testInboundRecord() throws Exception {
		Map<String, Object> props = KafkaTestUtils.consumerProps("test1", "true", embeddedKafka);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<Integer, String>(props);
		ContainerProperties containerProps = new ContainerProperties(topic1);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		KafkaMessageDrivenChannelAdapter<Integer, String> adapter = new KafkaMessageDrivenChannelAdapter<>(container);
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.afterPropertiesSet();
		adapter.setRecordMessageConverter(new MessagingMessageConverter() {

			@Override
			public Message<?> toMessage(ConsumerRecord<?, ?> record, Acknowledgment acknowledgment,
					Consumer<?, ?> consumer, Type type) {
				Message<?> message = super.toMessage(record, acknowledgment, consumer, type);
				return MessageBuilder.fromMessage(message).setHeader("testHeader", "testValue").build();
			}

		});
		adapter.start();
		ContainerTestUtils.waitForAssignment(container, 2);

		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic1);
		template.sendDefault(0, 1487694048607L, 1, "foo");

		Message<?> received = out.receive(10000);
		assertThat(received).isNotNull();

		MessageHeaders headers = received.getHeaders();
		assertThat(headers.get(KafkaHeaders.RECEIVED_MESSAGE_KEY)).isEqualTo(1);
		assertThat(headers.get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(topic1);
		assertThat(headers.get(KafkaHeaders.RECEIVED_PARTITION_ID)).isEqualTo(0);
		assertThat(headers.get(KafkaHeaders.OFFSET)).isEqualTo(0L);
		assertThat(headers.get(KafkaHeaders.RECEIVED_TIMESTAMP)).isEqualTo(1487694048607L);
		assertThat(headers.get(KafkaHeaders.TIMESTAMP_TYPE)).isEqualTo("CREATE_TIME");

		assertThat(headers.get("testHeader")).isEqualTo("testValue");

		template.sendDefault(1, null);

		received = out.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isInstanceOf(KafkaNull.class);

		headers = received.getHeaders();
		assertThat(headers.get(KafkaHeaders.RECEIVED_MESSAGE_KEY)).isEqualTo(1);
		assertThat(headers.get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(topic1);
		assertThat(headers.get(KafkaHeaders.RECEIVED_PARTITION_ID)).isEqualTo(0);
		assertThat(headers.get(KafkaHeaders.OFFSET)).isEqualTo(1L);
		assertThat((Long) headers.get(KafkaHeaders.RECEIVED_TIMESTAMP)).isGreaterThan(0L);
		assertThat(headers.get(KafkaHeaders.TIMESTAMP_TYPE)).isEqualTo("CREATE_TIME");

		assertThat(headers.get("testHeader")).isEqualTo("testValue");

		adapter.setMessageConverter(new RecordMessageConverter() {

			@Override
			public Message<?> toMessage(ConsumerRecord<?, ?> record, Acknowledgment acknowledgment,
					Consumer<?, ?> consumer, Type type) {
				throw new RuntimeException("testError");
			}

			@Override
			public ProducerRecord<?, ?> fromMessage(Message<?> message, String defaultTopic) {
				return null;
			}
		});
		PollableChannel errors = new QueueChannel();
		adapter.setErrorChannel(errors);
		template.sendDefault(1, "bar");
		Message<?> error = errors.receive(10000);
		assertThat(error).isNotNull();
		assertThat(error.getPayload()).isInstanceOf(ConversionException.class);
		assertThat(((ConversionException) error.getPayload()).getMessage())
				.contains("Failed to convert to message for: ConsumerRecord(topic = testTopic1");

		adapter.stop();
	}

	@Test
	public void testInboundBatch() throws Exception {
		Map<String, Object> props = KafkaTestUtils.consumerProps("test2", "true", embeddedKafka);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<Integer, String>(props);
		ContainerProperties containerProps = new ContainerProperties(topic2);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		KafkaMessageDrivenChannelAdapter<Integer, String> adapter = new KafkaMessageDrivenChannelAdapter<>(container,
				ListenerMode.batch);
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.afterPropertiesSet();
		adapter.setBatchMessageConverter(new BatchMessagingMessageConverter() {

			@Override
			public Message<?> toMessage(List<ConsumerRecord<?, ?>> records, Acknowledgment acknowledgment,
					Consumer<?, ?> consumer, Type type) {
				Message<?> message = super.toMessage(records, acknowledgment, consumer, type);
				return MessageBuilder.fromMessage(message).setHeader("testHeader", "testValue").build();
			}

		});
		adapter.start();
		ContainerTestUtils.waitForAssignment(container, 2);

		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<Integer, String>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic2);
		template.sendDefault(0, 1487694048607L, 1, "foo");
		template.sendDefault(0, 1487694048608L, 1, "bar");

		Message<?> received = out.receive(10000);
		assertThat(received).isNotNull();
		Object payload = received.getPayload();
		assertThat(payload).isInstanceOf(List.class);
		List<?> list = (List<?>) payload;
		assertThat(list.size()).isGreaterThan(0);

		MessageHeaders headers = received.getHeaders();
		assertThat(headers.get(KafkaHeaders.RECEIVED_MESSAGE_KEY)).isEqualTo(Arrays.asList(1, 1));
		assertThat(headers.get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(Arrays.asList("testTopic2", "testTopic2"));
		assertThat(headers.get(KafkaHeaders.RECEIVED_PARTITION_ID)).isEqualTo(Arrays.asList(0, 0));
		assertThat(headers.get(KafkaHeaders.OFFSET)).isEqualTo(Arrays.asList(0L, 1L));
		assertThat(headers.get(KafkaHeaders.TIMESTAMP_TYPE))
				.isEqualTo(Arrays.asList("CREATE_TIME", "CREATE_TIME"));
		assertThat(headers.get(KafkaHeaders.RECEIVED_TIMESTAMP))
				.isEqualTo(Arrays.asList(1487694048607L, 1487694048608L));
		assertThat(headers.get("testHeader")).isEqualTo("testValue");

		adapter.setMessageConverter(new BatchMessageConverter() {

			@Override
			public Message<?> toMessage(List<ConsumerRecord<?, ?>> records, Acknowledgment acknowledgment,
					Consumer<?, ?> consumer, Type payloadType) {
				throw new RuntimeException("testError");
			}

			@Override
			public List<ProducerRecord<?, ?>> fromMessage(Message<?> message, String defaultTopic) {
				return null;
			}

		});
		PollableChannel errors = new QueueChannel();
		adapter.setErrorChannel(errors);
		template.sendDefault(1, "bar");
		Message<?> error = errors.receive(10000);
		assertThat(error).isNotNull();
		assertThat(error.getPayload()).isInstanceOf(ConversionException.class);
		assertThat(((ConversionException) error.getPayload()).getMessage())
				.contains("Failed to convert to message for: [ConsumerRecord(topic = testTopic2");

		adapter.stop();
	}

	@Test
	public void testInboundJson() throws Exception {
		Map<String, Object> props = KafkaTestUtils.consumerProps("test3", "true", embeddedKafka);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<Integer, String>(props);
		ContainerProperties containerProps = new ContainerProperties(topic3);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		KafkaMessageDrivenChannelAdapter<Integer, String> adapter = new KafkaMessageDrivenChannelAdapter<>(container);
		adapter.setRecordMessageConverter(new StringJsonMessageConverter());
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.afterPropertiesSet();
		adapter.start();
		ContainerTestUtils.waitForAssignment(container, 2);

		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<Integer, String>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic3);
		template.sendDefault(0, 1487694048607L, 1, "{\"bar\":\"baz\"}");

		Message<?> received = out.receive(10000);
		assertThat(received).isNotNull();

		MessageHeaders headers = received.getHeaders();
		assertThat(headers.get(KafkaHeaders.RECEIVED_MESSAGE_KEY)).isEqualTo(1);
		assertThat(headers.get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(topic3);
		assertThat(headers.get(KafkaHeaders.RECEIVED_PARTITION_ID)).isEqualTo(0);
		assertThat(headers.get(KafkaHeaders.OFFSET)).isEqualTo(0L);

		assertThat(headers.get(KafkaHeaders.RECEIVED_TIMESTAMP)).isEqualTo(1487694048607L);
		assertThat(headers.get(KafkaHeaders.TIMESTAMP_TYPE)).isEqualTo("CREATE_TIME");
		assertThat(received.getPayload()).isInstanceOf(Map.class);

		adapter.setPayloadType(Foo.class);
		template.sendDefault(1, "{\"bar\":\"baz\"}");

		received = out.receive(10000);
		assertThat(received).isNotNull();

		headers = received.getHeaders();
		assertThat(headers.get(KafkaHeaders.RECEIVED_MESSAGE_KEY)).isEqualTo(1);
		assertThat(headers.get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(topic3);
		assertThat(headers.get(KafkaHeaders.RECEIVED_PARTITION_ID)).isEqualTo(0);
		assertThat(headers.get(KafkaHeaders.OFFSET)).isEqualTo(1L);
		assertThat((Long) headers.get(KafkaHeaders.RECEIVED_TIMESTAMP)).isGreaterThan(0L);
		assertThat(headers.get(KafkaHeaders.TIMESTAMP_TYPE)).isEqualTo("CREATE_TIME");

		assertThat(received.getPayload()).isInstanceOf(Foo.class);
		assertThat(received.getPayload()).isEqualTo(new Foo("baz"));

		adapter.stop();
	}

	public static class Foo {

		private String bar;

		public Foo() {
		}

		public Foo(String bar) {
			this.bar = bar;
		}

		protected String getBar() {
			return this.bar;
		}

		protected void setBar(String bar) {
			this.bar = bar;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.bar == null) ? 0 : this.bar.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Foo other = (Foo) obj;
			if (this.bar == null) {
				if (other.bar != null) {
					return false;
				}
			}
			else if (!this.bar.equals(other.bar)) {
				return false;
			}
			return true;
		}

	}

}
