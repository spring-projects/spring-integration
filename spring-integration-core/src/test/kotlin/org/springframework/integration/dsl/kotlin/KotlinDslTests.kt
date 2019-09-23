/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.dsl.kotlin

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.QueueChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.convert
import org.springframework.integration.support.MessageBuilder
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHeaders
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.*

/**
 * @author Artem Bilan
 *
 * @since 5.2
 */
@SpringJUnitConfig
@DirtiesContext
class KotlinDslTests {

	@Autowired
	private lateinit var beanFactory: BeanFactory

	@Autowired
	@Qualifier("convertFlow.input")
	private lateinit var convertFlowInput: MessageChannel

	@Autowired
	private lateinit var convertFlowBuilderInput: MessageChannel

	@Test
	fun `test convert extension`() {
		assertThat(this.beanFactory.containsBean("kotlinConverter"))

		val replyChannel = QueueChannel()
		val date = Date()
		val testMessage =
				MessageBuilder.withPayload("{\"name\": \"Baz\",\"date\": " + date.time + "}")
						.setHeader(MessageHeaders.CONTENT_TYPE, "application/json")
						.setReplyChannel(replyChannel)
						.build()
		this.convertFlowInput.send(testMessage)

		var receive = replyChannel.receive(10000)

		var payload = receive?.payload

		assertThat(payload)
				.isNotNull()
				.isInstanceOf(TestPojo::class.java)
				.isEqualTo(TestPojo("Baz", date))

		this.convertFlowBuilderInput.send(testMessage)

		receive = replyChannel.receive(10000)

		payload = receive?.payload

		assertThat(payload)
				.isNotNull()
				.isInstanceOf(TestPojo::class.java)
				.isEqualTo(TestPojo("Baz", date))
	}

	@Configuration
	@EnableIntegration
	class Config {

		@Bean
		fun convertFlow() =
				IntegrationFlow {
					it.convert<TestPojo> { it.id("kotlinConverter") }
				}

		@Bean
		fun convertFlowBuilder() =
				IntegrationFlows.from("convertFlowBuilderInput")
						.convert<TestPojo>()
						.get()

	}

	data class TestPojo(val name: String?, val date: Date?)

}
