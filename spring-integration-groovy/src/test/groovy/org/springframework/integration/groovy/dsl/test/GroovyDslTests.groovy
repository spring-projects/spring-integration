/*
 * Copyright 2022-2024 the original author or authors.
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

package org.springframework.integration.groovy.dsl.test

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.FluxMessageChannel
import org.springframework.integration.channel.QueueChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlowDefinition
import org.springframework.integration.dsl.Pollers
import org.springframework.integration.dsl.Transformers
import org.springframework.integration.dsl.context.IntegrationFlowContext
import org.springframework.integration.handler.LoggingHandler
import org.springframework.integration.scheduling.PollerMetadata
import org.springframework.integration.support.MessageBuilder
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.PollableChannel
import org.springframework.messaging.support.GenericMessage
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

import static org.springframework.integration.groovy.dsl.IntegrationGroovyDsl.integrationFlow

/**
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig
@DirtiesContext
class GroovyDslTests {

	@Autowired
	private BeanFactory beanFactory

	@Autowired
	private IntegrationFlowContext integrationFlowContext

	@Autowired
	private PollableChannel pollerResultChannel

	@Autowired
	@Qualifier('requestReplyFlow.input')
	private MessageChannel requestReplyFlowInput

	@Autowired
	private MessageChannel requestReplyFixedFlowInput

	@Autowired
	@Qualifier('functionGateway')
	private Function<?, ?> upperCaseFunction

	@Test
	void 'when application starts, it emits message to pollerResultChannel'() {
		assert this.pollerResultChannel.receive(10000) != null
		assert this.pollerResultChannel.receive(10000) != null
	}

	@Test
	void 'requestReplyFlow has to reply'() {
		def replyChannel = new QueueChannel()
		def testMessage =
				MessageBuilder.withPayload('hello')
						.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
						.build()

		this.requestReplyFlowInput.send(testMessage)

		assert replyChannel.receive(1000).payload == 'HELLO'
	}

	@Test
	void 'requestReplyFixedFlow has to reply'() {
		def replyChannel = new QueueChannel()
		def testMessage =
				MessageBuilder.withPayload(4)
						.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
						.build()

		this.requestReplyFixedFlowInput.send(testMessage)

		assert replyChannel.receive(1000).payload == 16
	}

	@Test
	void 'uppercase function'() {
		assert this.upperCaseFunction.apply('test'.bytes) == 'TEST'
	}

	@Test
	void 'reactive publisher flow'() {
		def fluxChannel = new FluxMessageChannel()

		def verifyLater =
				StepVerifier
						.create(Flux.from(fluxChannel).map { it.payload })
						.expectNext(4, 6)
						.thenCancel()
						.verifyLater()

		def publisher = Flux.just(2, 3).map { new GenericMessage<>(it) }

		def integrationFlow =
				integrationFlow(publisher) {
					transform {
						it.<Message<Integer>, Integer>transformer { it.payload * 2 }
						expectedType Message<Integer>
						id 'foo'
					}
					channel fluxChannel
				}

		def registration = this.integrationFlowContext.registration(integrationFlow).register()

		verifyLater.verify(Duration.ofSeconds(10))

		registration.destroy()
	}

	@Autowired
	@Qualifier('scatterGatherFlow.input')
	private MessageChannel scatterGatherFlowInput

	@Test
	void 'Scatter-Gather'() {
		def replyChannel = new QueueChannel()
		def request =
				MessageBuilder.withPayload("foo")
						.setReplyChannel(replyChannel)
						.build()

		this.scatterGatherFlowInput.send(request)

		def bestQuoteMessage = replyChannel.receive(10000)
		assert (bestQuoteMessage?.payload as List).size() >= 1
	}

	@Autowired
	@Qualifier('oddFlow.input')
	private MessageChannel oddFlowInput

	@Test
	void 'oddFlow must reply'() {
		def replyChannel = new QueueChannel()
		def testMessage =
				MessageBuilder.withPayload('test')
						.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
						.build()

		this.oddFlowInput.send(testMessage)

		assert replyChannel.receive(1000).payload == 'odd'
	}


	@Autowired
	@Qualifier('flowLambda.input')
	private MessageChannel flowLambdaInput

	@Autowired
	private PollableChannel wireTapChannel

	@Test
	void 'flow from lambda'() {
		def replyChannel = new QueueChannel()
		def message =
				MessageBuilder.withPayload('test')
						.setHeader('headerToRemove', 'no value')
						.setReplyChannel(replyChannel)
						.build()

		this.flowLambdaInput.send message

		def receive = replyChannel.receive(10_000)

		assert receive?.payload == 'TEST'
		assert !receive?.headers?.containsKey('headerToRemove')
		assert this.wireTapChannel.receive(10_000)?.payload == 'test'
	}

	@Autowired
	@Qualifier('externalServiceFlow.input')
	private MessageChannel externalServiceFlowInput

	@Autowired
	GroovyTestService groovyTestService

	@Test
	void 'handle service'() {
		this.externalServiceFlowInput.send(new GenericMessage<Object>('test'))
		assert groovyTestService.result.get() == 'TEST'
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean(PollerMetadata.DEFAULT_POLLER)
		poller() {
			Pollers.fixedDelay(1000)
		}


		@Bean
		someFlow() {
			integrationFlow { 'test' }
					{
						log LoggingHandler.Level.WARN, 'test.category'
						channel { queue 'pollerResultChannel' }
					}
		}

		@Bean
		requestReplyFlow() {
			integrationFlow {
				fluxTransform { it.map { it } }
				transform {
					transformer { it.toUpperCase() }
				}
			}
		}

		@Bean
		requestReplyFixedFlow() {
			integrationFlow 'requestReplyFixedFlowInput', true,
					{
						handle Integer, { p, h -> p**2 }
					}
		}

		@Bean
		functionFlow() {
			integrationFlow Function<byte[], String>,
					{ beanName 'functionGateway' },
					{
						transform {
							transformer Transformers.objectToString()
							id 'objectToStringTransformer'
						}
						transform {
							transformer { it.toUpperCase() }
						}
						splitWith {
							expectedType Message<?>
							function { it.payload }
						}
						splitWith {
							expectedType Object
							id 'splitterEndpoint'
							function { it }
						}
						resequence()
						aggregate {
							id 'aggregator'
							outputProcessor { it.one }
						}
					}
		}

		@Bean
		scatterGatherFlow() {
			integrationFlow {
				scatterGather(
						{
							applySequence true
							recipientFlow({ true }, recipientSubFlow())
							recipientFlow({ true },
									integrationFlow { handle Void, { p, h -> Math.random() * 10 } })
							recipientFlow({ true },
									integrationFlow { handle Void, { p, h -> Math.random() * 10 } })
						},
						{
							releaseStrategy {
								it.size() == 3 || it.messages.any { it.payload as Double > 5 }
							}
						})
						{
							gatherTimeout 10_000
						}
			}
		}

		static recipientSubFlow() {
			integrationFlow { handle Void, { p, h -> Math.random() * 10 } }
		}

		@Bean
		IntegrationFlow oddFlow() {
			{ IntegrationFlowDefinition flow ->
				flow.handle(Object, { p, h -> 'odd' })
			}
		}

		@Bean
		flowLambda() {
			integrationFlow {
				filter String, { it == 'test' }, { id 'filterEndpoint' }
				headerFilter {
					patternMatch false
					headersToRemove "notAHeader", "headerToRemove"
				}
				wireTap integrationFlow {
					channel { queue 'wireTapChannel' }
				}
				delay {
					messageGroupId 'delayGroup'
					defaultDelay 100
				}
				transform {
					transformer { it.toUpperCase() }
				}
			}
		}

		@Bean
		flowFromSupplier() {
			integrationFlow({ 'bar' }, { poller { it.fixedDelay(10).maxMessagesPerPoll(1) } }) {
				channel { queue 'fromSupplierQueue' }
			}
		}

		@Bean
		myService() {
			new GroovyTestService()
		}

		@Bean
		externalServiceFlow(GroovyTestService groovyTestService) {
			integrationFlow {
				handle groovyTestService
			}

		}

	}

	@CompileStatic
	static class GroovyTestService {

		AtomicReference<String> result = new AtomicReference<>()

		void handlePayload(String payload) {
			result.set payload.toUpperCase()
		}

	}

}
