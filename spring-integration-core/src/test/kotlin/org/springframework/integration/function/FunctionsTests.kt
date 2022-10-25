/*
 * Copyright 2018-2022 the original author or authors.
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

package org.springframework.integration.function

import assertk.assertThat
import assertk.assertions.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.*
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.channel.FluxMessageChannel
import org.springframework.integration.channel.QueueChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.integrationFlow
import org.springframework.integration.endpoint.SourcePollingChannelAdapter
import org.springframework.integration.gateway.GatewayProxyFactoryBean
import org.springframework.integration.handler.ServiceActivatingHandler
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.PollableChannel
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.support.GenericMessage
import org.springframework.messaging.support.MessageBuilder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.stream.Collectors

/**
 * @author Artem Bilan
 * @author Chris Bono
 *
 * @since 5.1
 */
@SpringJUnitConfig
@DirtiesContext
class FunctionsTests {

	@Autowired
	private lateinit var functionServiceChannel: MessageChannel

	@Autowired
	private lateinit var messageConsumerServiceChannel: MessageChannel

	@Autowired
	private lateinit var messageCollector: ArrayList<Message<Any>>

	@Autowired
	private lateinit var counterChannel: SubscribableChannel

	@Autowired
	@Qualifier("kotlinSupplierChannelAdapter")
	private lateinit var kotlinSupplierInboundChannelAdapter: SourcePollingChannelAdapter

	@Autowired
	private lateinit var fromSupplierQueue: PollableChannel

	@Test
	fun `invoke function via transformer`() {
		val replyChannel = QueueChannel()

		val message = MessageBuilder.withPayload("foo")
			.setReplyChannel(replyChannel)
			.build()

		this.functionServiceChannel.send(message)

		val receive = replyChannel.receive(10000)

		val payload = receive?.payload

		assertThat(payload)
			.isNotNull()
			.isEqualTo("FOO")
	}

	@Test
	fun `invoke consumer via service activator`() {
		this.messageConsumerServiceChannel.send(GenericMessage("bar"))

		assertThat(this.messageCollector).size().isEqualTo(1)

		val message = this.messageCollector[0]

		assertThat(message.payload).isEqualTo("bar")
	}

	@Test
	fun `verify supplier`() {
		val countDownLatch = CountDownLatch(10)
		this.counterChannel.subscribe { countDownLatch.countDown() }
		this.kotlinSupplierInboundChannelAdapter.start()

		assertThat(countDownLatch.await(10, TimeUnit.SECONDS)).isTrue()
	}

	@Test
	fun `verify supplier flow`() {
		assertThat(this.fromSupplierQueue.receive(10_000)).isNotNull()
	}

	@Autowired
	private lateinit var monoFunction: Function<String, Mono<Message<*>>>

	@Autowired
	@Qualifier("&monoFunctionGateway.gateway")
	private lateinit var monoFunctionGateway: GatewayProxyFactoryBean<*>

	@Test
	fun `verify Mono gateway`() {
		val mono = this.monoFunction.apply("test")

		StepVerifier.create(mono.map(Message<*>::getPayload).cast(String::class.java))
			.expectNext("TEST")
			.verifyComplete()

		val gateways = this.monoFunctionGateway.gateways
		assertThat(gateways).size().isEqualTo(3)
		val methodNames = gateways.keys.stream().map { it.name }.collect(Collectors.toList())
		assertThat(methodNames).containsAll("apply", "andThen", "compose")
	}

	@Autowired
	private lateinit var suspendServiceChannel: MessageChannel

	@Test
	fun `verify suspend function`() {
		val replyChannel = FluxMessageChannel()
		val testPayload = "test coroutine"
		val stepVerifier =
			StepVerifier.create(Flux.from(replyChannel).map(Message<*>::getPayload).cast(String::class.java))
				.expectNext(testPayload.uppercase())
				.thenCancel()
				.verifyLater()

		suspendServiceChannel.send(
			MessageBuilder.withPayload(testPayload)
				.setReplyChannel(replyChannel)
				.build()
		)

		stepVerifier.verify(Duration.ofSeconds(10))
	}

	@Autowired
	private lateinit var flowServiceChannel: MessageChannel

