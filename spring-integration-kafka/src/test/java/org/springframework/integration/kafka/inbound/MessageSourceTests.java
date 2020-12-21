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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.log.LogMessage;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.ConsumerProperties;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.LogIfLevelEnabled.Level;
import org.springframework.kafka.support.TopicPartitionOffset;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;


/**
 * @author Gary Russell
 * @author Anshul Mehra
 * @author Artem Bilan
 *
 * @since 5.4
 *
 */
class MessageSourceTests {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testIllegalArgs() {
		ConsumerFactory consumerFactory = mock(ConsumerFactory.class);
		assertThatThrownBy(() -> new KafkaMessageSource(consumerFactory, new ConsumerProperties((Pattern) null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("topics, topicPattern, or topicPartitions must be provided");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void testConsumerAwareRebalanceListener() {
		Consumer consumer = mock(Consumer.class);
		TopicPartition topicPartition = new TopicPartition("foo", 0);
		List<TopicPartition> assigned = Collections.singletonList(topicPartition);
		AtomicReference<ConsumerRebalanceListener> listener = new AtomicReference<>();
		willAnswer(i -> {
			listener.set(i.getArgument(1));
			return null;
		}).given(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));

		ConsumerFactory consumerFactory = mock(ConsumerFactory.class);
		willReturn(Collections.singletonMap(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)).given(consumerFactory)
				.getConfigurationProperties();
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull(), any())).willReturn(consumer);
		ConsumerProperties consumerProperties = new ConsumerProperties("foo");
		AtomicBoolean partitionsAssignedCalled = new AtomicBoolean();
		AtomicReference<Consumer> partitionsAssignedConsumer = new AtomicReference<>();
		AtomicBoolean partitionsRevokedCalled = new AtomicBoolean();
		AtomicReference<Consumer> partitionsRevokedConsumer = new AtomicReference<>();
		consumerProperties.setConsumerRebalanceListener(new ConsumerAwareRebalanceListener() {

			@Override
			public void onPartitionsRevokedAfterCommit(Consumer<?, ?> cons, Collection<TopicPartition> partitions) {
				partitionsRevokedCalled.getAndSet(true);
				partitionsRevokedConsumer.set(cons);
			}

			@Override
			public void onPartitionsAssigned(Consumer<?, ?> cons, Collection<TopicPartition> partitions) {
				partitionsAssignedCalled.getAndSet(true);
				partitionsAssignedConsumer.set(cons);
			}

		});
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory, consumerProperties);
		source.setRawMessageHeader(true);

		source.receive();

		listener.get().onPartitionsAssigned(assigned);
		assertThat(partitionsAssignedCalled.get()).isTrue();
		assertThat(partitionsAssignedConsumer.get()).isEqualTo(consumer);

		listener.get().onPartitionsRevoked(assigned);
		assertThat(partitionsRevokedCalled.get()).isTrue();
		assertThat(partitionsRevokedConsumer.get()).isEqualTo(consumer);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void testRebalanceListener() {
		Consumer consumer = mock(Consumer.class);
		TopicPartition topicPartition1 = new TopicPartition("foo", 0);
		List<TopicPartition> assigned1 = new ArrayList<>(Collections.singletonList(topicPartition1));
		TopicPartition topicPartition2 = new TopicPartition("foo", 1);
		List<TopicPartition> assigned2 = new ArrayList<>(Collections.singletonList(topicPartition2));
		AtomicReference<ConsumerRebalanceListener> listener = new AtomicReference<>();
		willAnswer(i -> {
			listener.set(i.getArgument(1));
			return null;
		}).given(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));

