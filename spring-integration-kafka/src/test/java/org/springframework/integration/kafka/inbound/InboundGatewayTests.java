/*
 * Copyright 2018-2020 the original author or authors.
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
import static org.mockito.Mockito.mock;
import static org.springframework.kafka.test.assertj.KafkaConditions.partition;
import static org.springframework.kafka.test.assertj.KafkaConditions.value;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.kafka.support.RawRecordHeaderErrorMessageStrategy;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.event.ConsumerPausedEvent;
import org.springframework.kafka.event.ConsumerResumedEvent;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Urs Keller
 *
 * @since 5.4
 *
 */
class InboundGatewayTests {

	private static String topic1 = "testTopic1";

	private static String topic2 = "testTopic2";

	private static String topic3 = "testTopic3";

	private static String topic4 = "testTopic4";

	private static String topic5 = "testTopic5";

	private static String topic6 = "testTopic6";

	private static String topic7 = "testTopic7";

	private static EmbeddedKafkaBroker embeddedKafka;

	@BeforeAll
	static void setup() {
		embeddedKafka = new EmbeddedKafkaBroker(1, true,
				topic1, topic2, topic3, topic4, topic5, topic6, topic7);
		embeddedKafka.afterPropertiesSet();
	}

	@AfterAll
	static void tearDown() {
		embeddedKafka.destroy();
	}

	@Test
	void testInbound() throws Exception {
		Map<String, Object> consumerProps =
				KafkaTestUtils.consumerProps("replyHandler1", "false", embeddedKafka);
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		ConsumerFactory<Integer, String> cf2 = new DefaultKafkaConsumerFactory<>(consumerProps);
		Consumer<Integer, String> consumer = cf2.createConsumer();
		embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic2);

