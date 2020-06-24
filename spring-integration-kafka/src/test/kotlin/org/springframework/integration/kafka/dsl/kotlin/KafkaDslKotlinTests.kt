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

package org.springframework.integration.kafka.dsl.kotlin

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import assertk.assertions.isTrue
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.IntegrationMessageHeaderAccessor
import org.springframework.integration.MessageRejectedException
import org.springframework.integration.channel.PublishSubscribeChannel
import org.springframework.integration.channel.QueueChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.Pollers
import org.springframework.integration.dsl.integrationFlow
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer
import org.springframework.integration.kafka.dsl.Kafka
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler
import org.springframework.integration.kafka.support.RawRecordHeaderErrorMessageStrategy
import org.springframework.integration.support.MessageBuilder
import org.springframework.integration.test.util.TestUtils
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ConsumerProperties
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.GenericMessageListenerContainer
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.DefaultKafkaHeaderMapper
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.PollableChannel
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.support.ErrorMessage
import org.springframework.messaging.support.GenericMessage
import org.springframework.retry.support.RetryTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.4
 */

@SpringJUnitConfig
@DirtiesContext
@EmbeddedKafka(topics = [KafkaDslKotlinTests.TEST_TOPIC1, KafkaDslKotlinTests.TEST_TOPIC2,
	KafkaDslKotlinTests.TEST_TOPIC3, KafkaDslKotlinTests.TEST_TOPIC4, KafkaDslKotlinTests.TEST_TOPIC5])
class KafkaDslKotlinTests {

	companion object {

		const val TEST_TOPIC1 = "test-topic1"

		const val TEST_TOPIC2 = "test-topic2"

		const val TEST_TOPIC3 = "test-topic3"

		const val TEST_TOPIC4 = "test-topic4"

		const val TEST_TOPIC5 = "test-topic5"

	}


	@Autowired
	@Qualifier("sendToKafkaFlow.input")
	private lateinit var sendToKafkaFlowInput: MessageChannel

	@Autowired
	private lateinit var listeningFromKafkaResults1: PollableChannel

	@Autowired
	private lateinit var listeningFromKafkaResults2: PollableChannel

	@Autowired
	@Qualifier("kafkaProducer1.handler")
	private lateinit var kafkaProducer1: KafkaProducerMessageHandler<*, *>

	@Autowired
	@Qualifier("kafkaProducer2.handler")
	private lateinit var kafkaProducer2: KafkaProducerMessageHandler<*, *>

	@Autowired
	private lateinit var errorChannel: PollableChannel

	@Autowired(required = false)
	@Qualifier("topic1ListenerContainer")
	private lateinit var messageListenerContainer: MessageListenerContainer

	@Autowired(required = false)
	@Qualifier("kafkaTemplate:test-topic1")
	private lateinit var kafkaTemplateTopic1: KafkaTemplate<Any, Any>

	@Autowired(required = false)
	@Qualifier("kafkaTemplate:test-topic2")
	private lateinit var kafkaTemplateTopic2: KafkaTemplate<*, *>

	@Autowired
	private lateinit var mapper: DefaultKafkaHeaderMapper

	@Autowired
	private lateinit var config: ContextConfiguration

	@Autowired
	private lateinit var gate: Gate

