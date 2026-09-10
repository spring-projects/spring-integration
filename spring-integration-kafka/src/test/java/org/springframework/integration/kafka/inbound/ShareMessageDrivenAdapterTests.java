/*
 * Copyright 2026-present the original author or authors.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.consumer.AcknowledgeType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.kafka.core.DefaultShareConsumerFactory;
import org.springframework.kafka.core.ShareConsumerFactory;
import org.springframework.kafka.listener.AbstractShareKafkaMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.ShareKafkaMessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.ShareAcknowledgment;
import org.springframework.kafka.support.converter.ConversionException;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Artem Bilan
 *
 * @since 7.2
 */
@EmbeddedKafka(
		topics = {
				ShareMessageDrivenAdapterTests.EXPLICIT_TOPIC,
				ShareMessageDrivenAdapterTests.MANUAL_TOPIC,
				ShareMessageDrivenAdapterTests.MANUAL_ERROR_TOPIC,
				ShareMessageDrivenAdapterTests.CONVERSION_ERROR_TOPIC,
				ShareMessageDrivenAdapterTests.CONVERSION_ERROR_NO_CHANNEL_TOPIC,
				ShareMessageDrivenAdapterTests.FILTER_TOPIC,
				ShareMessageDrivenAdapterTests.PAYLOAD_TYPE_TOPIC
		},
		partitions = 1,
		brokerProperties = {
				"share.coordinator.state.topic.replication.factor=1",
				"share.coordinator.state.topic.min.isr=1"
		})
class ShareMessageDrivenAdapterTests implements TestApplicationContextAware {

	static final String EXPLICIT_TOPIC = "share-explicit-test";

	static final String MANUAL_TOPIC = "share-manual-ack-test";

	static final String MANUAL_ERROR_TOPIC = "share-manual-error-test";

	static final String CONVERSION_ERROR_TOPIC = "share-conversion-error-test";

	static final String CONVERSION_ERROR_NO_CHANNEL_TOPIC = "share-conversion-error-no-channel-test";

	static final String FILTER_TOPIC = "share-filter-test";

	static final String PAYLOAD_TYPE_TOPIC = "share-payload-type-test";

	private final List<KafkaShareMessageDrivenChannelAdapter<?, ?>> adapters = new ArrayList<>();

	/**
	 * Register an adapter for an unconditional stop in {@link #stopAdapters()}.
	 * The tests share a single embedded broker for the whole class, so an adapter left
	 * running by a failed test keeps re-polling that broker and fails the rest of them.
	 */
	private <K, V> KafkaShareMessageDrivenChannelAdapter<K, V> register(
			KafkaShareMessageDrivenChannelAdapter<K, V> adapter) {

		this.adapters.add(adapter);
		return adapter;
	}

	@AfterEach
	void stopAdapters() {
		this.adapters.forEach(KafkaShareMessageDrivenChannelAdapter::stop);
		this.adapters.clear();
	}

	@Test
	void testExplicitModeRecordDelivery(EmbeddedKafkaBroker embeddedKafka) throws Exception {
		String groupId = "share-explicit-group";
		setShareAutoOffsetResetEarliest(embeddedKafka, groupId);

		ShareConsumerFactory<String, String> consumerFactory = shareConsumerFactory(embeddedKafka, groupId);
		AbstractShareKafkaMessageListenerContainer<String, String> container =
				new ShareKafkaMessageListenerContainer<>(consumerFactory, new ContainerProperties(EXPLICIT_TOPIC));
		KafkaShareMessageDrivenChannelAdapter<String, String> adapter =
				register(new KafkaShareMessageDrivenChannelAdapter<>(container));
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();

		produceRecords(embeddedKafka, EXPLICIT_TOPIC, "key0", "value0", "key1", "value1");

		Message<?> received1 = out.receive(20000);
		Message<?> received2 = out.receive(20000);
		assertThat(received1).isNotNull();
		assertThat(received2).isNotNull();

		// KafkaHeaders.GROUP_ID is not asserted here: it is populated via a thread-bound value that
		// only the classic KafkaMessageListenerContainer sets (KafkaUtils.setConsumerGroupId());
		// ShareKafkaMessageListenerContainer does not, so the header is inherently absent.
		MessageHeaders headers = received1.getHeaders();
		assertThat(headers)
				.containsEntry(KafkaHeaders.RECEIVED_TOPIC, EXPLICIT_TOPIC)
				.containsEntry(KafkaHeaders.RECEIVED_PARTITION, 0)
				.containsKeys(MessageHeaders.ID, MessageHeaders.TIMESTAMP, KafkaHeaders.OFFSET)
				.doesNotContainKeys(KafkaHeaders.ACKNOWLEDGMENT, KafkaHeaders.CONSUMER);
	}