	@Test
	fun `verify flow function`() {
		val replyChannel = FluxMessageChannel()
		val testPayload = "test flow"
		val stepVerifier =
			StepVerifier.create(Flux.from(replyChannel).map(Message<*>::getPayload).cast(String::class.java))
				.expectNext("$testPayload #1", "$testPayload #2", "$testPayload #3")
				.thenCancel()
				.verifyLater()

		flowServiceChannel.send(
			MessageBuilder.withPayload(testPayload)
				.setReplyChannel(replyChannel)
				.build()
		)

		stepVerifier.verify(Duration.ofSeconds(10))
	}

	@Autowired
	private lateinit var syncFlowServiceChannel: MessageChannel

	@Test
	fun `verify sync flow function reply`() {
		val replyChannel = QueueChannel()
		val testPayload = "test flow"

		syncFlowServiceChannel.send(
			MessageBuilder.withPayload(testPayload)
				.setReplyChannel(replyChannel)
				.build()
		)

		val receive = replyChannel.receive(10_000)

		val payload = receive?.payload

		assertThat(payload)
			.isNotNull()
			.isInstanceOf(Flow::class)

		runBlocking {
			@Suppress("UNCHECKED_CAST")
			val strings = (payload as Flow<String>).toList()
			assertThat(strings).containsExactly("Sync $testPayload #1", "Sync $testPayload #2", "Sync $testPayload #3")
		}
	}

	@Autowired
	private lateinit var suspendRequestChannel: DirectChannel

	@Autowired
	private lateinit var suspendFunGateway: SuspendFunGateway

	@Test
	fun `suspend gateway`() {
		suspendRequestChannel.subscribe(ServiceActivatingHandler { m -> m.payload.toString().uppercase() })

		runBlocking {
			val reply = suspendFunGateway.suspendGateway("test suspend gateway")
			assertThat(reply).isEqualTo("TEST SUSPEND GATEWAY")
		}
	}

	@Configuration
	@EnableIntegration
	@IntegrationComponentScan
	class Config {

		@Bean
		@Transformer(inputChannel = "functionServiceChannel")
		fun kotlinFunction(): (String) -> String {
			return { it.uppercase() }
		}

		@Bean
		fun messageCollector() = ArrayList<Message<Any>>()

		@Bean
		@ServiceActivator(inputChannel = "messageConsumerServiceChannel")
		fun kotlinConsumer(): (Message<Any>) -> Unit {
			return { messageCollector().add(it) }
		}

		@Bean
		fun counterChannel() = DirectChannel()

		@Bean
		@InboundChannelAdapter(
			value = "counterChannel", autoStartup = "false",
			poller = Poller(fixedRate = "10", maxMessagesPerPoll = "1")
		)
		@EndpointId("kotlinSupplierChannelAdapter")
		fun kotlinSupplier(): () -> String {
			return { "baz" }
		}

		@Bean
		fun flowFromSupplier() =
			integrationFlow({ "" }, { poller { it.fixedDelay(10).maxMessagesPerPoll(1) } }) {
				transform<String> { "blank" }
				channel { queue("fromSupplierQueue") }
			}

		@Bean
		fun monoFunctionGateway() =
			integrationFlow<MonoFunction>({ proxyDefaultMethods(true) }) {
				handle<String>({ p, _ -> Mono.just(p).map(String::uppercase) }) { async(true) }
			}


		@ServiceActivator(inputChannel = "suspendServiceChannel")
		suspend fun suspendServiceFunction(payload: String) = payload.uppercase()

		@ServiceActivator(inputChannel = "flowServiceChannel", async = "true")
		fun flowServiceFunction(payload: String) =
			flow {
				for (i in 1..3) {
					emit("$payload #$i")
				}
			}

		@ServiceActivator(inputChannel = "syncFlowServiceChannel")
		fun syncFlowServiceFunction(payload: String) =
			(1..3).asFlow()
				.map { "Sync $payload #$it" }

		@Bean
		fun suspendRequestChannel() = DirectChannel()

	}

	@MessagingGateway(defaultRequestChannel = "suspendRequestChannel")
	interface SuspendFunGateway {

		suspend fun suspendGateway(payload: String): String

	}

	interface MonoFunction : Function<String, Mono<Message<*>>>

}
