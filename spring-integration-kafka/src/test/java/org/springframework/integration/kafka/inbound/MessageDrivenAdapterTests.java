/*
 * Copyright 2016-present the original author or authors.
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

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter.ListenerMode;
import org.springframework.integration.kafka.support.RawRecordHeaderErrorMessageStrategy;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.event.ConsumerPausedEvent;
import org.springframework.kafka.event.ConsumerResumedEvent;
import org.springframework.kafka.event.KafkaEvent;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.JsonKafkaHeaderMapper;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.KafkaNull;
import org.springframework.kafka.support.TopicPartitionOffset;
import org.springframework.kafka.support.converter.BatchMessageConverter;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Biju Kunjummen
 * @author Cameron Mayfield
 * @author Urs Keller
 * @author Jooyoung Pyoung
 *
 * @since 5.4
 *
 */
@EmbeddedKafka(controlledShutdown = true,
		partitions = 1,
		topics = {MessageDrivenAdapterTests.topic1,
				MessageDrivenAdapterTests.topic2,
				MessageDrivenAdapterTests.topic3,
				MessageDrivenAdapterTests.topic4,
				MessageDrivenAdapterTests.topic5,
				MessageDrivenAdapterTests.topic6})
class MessageDrivenAdapterTests implements TestApplicationContextAware {

	static final String topic1 = "testTopic1";

	static final String topic2 = "testTopic2";

	static final String topic3 = "testTopic3";

	static final String topic4 = "testTopic4";

	static final String topic5 = "testTopic5";

	static final String topic6 = "testTopic6";

	@Test
	void testInboundRecord(EmbeddedKafkaBroker embeddedKafka) {
		Map<String, Object> props = KafkaTestUtils.consumerProps(embeddedKafka, "test1", true);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(props);
		ContainerProperties containerProps = new ContainerProperties(topic1);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		KafkaMessageDrivenChannelAdapter<Integer, String> adapter = new KafkaMessageDrivenChannelAdapter<>(container);
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.setRecordMessageConverter(new MessagingMessageConverter() {

			@Override
			public Message<?> toMessage(ConsumerRecord<?, ?> record, Object acknowledgment, Object consumer, Type type) {
				Message<?> message = super.toMessage(record, acknowledgment, consumer, type);
				return MessageBuilder.fromMessage(message).setHeader("testHeader", "testValue").build();
			}

		});
		adapter.start();
		ContainerTestUtils.waitForAssignment(container, 1);

		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic1);
		template.sendDefault(0, 1487694048607L, 1, "foo");

		Message<?> received = out.receive(10000);
		assertThat(received).isNotNull();

		MessageHeaders headers = received.getHeaders();
		assertThat(headers)
				.containsEntry(KafkaHeaders.RECEIVED_KEY, 1)
				.containsEntry(KafkaHeaders.RECEIVED_TOPIC, topic1)
				.containsEntry(KafkaHeaders.RECEIVED_PARTITION, 0)
				.containsEntry(KafkaHeaders.OFFSET, 0L)
				.containsEntry(KafkaHeaders.RECEIVED_TIMESTAMP, 1487694048607L)
				.containsEntry(KafkaHeaders.TIMESTAMP_TYPE, "CREATE_TIME")
				.containsKeys(MessageHeaders.TIMESTAMP, MessageHeaders.ID);

		assertThat(headers.get("testHeader")).isEqualTo("testValue");

		template.sendDefault(1, null);

		received = out.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isInstanceOf(KafkaNull.class);

		headers = received.getHeaders();
		assertThat(headers)
				.containsEntry(KafkaHeaders.RECEIVED_KEY, 1)
				.containsEntry(KafkaHeaders.RECEIVED_TOPIC, topic1)
				.containsEntry(KafkaHeaders.RECEIVED_PARTITION, 0)
				.containsEntry(KafkaHeaders.OFFSET, 1L)
				.containsEntry(KafkaHeaders.TIMESTAMP_TYPE, "CREATE_TIME")
				.containsEntry("testHeader", "testValue");