	@Test
	void testManualModeAcknowledgment(EmbeddedKafkaBroker embeddedKafka) throws Exception {
		String groupId = "share-manual-ack-group";
		setShareAutoOffsetResetEarliest(embeddedKafka, groupId);

		ContainerProperties containerProperties = new ContainerProperties(MANUAL_TOPIC);
		containerProperties.setShareAckMode(ContainerProperties.ShareAckMode.MANUAL);
		AbstractShareKafkaMessageListenerContainer<String, String> container =
				new ShareKafkaMessageListenerContainer<>(shareConsumerFactory(embeddedKafka, groupId),
						containerProperties);
		KafkaShareMessageDrivenChannelAdapter<String, String> adapter =
				register(new KafkaShareMessageDrivenChannelAdapter<>(container));
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();

		produceRecords(embeddedKafka, MANUAL_TOPIC, "key0", "value0", "key1", "value1", "key2", "value2");

		for (int i = 0; i < 3; i++) {
			Message<?> received = out.receive(20000);
			assertThat(received).isNotNull();
			Object consumer = received.getHeaders().get(KafkaHeaders.CONSUMER);
			assertThat(consumer).isNotNull();
			Object acknowledgment = received.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT);
			assertThat(acknowledgment).isInstanceOf(ShareAcknowledgment.class);
			((ShareAcknowledgment) acknowledgment).acknowledge();
		}
	}

	@Test
	void testManualModeErrorChannelOwnsAck(EmbeddedKafkaBroker embeddedKafka) throws Exception {
		String groupId = "share-manual-error-group";
		setShareAutoOffsetResetEarliest(embeddedKafka, groupId);

		ContainerProperties containerProperties = new ContainerProperties(MANUAL_ERROR_TOPIC);
		containerProperties.setShareAckMode(ContainerProperties.ShareAckMode.MANUAL);
		AbstractShareKafkaMessageListenerContainer<String, String> container =
				new ShareKafkaMessageListenerContainer<>(shareConsumerFactory(embeddedKafka, groupId),
						containerProperties);
		KafkaShareMessageDrivenChannelAdapter<String, String> adapter =
				register(new KafkaShareMessageDrivenChannelAdapter<>(container));
		QueueChannel out = new QueueChannel() {

			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				throw new RuntimeException("intended");
			}

		};
		adapter.setOutputChannel(out);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();

		produceRecords(embeddedKafka, MANUAL_ERROR_TOPIC, "key0", "value0", "key1", "value1");

		Message<?> errorMessage = errorChannel.receive(20000);
		assertThat(errorMessage).isInstanceOf(ErrorMessage.class);
		assertThat(errorMessage.getHeaders().get(KafkaHeaders.RAW_DATA)).isInstanceOf(ConsumerRecord.class);
		Message<?> originalMessage = ((ErrorMessage) errorMessage).getOriginalMessage();
		assertThat(originalMessage).isNotNull();
		Object acknowledgment = originalMessage.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT);
		assertThat(acknowledgment).isInstanceOf(ShareAcknowledgment.class);

		// The error channel subscriber owns the acknowledgment for the failed record.
		((ShareAcknowledgment) acknowledgment).reject();

		// A second record is still delivered, proving the poll loop was not stalled.
		Message<?> secondMessage = errorChannel.receive(20000);
		assertThat(secondMessage).isNotNull();
		Message<?> secondOriginal = ((ErrorMessage) secondMessage).getOriginalMessage();
		assertThat(secondOriginal).isNotNull();
		((ShareAcknowledgment) secondOriginal.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT)).reject();
	}

	@Test
	void testConversionErrorGoesToErrorChannel(EmbeddedKafkaBroker embeddedKafka) throws Exception {
		String groupId = "share-conversion-error-group";
		setShareAutoOffsetResetEarliest(embeddedKafka, groupId);

		AbstractShareKafkaMessageListenerContainer<String, String> container =
				new ShareKafkaMessageListenerContainer<>(shareConsumerFactory(embeddedKafka, groupId),
						new ContainerProperties(CONVERSION_ERROR_TOPIC));
		KafkaShareMessageDrivenChannelAdapter<String, String> adapter =
				register(new KafkaShareMessageDrivenChannelAdapter<>(container));
		adapter.setRecordMessageConverter(new RecordMessageConverter() {

			@Override
			public Message<?> toMessage(ConsumerRecord<?, ?> record, Object acknowledgment, Object consumer,
					Type type) {

				throw new RuntimeException("testError");
			}

			@Override
			public ProducerRecord<?, ?> fromMessage(Message<?> message, String defaultTopic) {
				return null;
			}

		});
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		PollableChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();

		produceRecords(embeddedKafka, CONVERSION_ERROR_TOPIC, "key0", "value0");

		Message<?> error = errorChannel.receive(20000);
		assertThat(error).isNotNull();
		assertThat(error.getPayload()).isInstanceOf(ConversionException.class);
		ConversionException conversionException = (ConversionException) error.getPayload();
		assertThat(conversionException.getMessage()).contains("Failed to convert to message");
		assertThat(conversionException.getRecord()).isNotNull();
	}

	@Test
	void testConversionErrorWithoutErrorChannelIsRejected(EmbeddedKafkaBroker embeddedKafka) throws Exception {
		String groupId = "share-conversion-error-no-channel-group";
		setShareAutoOffsetResetEarliest(embeddedKafka, groupId);

		AbstractShareKafkaMessageListenerContainer<String, String> container =
				new ShareKafkaMessageListenerContainer<>(shareConsumerFactory(embeddedKafka, groupId),
						new ContainerProperties(CONVERSION_ERROR_NO_CHANNEL_TOPIC));
		CountDownLatch recovererLatch = new CountDownLatch(1);
		AtomicReference<AcknowledgeType> recovererAction = new AtomicReference<>();
		container.setShareConsumerRecordRecoverer((record, ex) -> {
			recovererAction.set(AcknowledgeType.REJECT);
			recovererLatch.countDown();
			return AcknowledgeType.REJECT;
		});
		KafkaShareMessageDrivenChannelAdapter<String, String> adapter =
				register(new KafkaShareMessageDrivenChannelAdapter<>(container));
		adapter.setRecordMessageConverter(new RecordMessageConverter() {

			@Override
			public Message<?> toMessage(ConsumerRecord<?, ?> record, Object acknowledgment, Object consumer,
					Type type) {

				throw new RuntimeException("testError");
			}

			@Override
			public ProducerRecord<?, ?> fromMessage(Message<?> message, String defaultTopic) {
				return null;
			}

		});
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();

		produceRecords(embeddedKafka, CONVERSION_ERROR_NO_CHANNEL_TOPIC, "key0", "value0");

		assertThat(recovererLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(recovererAction.get()).isEqualTo(AcknowledgeType.REJECT);
	}

	@Test
	void testRecordFilterStrategyAndBindSourceRecord(EmbeddedKafkaBroker embeddedKafka) throws Exception {
		String groupId = "share-filter-group";
		setShareAutoOffsetResetEarliest(embeddedKafka, groupId);

		ContainerProperties containerProperties = new ContainerProperties(FILTER_TOPIC);
		containerProperties.setShareAckMode(ContainerProperties.ShareAckMode.MANUAL);
		AbstractShareKafkaMessageListenerContainer<String, String> container =
				new ShareKafkaMessageListenerContainer<>(shareConsumerFactory(embeddedKafka, groupId),
						containerProperties);
		KafkaShareMessageDrivenChannelAdapter<String, String> adapter =
				register(new KafkaShareMessageDrivenChannelAdapter<>(container));
		adapter.setRecordFilterStrategy(record -> Integer.parseInt(record.key()) % 2 != 0);
		adapter.setBindSourceRecord(true);
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();

		produceRecords(embeddedKafka, FILTER_TOPIC, "0", "v0", "1", "v1", "2", "v2", "3", "v3");

		for (int i = 0; i < 2; i++) {
			Message<?> received = out.receive(20000);
			assertThat(received).isNotNull();
			assertThat(received.getHeaders().get(KafkaHeaders.RECEIVED_KEY)).isIn("0", "2");
			assertThat(received.getHeaders().get(IntegrationMessageHeaderAccessor.SOURCE_DATA))
					.isInstanceOf(ConsumerRecord.class);
			Object acknowledgment = received.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT);
			((ShareAcknowledgment) acknowledgment).acknowledge();
		}
		assertThat(out.receive(1000)).isNull();
	}

	@Test
	void testPayloadType(EmbeddedKafkaBroker embeddedKafka) throws Exception {
		String groupId = "share-payload-type-group";
		setShareAutoOffsetResetEarliest(embeddedKafka, groupId);

		AbstractShareKafkaMessageListenerContainer<String, String> container =
				new ShareKafkaMessageListenerContainer<>(shareConsumerFactory(embeddedKafka, groupId),
						new ContainerProperties(PAYLOAD_TYPE_TOPIC));
		KafkaShareMessageDrivenChannelAdapter<String, String> adapter =
				register(new KafkaShareMessageDrivenChannelAdapter<>(container));
		adapter.setRecordMessageConverter(new StringJacksonJsonMessageConverter());
		adapter.setPayloadType(TestPayload.class);
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		adapter.start();

		produceRecords(embeddedKafka, PAYLOAD_TYPE_TOPIC, "key0", "{\"value\":\"test\"}");

		Message<?> received = out.receive(20000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo(new TestPayload("test"));
	}

	@Test
	void testConstructorRejectsContainerThatAlreadyHasAListener() {
		ShareConsumerFactory<String, String> consumerFactory = Mockito.mock();
		AbstractShareKafkaMessageListenerContainer<String, String> container =
				new ShareKafkaMessageListenerContainer<>(consumerFactory, new ContainerProperties(EXPLICIT_TOPIC));
		container.getContainerProperties().setMessageListener((MessageListener<String, String>) record -> {
		});

		assertThatIllegalArgumentException()
				.isThrownBy(() -> new KafkaShareMessageDrivenChannelAdapter<>(container))
				.withMessageContaining("Container must not already have a listener");
	}

	private static ShareConsumerFactory<String, String> shareConsumerFactory(EmbeddedKafkaBroker embeddedKafka,
			String groupId) {

		Map<String, Object> consumerProps = new HashMap<>();
		consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
		consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		return new DefaultShareConsumerFactory<>(consumerProps);
	}

	private static void produceRecords(EmbeddedKafkaBroker embeddedKafka, String topic, String... keysAndValues)
			throws Exception {

		Map<String, Object> producerProps = new HashMap<>();
		producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
		producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerProps.put(ProducerConfig.LINGER_MS_CONFIG, 0);
		try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
			for (int i = 0; i < keysAndValues.length; i += 2) {
				producer.send(new ProducerRecord<>(topic, keysAndValues[i], keysAndValues[i + 1])).get();
			}
		}
	}

	private static void setShareAutoOffsetResetEarliest(EmbeddedKafkaBroker embeddedKafka, String groupId)
			throws Exception {

		String bootstrapServers = embeddedKafka.getBrokersAsString();
		Map<String, Object> adminProperties = new HashMap<>();
		adminProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		ConfigEntry entry = new ConfigEntry("share.auto.offset.reset", "earliest");
		AlterConfigOp op = new AlterConfigOp(entry, AlterConfigOp.OpType.SET);
		Map<ConfigResource, Collection<AlterConfigOp>> configs =
				Map.of(new ConfigResource(ConfigResource.Type.GROUP, groupId), List.of(op));
		try (Admin admin = Admin.create(adminProperties)) {
			admin.incrementalAlterConfigs(configs).all().get();
		}
	}

	record TestPayload(String value) {

	}

}