	@Test
	fun testKafkaAdapters() {
		this.sendToKafkaFlowInput.send(GenericMessage("foo", hashMapOf<String, Any>("foo" to "bar")))

		assertThat(TestUtils.getPropertyValue(this.kafkaProducer1, "headerMapper")).isSameAs(this.mapper)

		for (i in 0..99) {
			val receive = this.listeningFromKafkaResults1.receive(20000)
			assertThat(receive).isNotNull()
			assertThat(receive!!.payload).isEqualTo("FOO")
			val headers = receive.headers
			assertThat(headers.containsKey(KafkaHeaders.ACKNOWLEDGMENT)).isTrue()
			val acknowledgment = headers[KafkaHeaders.ACKNOWLEDGMENT] as Acknowledgment
			acknowledgment.acknowledge()
			assertThat(headers[KafkaHeaders.RECEIVED_TOPIC]).isEqualTo(TEST_TOPIC1)
			assertThat(headers[KafkaHeaders.RECEIVED_MESSAGE_KEY]).isEqualTo(i + 1)
			assertThat(headers[KafkaHeaders.RECEIVED_PARTITION_ID]).isEqualTo(0)
			assertThat(headers[KafkaHeaders.OFFSET]).isEqualTo(i.toLong())
			assertThat(headers[KafkaHeaders.TIMESTAMP_TYPE]).isEqualTo("CREATE_TIME")
			assertThat(headers[KafkaHeaders.RECEIVED_TIMESTAMP]).isEqualTo(1487694048633L)
			assertThat(headers["foo"]).isEqualTo("bar")
		}

		for (i in 0..99) {
			val receive = this.listeningFromKafkaResults2.receive(20000)
			assertThat(receive).isNotNull()
			assertThat(receive!!.payload).isEqualTo("FOO")
			val headers = receive.headers
			assertThat(headers.containsKey(KafkaHeaders.ACKNOWLEDGMENT)).isTrue()
			val acknowledgment = headers[KafkaHeaders.ACKNOWLEDGMENT] as Acknowledgment
			acknowledgment.acknowledge()
			assertThat(headers[KafkaHeaders.RECEIVED_TOPIC]).isEqualTo(TEST_TOPIC2)
			assertThat(headers[KafkaHeaders.RECEIVED_MESSAGE_KEY]).isEqualTo(i + 1)
			assertThat(headers[KafkaHeaders.RECEIVED_PARTITION_ID]).isEqualTo(0)
			assertThat(headers[KafkaHeaders.OFFSET]).isEqualTo(i.toLong())
			assertThat(headers[KafkaHeaders.TIMESTAMP_TYPE]).isEqualTo("CREATE_TIME")
			assertThat(headers[KafkaHeaders.RECEIVED_TIMESTAMP]).isEqualTo(1487694048644L)
		}

		val message = MessageBuilder.withPayload("BAR").setHeader(KafkaHeaders.TOPIC, TEST_TOPIC2).build()

		this.sendToKafkaFlowInput.send(message)

		assertThat(this.listeningFromKafkaResults1.receive(10)).isNull()

		val error = this.errorChannel.receive(10000)

		assertThat(error).isNotNull().isInstanceOf(ErrorMessage::class.java)

		val payload = error?.payload

		assertThat(payload).isNotNull().isInstanceOf(MessageRejectedException::class.java)

		assertThat(this.messageListenerContainer).isNotNull()
		assertThat(this.kafkaTemplateTopic1).isNotNull()
		assertThat(this.kafkaTemplateTopic2).isNotNull()

		this.kafkaTemplateTopic1.send(TEST_TOPIC3, "foo")
		assertThat(this.config.sourceFlowLatch.await(10, TimeUnit.SECONDS)).isTrue()
		assertThat(this.config.fromSource).isEqualTo("foo")
	}

	@Test
	fun testGateways() {
		assertThat(this.config.replyContainerLatch.await(30, TimeUnit.SECONDS))
		assertThat(this.gate.exchange(TEST_TOPIC4, "foo")).isEqualTo("FOO")
	}

	@Configuration
	@EnableIntegration
	@EnableKafka
	class ContextConfiguration {

		val sourceFlowLatch = CountDownLatch(1)

		val replyContainerLatch = CountDownLatch(1)

		var fromSource: Any? = null

		@Autowired
		private lateinit var embeddedKafka: EmbeddedKafkaBroker

		@Bean
		fun consumerFactory(): ConsumerFactory<Int, String> {
			val props = KafkaTestUtils.consumerProps("test1", "false", this.embeddedKafka)
			props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
			return DefaultKafkaConsumerFactory(props)
		}

		@Bean
		fun errorChannel() = QueueChannel()

		@Bean
		fun topic1ListenerFromKafkaFlow() =
				integrationFlow(
						Kafka.messageDrivenChannelAdapter(consumerFactory(),
								KafkaMessageDrivenChannelAdapter.ListenerMode.record, TEST_TOPIC1)
								.configureListenerContainer {
									it.ackMode(ContainerProperties.AckMode.MANUAL)
											.id("topic1ListenerContainer")
								}
								.recoveryCallback(ErrorMessageSendingRecoverer(errorChannel(),
										RawRecordHeaderErrorMessageStrategy()))
								.retryTemplate(RetryTemplate())
								.filterInRetry(true)) {
					filter<Message<*>>({ m -> (m.headers[KafkaHeaders.RECEIVED_MESSAGE_KEY] as Int) < 101 }) { throwExceptionOnRejection(true) }
					transform<String> { it.toUpperCase() }
					channel { queue("listeningFromKafkaResults1") }
				}