		adapter.setMessageConverter(new RecordMessageConverter() {

			@Override
			public Message<?> toMessage(ConsumerRecord<?, ?> record, Object acknowledgment, Object con, Type type) {
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
				.contains("Failed to convert to message");
		assertThat(((ConversionException) error.getPayload()).getRecord()).isNotNull();

		adapter.stop();
		pf.reset();
	}

	@Test
	void testInboundRecordRetryRecover(EmbeddedKafkaBroker embeddedKafka) {
		Map<String, Object> props = KafkaTestUtils.consumerProps(embeddedKafka, "test4", true);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(props);
		ContainerProperties containerProps = new ContainerProperties(topic4);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		KafkaMessageDrivenChannelAdapter<Integer, String> adapter = new KafkaMessageDrivenChannelAdapter<>(container);
		AtomicReference<MessageHistory> receivedMessageHistory = new AtomicReference<>();
		MessageChannel out = new DirectChannel() {

			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				receivedMessageHistory.set(MessageHistory.read(message));
				throw new RuntimeException("intended");
			}

		};
		adapter.setOutputChannel(out);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setRecoveryCallback(
				new ErrorMessageSendingRecoverer(errorChannel, new RawRecordHeaderErrorMessageStrategy()));
		adapter.setRetryTemplate(new RetryTemplate(RetryPolicy.builder().maxRetries(2).delay(Duration.ZERO).build()));
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();
		ContainerTestUtils.waitForAssignment(container, 1);

		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic4);
		Message<?> msg = MessageBuilder.withPayload("foo").setHeader(KafkaHeaders.KEY, 1).build();
		NullChannel component = new NullChannel();
		component.setBeanName("myNullChannel");
		msg = MessageHistory.write(msg, component);
		template.send(msg);

		Message<?> received = errorChannel.receive(10000);
		assertThat(received).isInstanceOf(ErrorMessage.class);
		MessageHeaders headers = received.getHeaders();
		assertThat(headers.get(KafkaHeaders.RAW_DATA)).isNotNull();
		Message<?> originalMessage = ((ErrorMessage) received).getOriginalMessage();
		assertThat(originalMessage).isNotNull();
		assertThat(originalMessage.getHeaders().get(IntegrationMessageHeaderAccessor.SOURCE_DATA)).isNull();
		headers = originalMessage.getHeaders();
		assertThat(headers)
				.containsEntry(KafkaHeaders.RECEIVED_KEY, 1)
				.containsEntry(KafkaHeaders.RECEIVED_TOPIC, topic4)
				.containsEntry(KafkaHeaders.RECEIVED_PARTITION, 0)
				.containsEntry(KafkaHeaders.OFFSET, 0L);

		assertThat(StaticMessageHeaderAccessor.getDeliveryAttempt(originalMessage).get()).isEqualTo(3);

		assertThat(receivedMessageHistory.get()).isNotNull();
		assertThat(receivedMessageHistory.get().toString()).isEqualTo("myNullChannel");

		adapter.stop();
		pf.reset();
	}

	/**
	 * the recovery callback is not mandatory, if not set and retries are exhausted the last throwable is rethrown
	 * to the consumer.
	 */
	@Test
	void testInboundRecordRetryRecoverWithoutRecoveryCallback(EmbeddedKafkaBroker embeddedKafka) throws Exception {
		Map<String, Object> props = KafkaTestUtils.consumerProps(embeddedKafka, "test6", true);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(props);
		ContainerProperties containerProps = new ContainerProperties(topic6);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);