		ConsumerFactory consumerFactory = mock(ConsumerFactory.class);
		willReturn(Collections.singletonMap(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)).given(consumerFactory)
				.getConfigurationProperties();
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull(), any())).willReturn(consumer);
		ConsumerProperties consumerProperties = new ConsumerProperties("foo");
		AtomicBoolean partitionsAssignedCalled = new AtomicBoolean();
		AtomicBoolean partitionsRevokedCalled = new AtomicBoolean();
		consumerProperties.setConsumerRebalanceListener(new ConsumerRebalanceListener() {

			@Override
			public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
				partitionsRevokedCalled.getAndSet(true);
			}

			@Override
			public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
				partitionsAssignedCalled.getAndSet(true);
			}

		});
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory, consumerProperties);
		source.setRawMessageHeader(true);

		source.receive();

		listener.get().onPartitionsAssigned(assigned1);
		assertThat(partitionsAssignedCalled.get()).isTrue();
		assertThat(new ArrayList<>(source.getAssignedPartitions())).isEqualTo(assigned1);
		listener.get().onPartitionsAssigned(assigned2);
		List<TopicPartition> temp = new ArrayList<>(assigned1);
		temp.addAll(assigned2);
		assertThat(new ArrayList<>(source.getAssignedPartitions())).isEqualTo(temp);

		listener.get().onPartitionsRevoked(assigned1);
		assertThat(partitionsRevokedCalled.get()).isTrue();
		assertThat(new ArrayList<>(source.getAssignedPartitions())).isEqualTo(assigned2);

		source.pause();
		assertThat(source.isPaused()).isFalse();
		InOrder inOrder = inOrder(consumer);
		source.receive();
		assertThat(source.isPaused()).isTrue();
		inOrder.verify(consumer).pause(new LinkedHashSet<>(assigned2));
		inOrder.verify(consumer).poll(any());
		listener.get().onPartitionsAssigned(assigned1);
		inOrder.verify(consumer).pause(new LinkedHashSet<>(temp));
	}

	@Test
	void testAckSyncCommits() {
		testAckCommon(true, false);
	}

	@Test
	void testAckSyncCommitsTimeout() {
		testAckCommon(true, false);
	}

	@Test
	void testAckAsyncCommits() {
		testAckCommon(false, false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void testAckCommon(boolean sync, boolean timeout) {
		Consumer consumer = mock(Consumer.class);
		TopicPartition topicPartition = new TopicPartition("foo", 0);
		List<TopicPartition> assigned = Collections.singletonList(topicPartition);
		willAnswer(i -> {
			((ConsumerRebalanceListener) i.getArgument(1))
					.onPartitionsAssigned(assigned);
			return null;
		}).given(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
		ArgumentCaptor<Collection<TopicPartition>> partitions = ArgumentCaptor.forClass(Collection.class);
		willDoNothing().given(consumer).pause(partitions.capture());
		willDoNothing().given(consumer).resume(partitions.capture());
		willAnswer(invoc -> {
			OffsetCommitCallback callback = invoc.getArgument(1);
			callback.onComplete(null, null);
			return null;
		}).given(consumer).commitAsync(any(), any());
		Map<TopicPartition, List<ConsumerRecord>> records1 = new LinkedHashMap<>();
		records1.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 0L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "foo")));
		Map<TopicPartition, List<ConsumerRecord>> records2 = new LinkedHashMap<>();
		records2.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 1L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "bar")));
		Map<TopicPartition, List<ConsumerRecord>> records3 = new LinkedHashMap<>();
		records3.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 2L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "baz")));
		Map<TopicPartition, List<ConsumerRecord>> records4 = new LinkedHashMap<>();
		records4.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 3L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "qux")));
		ConsumerRecords cr1 = new ConsumerRecords(records1);
		ConsumerRecords cr2 = new ConsumerRecords(records2);
		ConsumerRecords cr3 = new ConsumerRecords(records3);
		ConsumerRecords cr4 = new ConsumerRecords(records4);
		ConsumerRecords cr5 = new ConsumerRecords(Collections.emptyMap());
		given(consumer.poll(any(Duration.class))).willReturn(cr1, cr2, cr3, cr4, cr5);
		ConsumerFactory consumerFactory = mock(ConsumerFactory.class);
		willReturn(Collections.singletonMap(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)).given(consumerFactory)
				.getConfigurationProperties();
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull(), any())).willReturn(consumer);
		ConsumerProperties consumerProperties = new ConsumerProperties("foo");
		AtomicInteger callbackCount = new AtomicInteger();
		OffsetCommitCallback commitCallback = (offsets, ex) -> {
			callbackCount.incrementAndGet();
		};
		if (!sync) {
			consumerProperties.setSyncCommits(false);
			consumerProperties.setCommitCallback(commitCallback);
		}
		if (timeout) {
			consumerProperties.setSyncCommitTimeout(Duration.ofSeconds(5));
		}
		consumerProperties.setCommitLogLevel(Level.INFO);
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory, consumerProperties);
		source.setRawMessageHeader(true);

		Message<?> received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isInstanceOf(ConsumerRecord.class);
		assertThat(received.getHeaders().get(IntegrationMessageHeaderAccessor.SOURCE_DATA))
				.isSameAs(received.getHeaders().get(KafkaHeaders.RAW_DATA));
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received = source.receive();
		assertThat(received).isNotNull();
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received = source.receive();
		assertThat(received).isNotNull();
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received = source.receive();
		assertThat(received).isNotNull();
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received = source.receive();
		assertThat(received).isNull();
		source.pause();
		source.receive();
		source.resume();
		source.receive();
		source.destroy();
		InOrder inOrder = inOrder(consumer);
		inOrder.verify(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
		inOrder.verify(consumer).poll(any(Duration.class));
		checkCommit(sync, timeout, consumer, topicPartition, commitCallback, inOrder, 1L);
		inOrder.verify(consumer).poll(any(Duration.class));
		checkCommit(sync, timeout, consumer, topicPartition, commitCallback, inOrder, 2L);
		inOrder.verify(consumer).poll(any(Duration.class));
		checkCommit(sync, timeout, consumer, topicPartition, commitCallback, inOrder, 3L);
		inOrder.verify(consumer).poll(any(Duration.class));
		checkCommit(sync, timeout, consumer, topicPartition, commitCallback, inOrder, 4L);
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).pause(partitions.getAllValues().get(0));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).resume(partitions.getAllValues().get(1));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).close(any());
		inOrder.verifyNoMoreInteractions();
		if (!sync) {
			assertThat(callbackCount.get()).isEqualTo(4);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void checkCommit(boolean sync, boolean timeout, Consumer consumer, TopicPartition topicPartition,
			OffsetCommitCallback commitCallback, InOrder inOrder, long offset) {

		if (sync) {
			if (timeout) {
				inOrder.verify(consumer).commitSync(
						Collections.singletonMap(topicPartition, new OffsetAndMetadata(offset)),
						Duration.ofSeconds(5));
			}
			else {
				inOrder.verify(consumer)
						.commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(offset)));
			}
		}
		else {
			inOrder.verify(consumer).commitAsync(
					Collections.singletonMap(topicPartition, new OffsetAndMetadata(offset)),
					commitCallback);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testAckOutOfOrder() {
		Consumer consumer = mock(Consumer.class);
		TopicPartition topicPartition = new TopicPartition("foo", 0);
		willAnswer(i -> {
			((ConsumerRebalanceListener) i.getArgument(1))
					.onPartitionsAssigned(Collections.singletonList(topicPartition));
			return null;
		}).given(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
		AtomicReference<Set<TopicPartition>> paused = new AtomicReference<>(new HashSet<>());
		willAnswer(i -> {
			paused.set(new HashSet<>(i.getArgument(0)));
			return null;
		}).given(consumer).pause(anyCollection());
		willAnswer(i -> paused.get()).given(consumer).paused();
		Map<TopicPartition, List<ConsumerRecord>> records1 = new LinkedHashMap<>();
		records1.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 0L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "foo")));
		Map<TopicPartition, List<ConsumerRecord>> records2 = new LinkedHashMap<>();
		records2.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 1L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "bar")));
		Map<TopicPartition, List<ConsumerRecord>> records3 = new LinkedHashMap<>();
		records3.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 2L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "baz")));
		Map<TopicPartition, List<ConsumerRecord>> records4 = new LinkedHashMap<>();
		records4.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 3L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "qux")));
		Map<TopicPartition, List<ConsumerRecord>> records5 = new LinkedHashMap<>();
		records5.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 4L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "fiz")));
		Map<TopicPartition, List<ConsumerRecord>> records6 = new LinkedHashMap<>();
		records6.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 5L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "buz")));
		ConsumerRecords cr1 = new ConsumerRecords(records1);
		ConsumerRecords cr2 = new ConsumerRecords(records2);
		ConsumerRecords cr3 = new ConsumerRecords(records3);
		ConsumerRecords cr4 = new ConsumerRecords(records4);
		ConsumerRecords cr5 = new ConsumerRecords(records5);
		ConsumerRecords cr6 = new ConsumerRecords(records6);
		ConsumerRecords cr7 = new ConsumerRecords(Collections.emptyMap());
		given(consumer.poll(any(Duration.class))).willReturn(cr1, cr2, cr3, cr4, cr5, cr6, cr7);
		ConsumerFactory consumerFactory = mock(ConsumerFactory.class);
		willReturn(Collections.singletonMap(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)).given(consumerFactory)
				.getConfigurationProperties();
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull(), any())).willReturn(consumer);
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory, new ConsumerProperties("foo"));

		Message<?> received1 = source.receive();
		consumer.paused(); // need some other interaction with mock between polls for InOrder
		Message<?> received2 = source.receive();
		consumer.paused(); // need some other interaction with mock between polls for InOrder
		Message<?> received3 = source.receive();
		consumer.paused(); // need some other interaction with mock between polls for InOrder
		Message<?> received4 = source.receive();
		consumer.paused(); // need some other interaction with mock between polls for InOrder
		Message<?> received5 = source.receive();
		consumer.paused(); // need some other interaction with mock between polls for InOrder
		Message<?> received6 = source.receive();
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received3)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received2)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received5)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received1)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT); // should commit offset 3 (received 3)
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received6)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received4)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT); // should commit offset 6 (received 6).
		assertThat(source.receive()).isNull();
		source.destroy();
		InOrder inOrder = inOrder(consumer);
		inOrder.verify(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).paused();
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).paused();
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).paused();
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).paused();
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(3L)));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(6L)));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).close(any());
		inOrder.verifyNoMoreInteractions();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testNack() {
		Consumer consumer = mock(Consumer.class);
		TopicPartition topicPartition = new TopicPartition("foo", 0);
		willAnswer(i -> {
			((ConsumerRebalanceListener) i.getArgument(1))
					.onPartitionsAssigned(Collections.singletonList(topicPartition));
			return null;
		}).given(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
		AtomicReference<Set<TopicPartition>> paused = new AtomicReference<>(new HashSet<>());
		willAnswer(i -> {
			paused.set(new HashSet<>(i.getArgument(0)));
			return null;
		}).given(consumer).pause(anyCollection());
		willAnswer(i -> paused.get()).given(consumer).paused();
		Map<TopicPartition, List<ConsumerRecord>> records1 = new LinkedHashMap<>();
		records1.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 0L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "foo")));
		ConsumerRecords cr1 = new ConsumerRecords(records1);
		Map<TopicPartition, List<ConsumerRecord>> records2 = new LinkedHashMap<>();
		records2.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 1L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "bar")));
		ConsumerRecords cr2 = new ConsumerRecords(records2);
		ConsumerRecords cr3 = new ConsumerRecords(Collections.emptyMap());
		given(consumer.poll(any(Duration.class))).willReturn(cr1, cr1, cr2, cr2, cr3);
		ConsumerFactory consumerFactory = mock(ConsumerFactory.class);
		willReturn(Collections.singletonMap(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)).given(consumerFactory)
				.getConfigurationProperties();
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull(), any())).willReturn(consumer);
		ConsumerProperties consumerProperties = new ConsumerProperties("foo");
		consumerProperties.setSyncCommitTimeout(Duration.ofSeconds(30));
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory, consumerProperties);

		Message<?> received = source.receive();
		assertThat(received.getHeaders().get(KafkaHeaders.OFFSET)).isEqualTo(0L);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.REQUEUE);
		received = source.receive();
		assertThat(received.getHeaders().get(KafkaHeaders.OFFSET)).isEqualTo(0L);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received = source.receive();
		assertThat(received.getHeaders().get(KafkaHeaders.OFFSET)).isEqualTo(1L);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.REQUEUE);
		received = source.receive();
		assertThat(received.getHeaders().get(KafkaHeaders.OFFSET)).isEqualTo(1L);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received = source.receive();
		source.destroy();
		assertThat(received).isNull();
		InOrder inOrder = inOrder(consumer);
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).seek(topicPartition, 0L); // rollback
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(1L)),
				Duration.ofSeconds(30));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).seek(topicPartition, 1L); // rollback
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(2L)),
				Duration.ofSeconds(30));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).close(any());
		inOrder.verifyNoMoreInteractions();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testNackWithLaterInflight() {
		Consumer consumer = mock(Consumer.class);
		TopicPartition topicPartition = new TopicPartition("foo", 0);
		willAnswer(i -> {
			((ConsumerRebalanceListener) i.getArgument(1))
					.onPartitionsAssigned(Collections.singletonList(topicPartition));
			return null;
		}).given(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
		AtomicReference<Set<TopicPartition>> paused = new AtomicReference<>(new HashSet<>());
		willAnswer(i -> {
			paused.set(new HashSet<>(i.getArgument(0)));
			return null;
		}).given(consumer).pause(anyCollection());
		willAnswer(i -> paused.get()).given(consumer).paused();
		Map<TopicPartition, List<ConsumerRecord>> records1 = new LinkedHashMap<>();
		records1.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 0L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "foo")));
		ConsumerRecords cr1 = new ConsumerRecords(records1);
		Map<TopicPartition, List<ConsumerRecord>> records2 = new LinkedHashMap<>();
		records2.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 1L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "bar")));
		ConsumerRecords cr2 = new ConsumerRecords(records2);
		ConsumerRecords cr3 = new ConsumerRecords(Collections.emptyMap());
		given(consumer.poll(any(Duration.class))).willReturn(cr1, cr2, cr1, cr2, cr3);
		ConsumerFactory consumerFactory = mock(ConsumerFactory.class);
		willReturn(Collections.singletonMap(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)).given(consumerFactory)
				.getConfigurationProperties();
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull(), any())).willReturn(consumer);
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory, new ConsumerProperties("foo"));

		Message<?> received1 = source.receive();
		consumer.paused(); // need some other interaction with mock between polls for InOrder
		Message<?> received2 = source.receive(); // inflight
		assertThat(received1.getHeaders().get(KafkaHeaders.OFFSET)).isEqualTo(0L);
		AcknowledgmentCallback ack1 = StaticMessageHeaderAccessor.getAcknowledgmentCallback(received1);
		Log log1 = spy(KafkaTestUtils.getPropertyValue(ack1, "logger.log", Log.class));
		new DirectFieldAccessor(ack1).setPropertyValue("logger.log", log1);
		given(log1.isWarnEnabled()).willReturn(true);
		willDoNothing().given(log1).warn(any());
		ack1.acknowledge(AcknowledgmentCallback.Status.REQUEUE);
		assertThat(received2.getHeaders().get(KafkaHeaders.OFFSET)).isEqualTo(1L);
		AcknowledgmentCallback ack2 = StaticMessageHeaderAccessor.getAcknowledgmentCallback(received2);
		Log log2 = spy(KafkaTestUtils.getPropertyValue(ack1, "logger.log", Log.class));
		new DirectFieldAccessor(ack2).setPropertyValue("logger.log", log2);
		given(log2.isWarnEnabled()).willReturn(true);
		willDoNothing().given(log2).warn(any());
		ack2.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received1 = source.receive();
		assertThat(received1.getHeaders().get(KafkaHeaders.OFFSET)).isEqualTo(0L);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received1)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received2 = source.receive();
		assertThat(received2.getHeaders().get(KafkaHeaders.OFFSET)).isEqualTo(1L);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received2)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received1 = source.receive();
		source.destroy();
		assertThat(received1).isNull();
		InOrder inOrder = inOrder(consumer, log1, log2);
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).paused();
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).seek(topicPartition, 0L); // rollback
		inOrder.verify(log1).isWarnEnabled();
		ArgumentCaptor<LogMessage> captor = ArgumentCaptor.forClass(LogMessage.class);
		inOrder.verify(log1).warn(captor.capture());
		assertThat(captor.getValue().toString())
				.contains("Rolled back")
				.contains("later in-flight offsets [1] will also be re-fetched");
		inOrder.verify(log2).isWarnEnabled();
		captor = ArgumentCaptor.forClass(LogMessage.class);
		inOrder.verify(log2).warn(captor.capture());
		assertThat(captor.getValue().toString())
				.contains("Cannot commit offset for")
				.contains("; an earlier offset was rolled back");
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(1L)));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(2L)));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).close(any());
		inOrder.verifyNoMoreInteractions();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testMaxPollRecords() {
		KafkaMessageSource source = new KafkaMessageSource(new DefaultKafkaConsumerFactory<>(Collections.emptyMap()),
				new ConsumerProperties("topic"));
		assertThat((TestUtils.getPropertyValue(source, "consumerFactory.configs", Map.class)
				.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG))).isEqualTo(1);
		source = new KafkaMessageSource(new DefaultKafkaConsumerFactory<>(
				Collections.singletonMap(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 2)), new ConsumerProperties("topic"));
		assertThat((TestUtils.getPropertyValue(source, "consumerFactory.configs", Map.class)
				.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG))).isEqualTo(1);

		assertThatIllegalArgumentException()
				.isThrownBy(() ->
						new KafkaMessageSource((new DefaultKafkaConsumerFactory(Collections.emptyMap()) { }),
								new ConsumerProperties("topic")))
				.withMessageContaining(ConsumerConfig.MAX_POLL_RECORDS_CONFIG);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testPollTimeouts() {
		Consumer consumer = mock(Consumer.class);
		TopicPartition topicPartition = new TopicPartition("foo", 0);
		List<TopicPartition> assigned = Collections.singletonList(topicPartition);
		AtomicReference<ConsumerRebalanceListener> listener = new AtomicReference<>();
		willAnswer(i -> {
			listener.set(i.getArgument(1));
			return null;
		}).given(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));

		Map<TopicPartition, List<ConsumerRecord>> records1 = new LinkedHashMap<>();
		records1.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 0L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "foo")));
		ConsumerRecords cr1 = new ConsumerRecords(records1);
		given(consumer.poll(Duration.of(20 * 5000, ChronoUnit.MILLIS))).willReturn(cr1, ConsumerRecords.EMPTY);
		Map<TopicPartition, List<ConsumerRecord>> records2 = new LinkedHashMap<>();
		records2.put(topicPartition, Collections.singletonList(
				new ConsumerRecord("foo", 0, 1L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "foo")));
		ConsumerRecords cr2 = new ConsumerRecords(records2);
		given(consumer.poll(Duration.of(5000, ChronoUnit.MILLIS))).willReturn(cr2, ConsumerRecords.EMPTY);
		ConsumerFactory consumerFactory = mock(ConsumerFactory.class);
		willReturn(Collections.singletonMap(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)).given(consumerFactory)
				.getConfigurationProperties();
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull(), any())).willReturn(consumer);
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory, new ConsumerProperties("foo"));
		source.setRawMessageHeader(true);

		Message<?> received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isInstanceOf(ConsumerRecord.class);
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isEqualTo(cr1.records(topicPartition).get(0));
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);

		listener.get().onPartitionsAssigned(assigned);
		received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isInstanceOf(ConsumerRecord.class);
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isEqualTo(cr2.records(topicPartition).get(0));
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);

		listener.get().onPartitionsRevoked(assigned);
		received = source.receive();
		assertThat(received).isNull();

		InOrder inOrder = inOrder(consumer);
		inOrder.verify(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
		// assignTimeout used on initial poll (before partition assigned)
		inOrder.verify(consumer).poll(Duration.of(20 * 5000, ChronoUnit.MILLIS));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(1L)));
		// pollTimeout used on subsequent polls
		inOrder.verify(consumer).poll(Duration.of(5000, ChronoUnit.MILLIS));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(2L)));
		// assignTimeout used after partitions revoked
		inOrder.verify(consumer).poll(Duration.of(20 * 5000, ChronoUnit.MILLIS));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testAllowMulti() {
		Consumer consumer = mock(Consumer.class);
		TopicPartition topicPartition = new TopicPartition("foo", 0);
		List<TopicPartition> assigned = Collections.singletonList(topicPartition);
		willAnswer(i -> {
			((ConsumerRebalanceListener) i.getArgument(1))
					.onPartitionsAssigned(assigned);
			return null;
		}).given(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
		ArgumentCaptor<Collection<TopicPartition>> partitions = ArgumentCaptor.forClass(Collection.class);
		willDoNothing().given(consumer).pause(partitions.capture());
		willDoNothing().given(consumer).resume(partitions.capture());
		Map<TopicPartition, List<ConsumerRecord>> records = new LinkedHashMap<>();
		records.put(topicPartition, Arrays.asList(
				new ConsumerRecord("foo", 0, 0L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "foo"),
				new ConsumerRecord("foo", 0, 1L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "bar"),
				new ConsumerRecord("foo", 0, 2L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "baz"),
				new ConsumerRecord("foo", 0, 3L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "qux")));
		ConsumerRecords cr1 = new ConsumerRecords(records);
		ConsumerRecords cr2 = new ConsumerRecords(Collections.emptyMap());
		given(consumer.poll(any(Duration.class))).willReturn(cr1, cr2);
		ConsumerFactory consumerFactory = mock(ConsumerFactory.class);
		willReturn(Collections.singletonMap(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 4)).given(consumerFactory)
				.getConfigurationProperties();
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull(), any())).willReturn(consumer);
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory, new ConsumerProperties("foo"), true);
		source.setRawMessageHeader(true);

		Message<?> received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaMessageSource.REMAINING_RECORDS, Integer.class)).isEqualTo(3);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaMessageSource.REMAINING_RECORDS, Integer.class)).isEqualTo(2);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaMessageSource.REMAINING_RECORDS, Integer.class)).isEqualTo(1);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaMessageSource.REMAINING_RECORDS, Integer.class)).isEqualTo(0);
		StaticMessageHeaderAccessor.getAcknowledgmentCallback(received)
				.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		received = source.receive();
		assertThat(received).isNull();
		source.destroy();
		InOrder inOrder = inOrder(consumer);
		inOrder.verify(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(1L)));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(2L)));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(3L)));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(4L)));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).close(any());
		inOrder.verifyNoMoreInteractions();
	}

	@SuppressWarnings("unchecked")
	@Test
	void testTopicPatternBasedMessageSource() {
		MockConsumer<String, String> consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
		TopicPartition topicPartition1 = new TopicPartition("abc_foo", 0);
		TopicPartition topicPartition2 = new TopicPartition("abc_foo", 1);
		TopicPartition topicPartition3 = new TopicPartition("def_foo", 0);
		TopicPartition topicPartition4 = new TopicPartition("def_foo", 1);
		List<TopicPartition> topicPartitions = Arrays
				.asList(topicPartition1, topicPartition2, topicPartition3, topicPartition4);

		Map<TopicPartition, Long> beginningOffsets = topicPartitions.stream().collect(Collectors
				.toMap(Function.identity(), tp -> 0L));
		consumer.updateBeginningOffsets(beginningOffsets);

		ConsumerFactory<String, String> consumerFactory = mock(ConsumerFactory.class);
		willReturn(Collections.singletonMap(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)).given(consumerFactory)
				.getConfigurationProperties();
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull(), any())).willReturn(consumer);
		KafkaMessageSource<String, String> source = new KafkaMessageSource<>(consumerFactory,
				new ConsumerProperties(Pattern
						.compile("[a-zA-Z0-9_]*?foo")));
		source.setRawMessageHeader(true);
		source.start();
		// force consumer creation
		source.receive();

		consumer.rebalance(topicPartitions);

		ConsumerRecord<String, String> record1 = new ConsumerRecord<>("abc_foo", 0, 0, null, "a");
		ConsumerRecord<String, String> record2 = new ConsumerRecord<>("abc_foo", 0, 1, null, "b");
		ConsumerRecord<String, String> record3 = new ConsumerRecord<>("abc_foo", 1, 0, null, "c");
		ConsumerRecord<String, String> record4 = new ConsumerRecord<>("def_foo", 1, 0, null, "d");
		ConsumerRecord<String, String> record5 = new ConsumerRecord<>("def_foo", 0, 0, null, "e");
		Arrays.asList(record1, record2, record3, record4, record5)
				.forEach(consumer::addRecord);

		Message<?> received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isInstanceOf(ConsumerRecord.class);
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isEqualTo(record1);
		received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isInstanceOf(ConsumerRecord.class);
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isEqualTo(record2);
		received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isInstanceOf(ConsumerRecord.class);
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isEqualTo(record3);
		received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isInstanceOf(ConsumerRecord.class);
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isEqualTo(record4);
		received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isInstanceOf(ConsumerRecord.class);
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isEqualTo(record5);

		source.destroy();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void testStaticPartitionAssignment() {
		MockConsumer<String, String> consumer = spy(new MockConsumer<>(OffsetResetStrategy.EARLIEST));

		TopicPartition beginning = new TopicPartition("foo", 0);
		TopicPartition end = new TopicPartition("foo", 1);
		TopicPartition timestamp = new TopicPartition("foo", 2);
		TopicPartition negativeOffset = new TopicPartition("foo", 3);
		TopicPartition negativeRelativeToCurrent = new TopicPartition("foo", 4);
		TopicPartition positiveRelativeToCurrent = new TopicPartition("foo", 5);

		List<TopicPartition> topicPartitions = Arrays.asList(beginning, end, timestamp,
				negativeOffset, negativeRelativeToCurrent, positiveRelativeToCurrent);

		Map<TopicPartition, Long> beginningOffsets = topicPartitions.stream().collect(Collectors
				.toMap(Function.identity(), tp -> 0L));
		consumer.updateBeginningOffsets(beginningOffsets);
		Map<TopicPartition, Long> endOffsets = topicPartitions.stream().collect(Collectors
				.toMap(Function.identity(), tp -> 3L));
		consumer.updateEndOffsets(endOffsets);
		consumer.assign(topicPartitions);
		consumer.seek(timestamp, 1);
		consumer.seek(negativeRelativeToCurrent, 3);
		consumer.seek(positiveRelativeToCurrent, 1);

		ConsumerFactory consumerFactory = mock(ConsumerFactory.class);
		willReturn(Collections.singletonMap(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)).given(consumerFactory)
				.getConfigurationProperties();
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull(), any())).willReturn(consumer);

		TopicPartitionOffset beginningTpo = new TopicPartitionOffset(beginning, null,
				TopicPartitionOffset.SeekPosition.BEGINNING);
		TopicPartitionOffset endTpo = new TopicPartitionOffset(end, null, TopicPartitionOffset.SeekPosition.END);
		TopicPartitionOffset timestampTpo = new TopicPartitionOffset(timestamp, null,
				TopicPartitionOffset.SeekPosition.TIMESTAMP);
		TopicPartitionOffset negativeOffsetTpo = new TopicPartitionOffset(negativeOffset, -1L, null);
		TopicPartitionOffset negativeRelativeToCurrentTpo = new TopicPartitionOffset(negativeRelativeToCurrent.topic(),
				negativeRelativeToCurrent.partition(), -1L, true);
		TopicPartitionOffset positiveRelativeToCurrentTpo = new TopicPartitionOffset(positiveRelativeToCurrent.topic(),
				positiveRelativeToCurrent.partition(), 1L, true);
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory,
				new ConsumerProperties(beginningTpo, endTpo, timestampTpo,
						negativeOffsetTpo, negativeRelativeToCurrentTpo, positiveRelativeToCurrentTpo));
		source.setRawMessageHeader(true);

		source.createConsumer();

		ConsumerRecord<String, String> p0r0 = new ConsumerRecord<>("foo", 0, 0, null, "p0r0");
		ConsumerRecord<String, String> p0r1 = new ConsumerRecord<>("foo", 0, 1, null, "p0r1");
		ConsumerRecord<String, String> p0r2 = new ConsumerRecord<>("foo", 0, 2, null, "p0r2");
		ConsumerRecord<String, String> p0r3 = new ConsumerRecord<>("foo", 0, 3, null, "p0r3");
		ConsumerRecord<String, String> p1r0 = new ConsumerRecord<>("foo", 1, 0, null, "p1r0");
		ConsumerRecord<String, String> p1r1 = new ConsumerRecord<>("foo", 1, 1, null, "p1r1");
		ConsumerRecord<String, String> p1r2 = new ConsumerRecord<>("foo", 1, 2, null, "p1r2");
		ConsumerRecord<String, String> p1r3 = new ConsumerRecord<>("foo", 1, 3, null, "p1r3");
		ConsumerRecord<String, String> p2r0 = new ConsumerRecord<>("foo", 2, 0, null, "p2r0");
		ConsumerRecord<String, String> p2r1 = new ConsumerRecord<>("foo", 2, 1, null, "p2r1");
		ConsumerRecord<String, String> p2r2 = new ConsumerRecord<>("foo", 2, 2, null, "p2r2");
		ConsumerRecord<String, String> p2r3 = new ConsumerRecord<>("foo", 2, 3, null, "p2r3");
		ConsumerRecord<String, String> p3r0 = new ConsumerRecord<>("foo", 3, 0, null, "p3r0");
		ConsumerRecord<String, String> p3r1 = new ConsumerRecord<>("foo", 3, 1, null, "p3r1");
		ConsumerRecord<String, String> p3r2 = new ConsumerRecord<>("foo", 3, 2, null, "p3r2");
		ConsumerRecord<String, String> p3r3 = new ConsumerRecord<>("foo", 3, 3, null, "p3r3");
		ConsumerRecord<String, String> p4r0 = new ConsumerRecord<>("foo", 4, 0, null, "p4r0");
		ConsumerRecord<String, String> p4r1 = new ConsumerRecord<>("foo", 4, 1, null, "p4r1");
		ConsumerRecord<String, String> p4r2 = new ConsumerRecord<>("foo", 4, 2, null, "p4r2");
		ConsumerRecord<String, String> p4r3 = new ConsumerRecord<>("foo", 4, 3, null, "p4r3");
		ConsumerRecord<String, String> p5r0 = new ConsumerRecord<>("foo", 5, 0, null, "p5r0");
		ConsumerRecord<String, String> p5r1 = new ConsumerRecord<>("foo", 5, 1, null, "p5r1");
		ConsumerRecord<String, String> p5r2 = new ConsumerRecord<>("foo", 5, 2, null, "p5r2");
		ConsumerRecord<String, String> p5r3 = new ConsumerRecord<>("foo", 5, 3, null, "p5r3");
		Arrays.asList(p0r0, p0r1, p0r2, p0r3, p1r0, p1r1, p1r2, p1r3, p2r0, p2r1, p2r2, p2r3, p3r0, p3r1, p3r2, p3r3,
				p4r0, p4r1, p4r2, p4r3, p5r0, p5r1, p5r2, p5r3)
				.forEach(consumer::addRecord);


		Message<Object> message;
		Set<String> expected = Stream.of(
				p0r0, p0r1, p0r2, p0r3, // Seek to beginning
				p1r3,                    // Seek to end
				p2r1, p2r2, p2r3,        // Null offset and SeekPosition.TIMESTAMP results in no change in position
				p3r3,                    // Negative offset ends up in seek to end
				p4r2, p4r3,            // Negative offset with relative to current(3)
				p5r2, p5r3                // Positive offset with relative to current(1)
		).map(ConsumerRecord::value).collect(Collectors.toSet());
		Set<Object> received = new HashSet<>();
		while ((message = source.receive()) != null) {
			received.add(message.getHeaders().get(KafkaHeaders.RAW_DATA, ConsumerRecord.class).value());
		}

		assertThat(received).isEqualTo(expected);

		source.pause();
		source.receive();
		source.resume();
		source.receive();
		source.destroy();

		InOrder inOrder = inOrder(consumer);
		inOrder.verify(consumer).assign(anyCollection());
		inOrder.verify(consumer).seekToBeginning(anyCollection());
		inOrder.verify(consumer, times(2)).seekToEnd(anyCollection());
		inOrder.verify(consumer).position(negativeRelativeToCurrent);
		inOrder.verify(consumer).seek(negativeRelativeToCurrent, 2L);
		inOrder.verify(consumer).position(positiveRelativeToCurrent);
		inOrder.verify(consumer).seek(positiveRelativeToCurrent, 2L);
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).pause(anyCollection());
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).resume(anyCollection());
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).close(any());
	}

}
