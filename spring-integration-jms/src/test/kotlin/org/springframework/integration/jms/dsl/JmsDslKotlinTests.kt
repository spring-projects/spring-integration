/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.jms.dsl

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import jakarta.jms.DeliveryMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.IntegrationMessageHeaderAccessor
import org.springframework.integration.channel.QueueChannel
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
		fun jmsOutboundInboundReplyChannel() = MessageChannels.queue()

		@Bean
		fun jmsMessageDrivenFlowWithContainer(jmsOutboundInboundReplyChannel: QueueChannel) =
			integrationFlow(
				Jms.messageDrivenChannelAdapter(
					Jms.container(amqFactory, "containerSpecDestination")
						.pubSubDomain(false)
						.taskExecutor(Executors.newCachedThreadPool())
				)
					.headerMapper(jmsHeaderMapper())
			) {
				transform { it: String -> it.trim { it <= ' ' } }
				channel(jmsOutboundInboundReplyChannel)
			}

	}

}