		@Bean
		fun topic2ListenerFromKafkaFlow() =
				integrationFlow(
						Kafka.messageDrivenChannelAdapter(consumerFactory(),
								KafkaMessageDrivenChannelAdapter.ListenerMode.record, TEST_TOPIC2)
								.configureListenerContainer { it.ackMode(ContainerProperties.AckMode.MANUAL) }
								.recoveryCallback(ErrorMessageSendingRecoverer(errorChannel(),
										RawRecordHeaderErrorMessageStrategy()))
								.retryTemplate(RetryTemplate())
								.filterInRetry(true)) {
					filter<Message<*>>({ m -> (m.headers[KafkaHeaders.RECEIVED_MESSAGE_KEY] as Int) < 101 }) { throwExceptionOnRejection(true) }
					transform<String> { it.toUpperCase() }
					channel { queue("listeningFromKafkaResults2") }
				}

		@Bean
		fun producerFactory(): DefaultKafkaProducerFactory<Int, String> {
			val props = KafkaTestUtils.producerProps(this.embeddedKafka)
			props[ProducerConfig.MAX_BLOCK_MS_CONFIG] = "10000"
			return DefaultKafkaProducerFactory(props)
		}

		@Bean
		fun sendToKafkaFlow() =
				integrationFlow {
					split<String> { p -> Stream.generate { p }.limit(101) }
					publishSubscribe(PublishSubscribeChannel(),
							{
								handle(kafkaMessageHandler(producerFactory(), TEST_TOPIC1)
										.timestampExpression("T(Long).valueOf('1487694048633')")
								) { id("kafkaProducer1") }
							},
							{
								handle(kafkaMessageHandler(producerFactory(), TEST_TOPIC2)
										.timestamp<Any> { 1487694048644L }
								) { id("kafkaProducer2") }
							}
					)
				}

		@Bean
		fun mapper() = DefaultKafkaHeaderMapper()

		private fun kafkaMessageHandler(producerFactory: ProducerFactory<Int, String>, topic: String) =
				Kafka.outboundChannelAdapter(producerFactory)
						.messageKey<Any> { it.headers[IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER] }
						.headerMapper(mapper())
						.sync(true)
						.partitionId<Any> { 0 }
						.topicExpression("headers[kafka_topic] ?: '$topic'")
						.configureKafkaTemplate { it.id("kafkaTemplate:$topic") }


		@Bean
		fun sourceFlow() =
				integrationFlow(Kafka.inboundChannelAdapter(consumerFactory(), ConsumerProperties(TEST_TOPIC3)),
						{ poller(Pollers.fixedDelay(100)) }) {
					handle { m ->
						this@ContextConfiguration.fromSource = m.payload
						this@ContextConfiguration.sourceFlowLatch.countDown()
					}
				}

		@Bean
		fun replyingKafkaTemplate() =
				ReplyingKafkaTemplate(producerFactory(), replyContainer())
						.also {
							it.setDefaultReplyTimeout(Duration.ofSeconds(30))
						}

		@Bean
		fun outboundGateFlow() =
				integrationFlow<Gate> {
					handle(Kafka.outboundGateway(replyingKafkaTemplate())
							.sync(true))
				}

		private fun replyContainer(): GenericMessageListenerContainer<Int, String> {
			val containerProperties = ContainerProperties(TEST_TOPIC5)
			containerProperties.groupId = "outGate"
			containerProperties.consumerRebalanceListener = object : ConsumerRebalanceListener {

				override fun onPartitionsRevoked(partitions: Collection<TopicPartition>) {
					// empty
				}

				override fun onPartitionsAssigned(partitions: Collection<TopicPartition>) {
					this@ContextConfiguration.replyContainerLatch.countDown()
				}

			}
			return KafkaMessageListenerContainer(consumerFactory(), containerProperties)
		}

		@Bean
		fun serverGateway() =
				integrationFlow(Kafka.inboundGateway(consumerFactory(), containerProperties(), producerFactory())) {
					transform<String> { it.toUpperCase() }
				}

		private fun containerProperties() =
				ContainerProperties(TEST_TOPIC4)
						.also {
							it.groupId = "inGateGroup"
						}

	}

	interface Gate {

		fun exchange(@Header(KafkaHeaders.TOPIC) topic: String, out: String): String

	}

}
