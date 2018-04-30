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

package org.springframework.integration.jms.dsl

import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.apache.activemq.ActiveMQConnectionFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.MessageChannels
import org.springframework.integration.support.MessageBuilder
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
class JmsDslKotlinTests {

    @Autowired
    @Qualifier("jmsOutboundFlow.input")
    private lateinit var jmsOutboundInboundChannel: MessageChannel

    @Autowired
    private lateinit var jmsOutboundInboundReplyChannel: PollableChannel

    @Test
    fun `test JMS Channel Adapters DSL`() {

        this.jmsOutboundInboundChannel.send(MessageBuilder.withPayload("    foo    ")
                .setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, "containerSpecDestination")
                .build())

        val receive = this.jmsOutboundInboundReplyChannel.receive(10000)

        val payload = receive?.payload

        assertk.assert(payload).isNotNull {
            it.isEqualTo("foo")
        }
    }

    @Configuration
    @EnableIntegration
    class Config {

        @Bean
        fun jmsConnectionFactory(): ActiveMQConnectionFactory {
            val activeMQConnectionFactory = ActiveMQConnectionFactory("vm://localhost?broker.persistent=false")
            activeMQConnectionFactory.isTrustAllPackages = true
            return activeMQConnectionFactory
        }

        @Bean
        fun jmsOutboundFlow() =
                IntegrationFlow { f ->
                    f.handle(Jms.outboundAdapter(jmsConnectionFactory())
                            .destinationExpression("headers." + SimpMessageHeaderAccessor.DESTINATION_HEADER))
                }

        @Bean
        fun jmsMessageDrivenFlowWithContainer() =
                IntegrationFlows.from(
                        Jms.messageDrivenChannelAdapter(
                                Jms.container(jmsConnectionFactory(), "containerSpecDestination")
                                        .pubSubDomain(false)
                                        .taskExecutor(Executors.newCachedThreadPool())
                                        .get()))
                        .transform({ it: String -> it.trim({ it <= ' ' }) })
                        .channel(jmsOutboundInboundReplyChannel())
                        .get()

        @Bean
        fun jmsOutboundInboundReplyChannel() = MessageChannels.queue().get()

    }


}