		Map<String, Object> props = KafkaTestUtils.consumerProps("test1", "false", embeddedKafka);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(props);
		ContainerProperties containerProps = new ContainerProperties(topic1);
		containerProps.setIdleEventInterval(100L);
		containerProps.setPollTimeout(200L);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		CountDownLatch pausedLatch = new CountDownLatch(1);
		CountDownLatch resumedLatch = new CountDownLatch(1);
		container.setApplicationEventPublisher(event -> {
			if (event instanceof ConsumerPausedEvent) {
				pausedLatch.countDown();
			}
			else if (event instanceof ConsumerResumedEvent) {
				resumedLatch.countDown();
			}
		});
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic1);
		KafkaInboundGateway<Integer, String, String> gateway = new KafkaInboundGateway<>(container, template);
		QueueChannel out = new QueueChannel();
		DirectChannel reply = new DirectChannel();
		gateway.setRequestChannel(out);
		gateway.setReplyChannel(reply);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setReplyTimeout(30_000);
		gateway.afterPropertiesSet();
		gateway.setMessageConverter(new MessagingMessageConverter() {

			@Override
			public Message<?> toMessage(ConsumerRecord<?, ?> record, Acknowledgment acknowledgment,
					Consumer<?, ?> con, Type type) {

				Message<?> message = super.toMessage(record, acknowledgment, con, type);
				return MessageBuilder.fromMessage(message)
						.setHeader("testHeader", "testValue")
						.setHeader(KafkaHeaders.REPLY_TOPIC, topic2)
						.setHeader(KafkaHeaders.REPLY_PARTITION, 1)
						.build();
			}

		});

		CountDownLatch onPartitionsAssignedCalledLatch = new CountDownLatch(1);

		gateway.setOnPartitionsAssignedSeekCallback((map, seekConsumer) -> onPartitionsAssignedCalledLatch.countDown());

		gateway.start();
		ContainerTestUtils.waitForAssignment(container, 2);

		template.sendDefault(0, 1487694048607L, 1, "foo");
		Message<?> received = out.receive(30_000);
		assertThat(received).isNotNull();

		MessageHeaders headers = received.getHeaders();
		assertThat(headers.get(KafkaHeaders.RECEIVED_MESSAGE_KEY)).isEqualTo(1);
		assertThat(headers.get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(topic1);
		assertThat(headers.get(KafkaHeaders.RECEIVED_PARTITION_ID)).isEqualTo(0);
		assertThat(headers.get(KafkaHeaders.OFFSET)).isEqualTo(0L);
		assertThat(headers.get(KafkaHeaders.RECEIVED_TIMESTAMP)).isEqualTo(1487694048607L);
		assertThat(headers.get(KafkaHeaders.TIMESTAMP_TYPE)).isEqualTo("CREATE_TIME");
		assertThat(headers.get(KafkaHeaders.REPLY_TOPIC)).isEqualTo(topic2);
		assertThat(headers.get("testHeader")).isEqualTo("testValue");

		reply.send(MessageBuilder.withPayload("FOO").copyHeaders(headers).build());

		ConsumerRecord<Integer, String> record = KafkaTestUtils.getSingleRecord(consumer, topic2);
		assertThat(record).has(partition(1));
		assertThat(record).has(value("FOO"));
		assertThat(onPartitionsAssignedCalledLatch.await(10, TimeUnit.SECONDS)).isTrue();
		gateway.pause();
		assertThat(pausedLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(gateway.isPaused()).isTrue();
		gateway.resume();
		assertThat(resumedLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(gateway.isPaused()).isFalse();

		gateway.stop();
	}

	@Test
	void testInboundErrorRecover() {
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("replyHandler2", "false", embeddedKafka);
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		ConsumerFactory<Integer, String> cf2 = new DefaultKafkaConsumerFactory<>(consumerProps);
		Consumer<Integer, String> consumer = cf2.createConsumer();
		embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic4);

		Map<String, Object> props = KafkaTestUtils.consumerProps("test2", "false", embeddedKafka);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(props);
		ContainerProperties containerProps = new ContainerProperties(topic3);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic3);
		KafkaInboundGateway<Integer, String, String> gateway = new KafkaInboundGateway<>(container, template);
		MessageChannel out = new DirectChannel() {

			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				throw new RuntimeException("intended");
			}

		};
		QueueChannel errors = new QueueChannel();
		gateway.setRequestChannel(out);
		gateway.setErrorChannel(errors);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setMessageConverter(new MessagingMessageConverter() {

			@Override
			public Message<?> toMessage(ConsumerRecord<?, ?> record, Acknowledgment acknowledgment,
					Consumer<?, ?> con, Type type) {
				Message<?> message = super.toMessage(record, acknowledgment, con, type);
				return MessageBuilder.fromMessage(message)
						.setHeader("testHeader", "testValue")
						.setHeader(KafkaHeaders.REPLY_TOPIC, topic4)
						.setHeader(KafkaHeaders.REPLY_PARTITION, 1)
						.build();
			}

		});
		gateway.setReplyTimeout(30_000);
		gateway.setBindSourceRecord(true);
		gateway.afterPropertiesSet();
		gateway.start();
		ContainerTestUtils.waitForAssignment(container, 2);

		template.sendDefault(0, 1487694048607L, 1, "foo");
		ErrorMessage em = (ErrorMessage) errors.receive(30_000);
		assertThat(em).isNotNull();
		assertThat(em.getHeaders().get(KafkaHeaders.RAW_DATA)).isNotNull();
		Message<?> failed = ((MessagingException) em.getPayload()).getFailedMessage();
		assertThat(failed).isNotNull();
		assertThat(failed.getHeaders().get(IntegrationMessageHeaderAccessor.SOURCE_DATA))
				.isSameAs(em.getHeaders().get(KafkaHeaders.RAW_DATA));
		MessageChannel reply = (MessageChannel) em.getHeaders().getReplyChannel();
		MessageHeaders headers = failed.getHeaders();
		reply.send(MessageBuilder.withPayload("ERROR").copyHeaders(headers).build());

		assertThat(headers.get(KafkaHeaders.RECEIVED_MESSAGE_KEY)).isEqualTo(1);
		assertThat(headers.get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(topic3);
		assertThat(headers.get(KafkaHeaders.RECEIVED_PARTITION_ID)).isEqualTo(0);
		assertThat(headers.get(KafkaHeaders.OFFSET)).isEqualTo(0L);
		assertThat(headers.get(KafkaHeaders.RECEIVED_TIMESTAMP)).isEqualTo(1487694048607L);
		assertThat(headers.get(KafkaHeaders.TIMESTAMP_TYPE)).isEqualTo("CREATE_TIME");
		assertThat(headers.get(KafkaHeaders.REPLY_TOPIC)).isEqualTo(topic4);
		assertThat(headers.get("testHeader")).isEqualTo("testValue");

		ConsumerRecord<Integer, String> record = KafkaTestUtils.getSingleRecord(consumer, topic4);
		assertThat(record).has(partition(1));
		assertThat(record).has(value("ERROR"));

		gateway.stop();
	}

	@Test
	void testInboundRetryErrorRecover() {
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("replyHandler3", "false", embeddedKafka);
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		ConsumerFactory<Integer, String> cf2 = new DefaultKafkaConsumerFactory<>(consumerProps);
		Consumer<Integer, String> consumer = cf2.createConsumer();
		embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic6);

		Map<String, Object> props = KafkaTestUtils.consumerProps("test3", "false", embeddedKafka);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(props);
		ContainerProperties containerProps = new ContainerProperties(topic5);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic5);
		KafkaInboundGateway<Integer, String, String> gateway = new KafkaInboundGateway<>(container, template);
		MessageChannel out = new DirectChannel() {

			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				throw new RuntimeException("intended");
			}

		};
		QueueChannel errors = new QueueChannel();
		gateway.setRequestChannel(out);
		gateway.setErrorChannel(errors);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setMessageConverter(new MessagingMessageConverter() {

			@Override
			public Message<?> toMessage(ConsumerRecord<?, ?> record, Acknowledgment acknowledgment,
					Consumer<?, ?> con, Type type) {
				Message<?> message = super.toMessage(record, acknowledgment, con, type);
				return MessageBuilder.fromMessage(message)
						.setHeader("testHeader", "testValue")
						.setHeader(KafkaHeaders.REPLY_TOPIC, topic6)
						.setHeader(KafkaHeaders.REPLY_PARTITION, 1)
						.build();
			}

		});
		gateway.setReplyTimeout(30_000);
		RetryTemplate retryTemplate = new RetryTemplate();
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts(2);
		retryTemplate.setRetryPolicy(retryPolicy);
		retryTemplate.setBackOffPolicy(new NoBackOffPolicy());
		gateway.setRetryTemplate(retryTemplate);
		gateway.setRecoveryCallback(
				new ErrorMessageSendingRecoverer(errors, new RawRecordHeaderErrorMessageStrategy()));
		gateway.afterPropertiesSet();
		gateway.start();
		ContainerTestUtils.waitForAssignment(container, 2);

		template.sendDefault(0, 1487694048607L, 1, "foo");
		ErrorMessage em = (ErrorMessage) errors.receive(30_000);
		assertThat(em).isNotNull();
		Message<?> failed = ((MessagingException) em.getPayload()).getFailedMessage();
		assertThat(failed).isNotNull();
		assertThat(failed.getHeaders().get(IntegrationMessageHeaderAccessor.SOURCE_DATA)).isNull();
		MessageChannel reply = (MessageChannel) em.getHeaders().getReplyChannel();
		MessageHeaders headers = failed.getHeaders();
		reply.send(MessageBuilder.withPayload("ERROR").copyHeaders(headers).build());

		assertThat(headers.get(KafkaHeaders.RECEIVED_MESSAGE_KEY)).isEqualTo(1);
		assertThat(headers.get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(topic5);
		assertThat(headers.get(KafkaHeaders.RECEIVED_PARTITION_ID)).isEqualTo(0);
		assertThat(headers.get(KafkaHeaders.OFFSET)).isEqualTo(0L);
		assertThat(headers.get(KafkaHeaders.RECEIVED_TIMESTAMP)).isEqualTo(1487694048607L);
		assertThat(headers.get(KafkaHeaders.TIMESTAMP_TYPE)).isEqualTo("CREATE_TIME");
		assertThat(headers.get(KafkaHeaders.REPLY_TOPIC)).isEqualTo(topic6);
		assertThat(headers.get("testHeader")).isEqualTo("testValue");

		ConsumerRecord<Integer, String> record = KafkaTestUtils.getSingleRecord(consumer, topic6);
		assertThat(record).has(partition(1));
		assertThat(record).has(value("ERROR"));

		gateway.stop();
	}

	@Test
	void testInboundRetryErrorRecoverWithoutRecocveryCallback() throws Exception {
		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("replyHandler4", "false", embeddedKafka);
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		ConsumerFactory<Integer, String> cf2 = new DefaultKafkaConsumerFactory<>(consumerProps);
		Consumer<Integer, String> consumer = cf2.createConsumer();
		embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic7);

		Map<String, Object> props = KafkaTestUtils.consumerProps("test4", "false", embeddedKafka);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(props);
		ContainerProperties containerProps = new ContainerProperties(topic7);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic7);
		KafkaInboundGateway<Integer, String, String> gateway = new KafkaInboundGateway<>(container, template);
		MessageChannel out = new DirectChannel() {

			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				throw new RuntimeException("intended");
			}

		};
		gateway.setRequestChannel(out);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setReplyTimeout(30_000);
		RetryTemplate retryTemplate = new RetryTemplate();
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts(5);
		retryTemplate.setRetryPolicy(retryPolicy);
		retryTemplate.setBackOffPolicy(new NoBackOffPolicy());
		final CountDownLatch retryCountLatch = new CountDownLatch(retryPolicy.getMaxAttempts());
		retryTemplate.registerListener(new RetryListenerSupport() {

			@Override
			public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
					Throwable throwable) {
				retryCountLatch.countDown();
			}
		});
		gateway.setRetryTemplate(retryTemplate);
		gateway.afterPropertiesSet();
		gateway.start();
		ContainerTestUtils.waitForAssignment(container, 2);

		template.sendDefault(0, 1487694048607L, 1, "foo");

		assertThat(retryCountLatch.await(10, TimeUnit.SECONDS)).isTrue();

		gateway.stop();
		consumer.close();
	}

}
