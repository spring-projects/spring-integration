/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.integration.kafka.dsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.integration.kafka.support.RawRecordHeaderErrorMessageStrategy;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 * @author Nasko Vasilev
 * @author Biju Kunjummen
 * @author Gary Russell
 *
 * @since 3.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class KafkaDslTests {

	private static final String TEST_TOPIC1 = "test-topic1";

	private static final String TEST_TOPIC2 = "test-topic2";

	private static final String TEST_TOPIC3 = "test-topic3";

	@ClassRule
	public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, TEST_TOPIC1, TEST_TOPIC2, TEST_TOPIC3);

	@Autowired
	@Qualifier("sendToKafkaFlow.input")
	private MessageChannel sendToKafkaFlowInput;

	@Autowired
	private PollableChannel listeningFromKafkaResults1;

	@Autowired
	private PollableChannel listeningFromKafkaResults2;

	@Autowired
	@Qualifier("kafkaProducer1.handler")
	private KafkaProducerMessageHandler<?, ?> kafkaProducer1;

	@Autowired
	@Qualifier("kafkaProducer2.handler")
	private KafkaProducerMessageHandler<?, ?> kafkaProducer2;

	@Autowired
	private PollableChannel errorChannel;

	@Autowired(required = false)
	@Qualifier("topic1ListenerContainer")
	private MessageListenerContainer messageListenerContainer;

	@Autowired(required = false)
	@Qualifier("kafkaTemplate:" + TEST_TOPIC1)
	private KafkaTemplate<Object, Object> kafkaTemplateTopic1;

	@Autowired(required = false)
	@Qualifier("kafkaTemplate:" + TEST_TOPIC2)
	private KafkaTemplate<?, ?> kafkaTemplateTopic2;

	@Autowired
	private DefaultKafkaHeaderMapper mapper;

	@Autowired
	private ContextConfiguration config;

	@Test
	public void testKafkaAdapters() throws Exception {

		assertThatThrownBy(() -> this.sendToKafkaFlowInput.send(new GenericMessage<>("foo")))
				.hasMessageContaining("10 is not in the range");

		this.kafkaProducer1.setPartitionIdExpression(new ValueExpression<>(0));
		this.kafkaProducer2.setPartitionIdExpression(new ValueExpression<>(0));
		this.sendToKafkaFlowInput.send(new GenericMessage<>("foo", Collections.singletonMap("foo", "bar")));

		assertThat(TestUtils.getPropertyValue(this.kafkaProducer1, "headerMapper")).isSameAs(this.mapper);

		for (int i = 0; i < 100; i++) {
			Message<?> receive = this.listeningFromKafkaResults1.receive(20000);
			assertThat(receive).isNotNull();
			assertThat(receive.getPayload()).isEqualTo("FOO");
			MessageHeaders headers = receive.getHeaders();
			assertThat(headers.containsKey(KafkaHeaders.ACKNOWLEDGMENT)).isTrue();
			Acknowledgment acknowledgment = headers.get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
			acknowledgment.acknowledge();
			assertThat(headers.get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(TEST_TOPIC1);
			assertThat(headers.get(KafkaHeaders.RECEIVED_MESSAGE_KEY)).isEqualTo(i + 1);
			assertThat(headers.get(KafkaHeaders.RECEIVED_PARTITION_ID)).isEqualTo(0);
			assertThat(headers.get(KafkaHeaders.OFFSET)).isEqualTo((long) i);
			assertThat(headers.get(KafkaHeaders.TIMESTAMP_TYPE)).isEqualTo("CREATE_TIME");
			assertThat(headers.get(KafkaHeaders.RECEIVED_TIMESTAMP)).isEqualTo(1487694048633L);
			assertThat(headers.get("foo")).isEqualTo("bar");
		}

		for (int i = 0; i < 100; i++) {
			Message<?> receive = this.listeningFromKafkaResults2.receive(20000);
			assertThat(receive).isNotNull();
			assertThat(receive.getPayload()).isEqualTo("FOO");
			MessageHeaders headers = receive.getHeaders();
			assertThat(headers.containsKey(KafkaHeaders.ACKNOWLEDGMENT)).isTrue();
			Acknowledgment acknowledgment = headers.get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
			acknowledgment.acknowledge();
			assertThat(headers.get(KafkaHeaders.RECEIVED_TOPIC)).isEqualTo(TEST_TOPIC2);
			assertThat(headers.get(KafkaHeaders.RECEIVED_MESSAGE_KEY)).isEqualTo(i + 1);
			assertThat(headers.get(KafkaHeaders.RECEIVED_PARTITION_ID)).isEqualTo(0);
			assertThat(headers.get(KafkaHeaders.OFFSET)).isEqualTo((long) i);
			assertThat(headers.get(KafkaHeaders.TIMESTAMP_TYPE)).isEqualTo("CREATE_TIME");
			assertThat(headers.get(KafkaHeaders.RECEIVED_TIMESTAMP)).isEqualTo(1487694048644L);
		}

		Message<String> message = MessageBuilder.withPayload("BAR").setHeader(KafkaHeaders.TOPIC, TEST_TOPIC2).build();

		this.sendToKafkaFlowInput.send(message);

		assertThat(this.listeningFromKafkaResults1.receive(10)).isNull();

		Message<?> error = this.errorChannel.receive(10000);
		assertThat(error).isNotNull();
		assertThat(error).isInstanceOf(ErrorMessage.class);
		assertThat(error.getPayload()).isInstanceOf(MessageRejectedException.class);

		assertThat(this.messageListenerContainer).isNotNull();
		assertThat(this.kafkaTemplateTopic1).isNotNull();
		assertThat(this.kafkaTemplateTopic2).isNotNull();

		this.kafkaTemplateTopic1.send(TEST_TOPIC3, "foo");
		assertThat(this.config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.config.fromSource).isEqualTo("foo");
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		private final CountDownLatch latch = new CountDownLatch(1);

		private Object fromSource;

		@Bean
		public ConsumerFactory<Integer, String> consumerFactory() {
			Map<String, Object> props = KafkaTestUtils.consumerProps("test1", "false", embeddedKafka);
			props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
			return new DefaultKafkaConsumerFactory<>(props);
		}

		@Bean
		public PollableChannel errorChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow topic1ListenerFromKafkaFlow() {
			return IntegrationFlows
					.from(Kafka.messageDrivenChannelAdapter(consumerFactory(),
							KafkaMessageDrivenChannelAdapter.ListenerMode.record, TEST_TOPIC1)
							.configureListenerContainer(c ->
									c.ackMode(AbstractMessageListenerContainer.AckMode.MANUAL)
											.id("topic1ListenerContainer"))
							.recoveryCallback(new ErrorMessageSendingRecoverer(errorChannel(),
									new RawRecordHeaderErrorMessageStrategy()))
							.retryTemplate(new RetryTemplate())
							.filterInRetry(true))
					.filter(Message.class, m ->
									m.getHeaders().get(KafkaHeaders.RECEIVED_MESSAGE_KEY, Integer.class) < 101,
							f -> f.throwExceptionOnRejection(true))
					.<String, String>transform(String::toUpperCase)
					.channel(c -> c.queue("listeningFromKafkaResults1"))
					.get();
		}

		@Bean
		public IntegrationFlow topic2ListenerFromKafkaFlow() {
			return IntegrationFlows
					.from(Kafka.messageDrivenChannelAdapter(consumerFactory(),
							KafkaMessageDrivenChannelAdapter.ListenerMode.record, TEST_TOPIC2)
							.configureListenerContainer(c ->
									c.ackMode(AbstractMessageListenerContainer.AckMode.MANUAL))
							.recoveryCallback(new ErrorMessageSendingRecoverer(errorChannel(),
									new RawRecordHeaderErrorMessageStrategy()))
							.retryTemplate(new RetryTemplate())
							.filterInRetry(true))
					.filter(Message.class, m ->
									m.getHeaders().get(KafkaHeaders.RECEIVED_MESSAGE_KEY, Integer.class) < 101,
							f -> f.throwExceptionOnRejection(true))
					.<String, String>transform(String::toUpperCase)
					.channel(c -> c.queue("listeningFromKafkaResults2"))
					.get();
		}

		@Bean
		public ProducerFactory<Integer, String> producerFactory() {
			return new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(embeddedKafka));
		}

		@Bean
		public IntegrationFlow sendToKafkaFlow() {
			return f -> f
					.<String>split(p -> Stream.generate(() -> p).limit(101).iterator(), null)
					.publishSubscribeChannel(c -> c
							.subscribe(sf -> sf.handle(
									kafkaMessageHandler(producerFactory(), TEST_TOPIC1)
											.timestampExpression("T(Long).valueOf('1487694048633')"),
									e -> e.id("kafkaProducer1")))
							.subscribe(sf -> sf.handle(
									kafkaMessageHandler(producerFactory(), TEST_TOPIC2)
											.timestamp(m -> 1487694048644L),
									e -> e.id("kafkaProducer2")))
					);
		}

		@Bean
		public DefaultKafkaHeaderMapper mapper() {
			return new DefaultKafkaHeaderMapper();
		}

		private KafkaProducerMessageHandlerSpec<Integer, String, ?> kafkaMessageHandler(
				ProducerFactory<Integer, String> producerFactory, String topic) {
			return Kafka
					.outboundChannelAdapter(producerFactory)
					.messageKey(m -> m
							.getHeaders()
							.get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER))
					.headerMapper(mapper())
					.partitionId(m -> 10)
					.topicExpression("headers[kafka_topic] ?: '" + topic + "'")
					.configureKafkaTemplate(t -> t.id("kafkaTemplate:" + topic));
		}


		@Bean
		public IntegrationFlow sourceFlow() {
			return IntegrationFlows
					.from(Kafka.inboundChannelAdapter(consumerFactory(), TEST_TOPIC3),
							e -> e.poller(Pollers.fixedDelay(100)))
					.handle(p -> {
						this.fromSource = p.getPayload();
						this.latch.countDown();
					})
					.get();
		}


	}

}