		KafkaMessageDrivenChannelAdapter<Integer, String> adapter = new KafkaMessageDrivenChannelAdapter<>(container);
		MessageChannel out = new DirectChannel() {

			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				throw new RuntimeException("intended");
			}

		};
		adapter.setOutputChannel(out);
		RetryTemplate retryTemplate = new RetryTemplate(RetryPolicy.builder().maxRetries(2).delay(Duration.ZERO).build());
		final CountDownLatch retryCountLatch = new CountDownLatch(3);
		retryTemplate.setRetryListener(new RetryListener() {

			@Override
			public void onRetryFailure(RetryPolicy retryPolicy, Retryable<?> retryable, Throwable throwable) {
				retryCountLatch.countDown();
			}

		});
		adapter.setRetryTemplate(retryTemplate);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();
		ContainerTestUtils.waitForAssignment(container, 1);

		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		DefaultKafkaProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic6);
		template.sendDefault(1, "foo");

		assertThat(retryCountLatch.await(10, TimeUnit.SECONDS)).isTrue();

		adapter.stop();
		pf.destroy();
	}

	@Test
	void testInboundRecordNoRetryRecover(EmbeddedKafkaBroker embeddedKafka) {
		Map<String, Object> props = KafkaTestUtils.consumerProps(embeddedKafka, "test5", true);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(props);
		ContainerProperties containerProps = new ContainerProperties(topic5);
		containerProps.setDeliveryAttemptHeader(true);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		container.setCommonErrorHandler(new DefaultErrorHandler());
		KafkaMessageDrivenChannelAdapter<Integer, String> adapter = new KafkaMessageDrivenChannelAdapter<>(container);
		MessageChannel out = new DirectChannel() {

			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				throw new RuntimeException("intended");
			}

		};
		adapter.setOutputChannel(out);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		adapter.setRecoveryCallback(
				new ErrorMessageSendingRecoverer(errorChannel, new RawRecordHeaderErrorMessageStrategy()));
		adapter.setBindSourceRecord(true);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();
		ContainerTestUtils.waitForAssignment(container, 1);

		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic5);
		template.sendDefault(1, "foo");

		Message<?> received = errorChannel.receive(10000);
		assertThat(received).isInstanceOf(ErrorMessage.class);
		MessageHeaders headers = received.getHeaders();
		assertThat(headers.get(KafkaHeaders.RAW_DATA)).isNotNull();
		assertThat(headers.get(IntegrationMessageHeaderAccessor.SOURCE_DATA))
				.isSameAs(headers.get(KafkaHeaders.RAW_DATA));
		Message<?> originalMessage = ((ErrorMessage) received).getOriginalMessage();
		assertThat(originalMessage).isNotNull();
		assertThat(originalMessage.getHeaders().get(IntegrationMessageHeaderAccessor.SOURCE_DATA))
				.isSameAs(headers.get(KafkaHeaders.RAW_DATA));
		headers = originalMessage.getHeaders();
		assertThat(headers)
				.containsEntry(KafkaHeaders.RECEIVED_KEY, 1)
				.containsEntry(KafkaHeaders.RECEIVED_TOPIC, topic5)
				.containsEntry(KafkaHeaders.RECEIVED_PARTITION, 0)
				.containsEntry(KafkaHeaders.OFFSET, 0L);
		assertThat(StaticMessageHeaderAccessor.getDeliveryAttempt(originalMessage).get()).isEqualTo(1);

		adapter.stop();
		pf.reset();
	}

	@Test
	void testInboundBatch(EmbeddedKafkaBroker embeddedKafka) throws Exception {
		Map<String, Object> props = KafkaTestUtils.consumerProps(embeddedKafka, "test2", true);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 60);
		props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 2000);

		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(props);
		ContainerProperties containerProps = new ContainerProperties(topic2);
		containerProps.setIdleEventInterval(100L);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		KafkaMessageDrivenChannelAdapter<Integer, String> adapter = new KafkaMessageDrivenChannelAdapter<>(container,
				ListenerMode.batch);
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);

		final CountDownLatch onPartitionsAssignedCalledLatch = new CountDownLatch(1);

		adapter.setOnPartitionsAssignedSeekCallback((map, consumer) -> onPartitionsAssignedCalledLatch.countDown());
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
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
		ContainerTestUtils.waitForAssignment(container, 1);

		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic2);
		template.sendDefault(0, 1487694048607L, 1, "foo");
		template.sendDefault(0, 1487694048608L, 1, "bar");

		Message<?> received = out.receive(10000);
		assertThat(received).isNotNull();
		Object payload = received.getPayload();
		assertThat(payload).asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(2);

		MessageHeaders headers = received.getHeaders();
		assertThat(headers.get(KafkaHeaders.RECEIVED_KEY)).isEqualTo(Arrays.asList(1, 1));
		assertThat(headers.get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(Arrays.asList("testTopic2", "testTopic2"));
		assertThat(headers.get(KafkaHeaders.RECEIVED_PARTITION)).isEqualTo(Arrays.asList(0, 0));
		assertThat(headers.get(KafkaHeaders.OFFSET)).isEqualTo(Arrays.asList(0L, 1L));
		assertThat(headers.get(KafkaHeaders.TIMESTAMP_TYPE))
				.isEqualTo(Arrays.asList("CREATE_TIME", "CREATE_TIME"));
		assertThat(headers.get(KafkaHeaders.RECEIVED_TIMESTAMP))
				.isEqualTo(Arrays.asList(1487694048607L, 1487694048608L));
		assertThat(headers.get("testHeader")).isEqualTo("testValue");

		assertThat(onPartitionsAssignedCalledLatch.await(10, TimeUnit.SECONDS)).isTrue();

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
		template.sendDefault(1, "foo");
		template.sendDefault(1, "bar");
		Message<?> error = errors.receive(10000);
		assertThat(error).isNotNull();
		assertThat(error.getPayload()).isInstanceOf(ConversionException.class);
		assertThat(((ConversionException) error.getPayload()).getMessage())
				.contains("Failed to convert to message");
		assertThat(((ConversionException) error.getPayload()).getRecords()).hasSize(2);

		adapter.stop();
		pf.reset();
	}

	@Test
	void testInboundJson(EmbeddedKafkaBroker embeddedKafka) {
		Map<String, Object> props = KafkaTestUtils.consumerProps(embeddedKafka, "test3", true);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(props);
		ContainerProperties containerProps = new ContainerProperties(topic3);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		KafkaMessageDrivenChannelAdapter<Integer, String> adapter = new KafkaMessageDrivenChannelAdapter<>(container);
		adapter.setRecordMessageConverter(new StringJacksonJsonMessageConverter());
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();
		ContainerTestUtils.waitForAssignment(container, 1);

		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic3);
		Headers kHeaders = new RecordHeaders();
		MessageHeaders siHeaders = new MessageHeaders(Collections.singletonMap("foo", "bar"));
		new JsonKafkaHeaderMapper().fromHeaders(siHeaders, kHeaders);
		template.send(new ProducerRecord<>(topic3, 0, 1487694048607L, 1, "{\"bar\":\"baz\"}", kHeaders));

		Message<?> received = out.receive(10000);
		assertThat(received).isNotNull();

		MessageHeaders headers = received.getHeaders();
		assertThat(headers)
				.containsEntry(KafkaHeaders.RECEIVED_KEY, 1)
				.containsEntry(KafkaHeaders.RECEIVED_TOPIC, topic3)
				.containsEntry(KafkaHeaders.RECEIVED_PARTITION, 0)
				.containsEntry(KafkaHeaders.OFFSET, 0L)
				.containsEntry(KafkaHeaders.RECEIVED_TIMESTAMP, 1487694048607L)
				.containsEntry(KafkaHeaders.TIMESTAMP_TYPE, "CREATE_TIME")
				.containsEntry("foo", "bar");

		assertThat(received.getPayload()).isInstanceOf(Map.class);

		adapter.stop();
		pf.reset();
	}

	@Test
	void testInboundJsonWithPayload(EmbeddedKafkaBroker embeddedKafka) {
		Map<String, Object> props = KafkaTestUtils.consumerProps(embeddedKafka, "test7", true);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, Foo> cf = new DefaultKafkaConsumerFactory<>(props);
		ContainerProperties containerProps = new ContainerProperties(topic6);
		KafkaMessageListenerContainer<Integer, Foo> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);

		KafkaMessageDrivenChannelAdapter<Integer, Foo> adapter = Kafka
				.messageDrivenChannelAdapter(container, ListenerMode.record)
				.recordMessageConverter(new StringJacksonJsonMessageConverter())
				.payloadType(Foo.class)
				.getObject();
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();
		ContainerTestUtils.waitForAssignment(container, 1);

		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic(topic6);
		Headers kHeaders = new RecordHeaders();
		MessageHeaders siHeaders = new MessageHeaders(Collections.singletonMap("foo", "bar"));
		new JsonKafkaHeaderMapper().fromHeaders(siHeaders, kHeaders);

		template.sendDefault(1, "{\"bar\":\"baz\"}");

		Message<?> received = out.receive(10000);
		assertThat(received).isNotNull();

		MessageHeaders headers = received.getHeaders();
		assertThat(headers.get(KafkaHeaders.RECEIVED_KEY)).isEqualTo(1);
		assertThat(headers.get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(topic6);
		assertThat(headers.get(KafkaHeaders.RECEIVED_PARTITION)).isEqualTo(0);
		assertThat(headers.get(KafkaHeaders.OFFSET)).isEqualTo(0L);
		assertThat((Long) headers.get(KafkaHeaders.RECEIVED_TIMESTAMP)).isGreaterThan(0L);
		assertThat(headers.get(KafkaHeaders.TIMESTAMP_TYPE)).isEqualTo("CREATE_TIME");

		assertThat(received.getPayload()).isInstanceOf(Foo.class);
		assertThat(received.getPayload()).isEqualTo(new Foo("baz"));

		adapter.stop();
		pf.reset();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Test
	void testPauseResume() throws Exception {
		ConsumerFactory<Integer, String> cf = mock();
		Consumer<Integer, String> consumer = mock();
		given(cf.createConsumer(eq("testPauseResumeGroup"), eq("clientId"), isNull(), any())).willReturn(consumer);
		final Map<TopicPartition, List<ConsumerRecord<Integer, String>>> records = new HashMap<>();
		records.put(new TopicPartition("foo", 0), Arrays.asList(
				new ConsumerRecord<>("foo", 0, 0L, 1, "foo"),
				new ConsumerRecord<>("foo", 0, 1L, 1, "bar")));
		ConsumerRecords<Integer, String> consumerRecords = new ConsumerRecords<>(records, Map.of());
		ConsumerRecords<Integer, String> emptyRecords = new ConsumerRecords<>(Collections.emptyMap(), Map.of());
		AtomicBoolean first = new AtomicBoolean(true);
		given(consumer.poll(any(Duration.class))).willAnswer(i -> {
			Thread.sleep(50);
			return first.getAndSet(false) ? consumerRecords : emptyRecords;
		});
		final CountDownLatch commitLatch = new CountDownLatch(2);
		willAnswer(i -> {
			commitLatch.countDown();
			return null;
		}).given(consumer).commitSync(anyMap(), any());
		given(consumer.assignment()).willReturn(records.keySet());
		given(consumer.paused()).willReturn(records.keySet());
		TopicPartitionOffset[] topicPartition = {new TopicPartitionOffset("foo", 0)};
		ContainerProperties containerProps = new ContainerProperties(topicPartition);
		containerProps.setAckMode(ContainerProperties.AckMode.RECORD);
		containerProps.setClientId("clientId");
		containerProps.setGroupId("testPauseResumeGroup");
		containerProps.setIdleEventInterval(100L);
		BlockingQueue<KafkaEvent> containerEvents = new LinkedBlockingQueue<>();
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		container.setApplicationEventPublisher(event -> {
			if (event instanceof ConsumerPausedEvent || event instanceof ConsumerResumedEvent) {
				containerEvents.offer((KafkaEvent) event);
			}
		});
		KafkaMessageDrivenChannelAdapter adapter = new KafkaMessageDrivenChannelAdapter(container);
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();
		assertThat(commitLatch.await(10, TimeUnit.SECONDS)).isTrue();
		verify(consumer, times(2)).commitSync(anyMap(), any());
		assertThat(outputChannel.getQueueSize()).isEqualTo(2);
		adapter.pause();
		await().until(containerEvents::take, ConsumerPausedEvent.class::isInstance);
		assertThat(adapter.isPaused()).isTrue();
		adapter.resume();
		await().until(containerEvents::take, ConsumerResumedEvent.class::isInstance);
		assertThat(adapter.isPaused()).isFalse();
		adapter.stop();
	}

	record Foo(String bar) {

	}

}
