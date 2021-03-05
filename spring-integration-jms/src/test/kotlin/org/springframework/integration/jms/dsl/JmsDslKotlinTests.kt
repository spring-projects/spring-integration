/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.integration.jms.dsl

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.IntegrationMessageHeaderAccessor
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.MessageChannels
import org.springframework.integration.dsl.integrationFlow
import org.springframework.integration.jms.ActiveMQMultiContextTests
import org.springframework.integration.jms.DefaultJmsHeaderMapper
import org.springframework.integration.support.MessageBuilder
import org.springframework.jms.support.JmsHeaders
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.PollableChannel
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.concurrent.Executors
import javax.jms.DeliveryMode

/**
 * @author Artem Bilan
 *
 * @since 5.0.5
 */
@SpringJUnitConfig
@DirtiesContext
class JmsDslKotlinTests : ActiveMQMultiContextTests() {

	@Autowired
	@Qualifier("jmsOutboundFlow.input")
	private lateinit var jmsOutboundInboundChannel: MessageChannel

	@Autowired
	private lateinit var jmsOutboundInboundReplyChannel: PollableChannel

	@Test
	fun `test JMS Channel Adapters DSL`() {

		this.jmsOutboundInboundChannel.send(
			MessageBuilder.withPayload("    foo    ")
				.setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, "containerSpecDestination")
				.setPriority(9)
				.build()
		)

		val receive = this.jmsOutboundInboundReplyChannel.receive(10000)

		val payload = receive?.payload

		assertThat(payload).isNotNull().isEqualTo("foo")

		assertThat(receive?.headers)
			.isNotNull()
			.all {
				contains(IntegrationMessageHeaderAccessor.PRIORITY, 9)
				contains(JmsHeaders.DELIVERY_MODE, 1)
			}

		val expiration = receive!!.headers[JmsHeaders.EXPIRATION] as Long
		assertThat(expiration).isGreaterThan(System.currentTimeMillis())
	}

	@Configuration
	@EnableIntegration
	class Config {

		@Bean
		fun jmsOutboundFlow() =
			integrationFlow {
				handle(Jms.outboundAdapter(connectionFactory)
					.apply {
						destinationExpression("headers." + SimpMessageHeaderAccessor.DESTINATION_HEADER)
						deliveryModeFunction<Any> { DeliveryMode.NON_PERSISTENT }
						timeToLiveExpression("10000")
						configureJmsTemplate { it.explicitQosEnabled(true) }
					}
				)
			}

		@Bean
		fun jmsHeaderMapper(): DefaultJmsHeaderMapper {
			val jmsHeaderMapper = DefaultJmsHeaderMapper()
			jmsHeaderMapper.setMapInboundDeliveryMode(true)
			jmsHeaderMapper.setMapInboundExpiration(true)
			return jmsHeaderMapper
		}

		@Bean
		fun jmsOutboundInboundReplyChannel() = MessageChannels.queue().get()

		@Bean
		fun jmsMessageDrivenFlowWithContainer() =
			integrationFlow(
				Jms.messageDrivenChannelAdapter(
					Jms.container(amqFactory, "containerSpecDestination")
						.pubSubDomain(false)
						.taskExecutor(Executors.newCachedThreadPool())
				)
					.headerMapper(jmsHeaderMapper())
			) {
				transform { it: String -> it.trim { it <= ' ' } }
				channel(jmsOutboundInboundReplyChannel())
			}

	}

}
