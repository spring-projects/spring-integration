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

package org.springframework.integration.function

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.size
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.InboundChannelAdapter
import org.springframework.integration.annotation.Poller
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.annotation.Transformer
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.channel.QueueChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.endpoint.SourcePollingChannelAdapter
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.support.GenericMessage
import org.springframework.messaging.support.MessageBuilder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

/**
 * @author Artem Bilan
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
	private lateinit var kotlinSupplierInboundChannelAdapter: SourcePollingChannelAdapter

	@Test
	fun `invoke function via transformer`() {
		val replyChannel = QueueChannel()

		val message = MessageBuilder.withPayload("foo")
				.setReplyChannel(replyChannel)
				.build()

		this.functionServiceChannel.send(message)

		val receive = replyChannel.receive(10000)

		val payload = receive?.payload

		assertk.assert(payload).isNotNull {
			it.isEqualTo("FOO")
		}
	}

	@Test
	fun `invoke consumer via service activator`() {
		this.messageConsumerServiceChannel.send(GenericMessage("bar"))

		assert(this.messageCollector).size().isEqualTo(1)

		val message = this.messageCollector[0]

		assert(message.payload).isEqualTo("bar")
	}

	@Test
	fun `verify supplier`() {
		val countDownLatch = CountDownLatch(10)
		this.counterChannel.subscribe { countDownLatch.countDown() }
		this.kotlinSupplierInboundChannelAdapter.start()

		assert(countDownLatch.await(10, TimeUnit.SECONDS)).isTrue()
	}

	@Configuration
	@EnableIntegration
	class Config {

		@Bean
		@Transformer(inputChannel = "functionServiceChannel")
		fun kotlinFunction(): (String) -> String {
			return { it.toUpperCase() }
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
		@InboundChannelAdapter(value = "counterChannel", autoStartup = "false",
				poller = [Poller(fixedRate = "10", maxMessagesPerPoll = "1")])
		fun kotlinSupplier(): () -> String {
			return { "baz" }
		}

	}

}
