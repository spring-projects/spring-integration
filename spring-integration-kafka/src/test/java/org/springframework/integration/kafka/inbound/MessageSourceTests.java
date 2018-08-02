/*
 * Copyright 2018 the original author or authors.
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
import static org.assertj.core.api.Assertions.fail;
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

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.record.TimestampType;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;


/**
 * @author Gary Russell
 * @since 3.0.1
 *
 */
public class MessageSourceTests {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testAck() {
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
		records1.put(topicPartition, Arrays.asList(
				new ConsumerRecord("foo", 0, 0L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "foo")));
		Map<TopicPartition, List<ConsumerRecord>> records2 = new LinkedHashMap<>();
		records2.put(topicPartition, Arrays.asList(
				new ConsumerRecord("foo", 0, 1L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "bar")));
		Map<TopicPartition, List<ConsumerRecord>> records3 = new LinkedHashMap<>();
		records3.put(topicPartition, Arrays.asList(
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
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull())).willReturn(consumer);
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory, "foo");
		source.setRawMessageHeader(true);

		Message<?> received = source.receive();
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(KafkaHeaders.RAW_DATA)).isInstanceOf(ConsumerRecord.class);
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
		source.destroy();
		InOrder inOrder = inOrder(consumer);
		inOrder.verify(consumer).subscribe(anyCollection(), any(ConsumerRebalanceListener.class));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(1L)));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(2L)));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(3L)));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(4L)));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).close();
		inOrder.verifyNoMoreInteractions();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testAckOutOfOrder() {
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
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull())).willReturn(consumer);
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory, "foo");

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
		inOrder.verify(consumer).close();
		inOrder.verifyNoMoreInteractions();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testNack() {
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
		records1.put(topicPartition, Arrays.asList(
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
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull())).willReturn(consumer);
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory, "foo");

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
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(1L)));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).seek(topicPartition, 1L); // rollback
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(2L)));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).close();
		inOrder.verifyNoMoreInteractions();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testNackWithLaterInflight() {
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
		records1.put(topicPartition, Arrays.asList(
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
		given(consumerFactory.createConsumer(isNull(), anyString(), isNull())).willReturn(consumer);
		KafkaMessageSource source = new KafkaMessageSource(consumerFactory, "foo");

		Message<?> received1 = source.receive();
		consumer.paused(); // need some other interaction with mock between polls for InOrder
		Message<?> received2 = source.receive(); // inflight
		assertThat(received1.getHeaders().get(KafkaHeaders.OFFSET)).isEqualTo(0L);
		AcknowledgmentCallback ack1 = StaticMessageHeaderAccessor.getAcknowledgmentCallback(received1);
		Log log1 = spy(KafkaTestUtils.getPropertyValue(ack1, "logger", Log.class));
		new DirectFieldAccessor(ack1).setPropertyValue("logger", log1);
		given(log1.isWarnEnabled()).willReturn(true);
		willDoNothing().given(log1).warn(any());
		ack1.acknowledge(AcknowledgmentCallback.Status.REQUEUE);
		assertThat(received2.getHeaders().get(KafkaHeaders.OFFSET)).isEqualTo(1L);
		AcknowledgmentCallback ack2 = StaticMessageHeaderAccessor.getAcknowledgmentCallback(received2);
		Log log2 = spy(KafkaTestUtils.getPropertyValue(ack1, "logger", Log.class));
		new DirectFieldAccessor(ack2).setPropertyValue("logger", log2);
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
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		inOrder.verify(log1).warn(captor.capture());
		assertThat(captor.getValue())
				.contains("Rolled back")
				.contains("later in-flight offsets [1] will also be re-fetched");
		inOrder.verify(log2).isWarnEnabled();
		captor = ArgumentCaptor.forClass(String.class);
		inOrder.verify(log2).warn(captor.capture());
		assertThat(captor.getValue())
				.contains("Cannot commit offset for ConsumerRecord")
				.contains("; an earlier offset was rolled back");
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(1L)));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(2L)));
		inOrder.verify(consumer).poll(any(Duration.class));
		inOrder.verify(consumer).close();
		inOrder.verifyNoMoreInteractions();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testMaxPollRecords() {
		KafkaMessageSource source = new KafkaMessageSource(new DefaultKafkaConsumerFactory<>(Collections.emptyMap()));
		assertThat((TestUtils.getPropertyValue(source, "consumerFactory.configs", Map.class)
				.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG))).isEqualTo(1);
		source = new KafkaMessageSource(new DefaultKafkaConsumerFactory<>(
				Collections.singletonMap(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 2)));
		assertThat((TestUtils.getPropertyValue(source, "consumerFactory.configs", Map.class)
				.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG))).isEqualTo(1);
		try {
			new KafkaMessageSource((new DefaultKafkaConsumerFactory(Collections.emptyMap()) {

			}));
			fail("Expected exception");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).contains(ConsumerConfig.MAX_POLL_RECORDS_CONFIG);
		}
	}

}
