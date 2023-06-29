/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.integration.support.management.observation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.micrometer.common.KeyValues;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.simple.SpansAssert;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.EndpointId;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.ObservationPropagationChannelInterceptor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.handler.advice.HandleMessageAdvice;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class IntegrationObservabilityZipkinTests extends SampleTestRunner {

	@Override
	public TracingSetup[] getTracingSetup() {
		return new TracingSetup[] {TracingSetup.IN_MEMORY_BRAVE, TracingSetup.ZIPKIN_BRAVE};
	}

	@Override
	public SampleTestRunnerConsumer yourCode() {
		return (bb, meterRegistry) -> {
			ObservationRegistry observationRegistry = getObservationRegistry();

			observationRegistry.observationConfig()
					.observationPredicate((name, context) ->
							!(context instanceof MessageRequestReplyReceiverContext messageRequestReplyReceiverContext)
									|| !messageRequestReplyReceiverContext.getGatewayName()
									.equals("skippedObservationInboundGateway"));

			try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
				applicationContext.registerBean(ObservationRegistry.class, () -> observationRegistry);
				applicationContext.register(ObservationIntegrationTestConfiguration.class);
				applicationContext.refresh();

				TestMessagingGatewaySupport messagingGateway =
						applicationContext.getBean("testInboundGateway", TestMessagingGatewaySupport.class);

				Message<?> receive = messagingGateway.process(new GenericMessage<>("test data"));

				assertThat(receive).isNotNull()
						.extracting("payload").isEqualTo("test data");
				var configuration = applicationContext.getBean(ObservationIntegrationTestConfiguration.class);

				messagingGateway =
						applicationContext.getBean("skippedObservationInboundGateway",
								TestMessagingGatewaySupport.class);

				receive = messagingGateway.process(new GenericMessage<>("void data"));

				assertThat(receive).isNull();

				assertThat(configuration.observedHandlerLatch.await(10, TimeUnit.SECONDS)).isTrue();
			}

			SpansAssert.assertThat(bb.getFinishedSpans())
					.haveSameTraceId()
					.hasASpanWithName("testInboundGateway process", spanAssert -> spanAssert
							.hasTag(IntegrationObservation.GatewayTags.COMPONENT_NAME.asString(), "testInboundGateway")
							.hasTag(IntegrationObservation.GatewayTags.COMPONENT_TYPE.asString(), "gateway")
							.hasTagWithKey("test.message.id")
							.hasKindEqualTo(Span.Kind.SERVER))
					.hasASpanWithName("observedEndpoint receive", spanAssert -> spanAssert
							.hasTag(IntegrationObservation.HandlerTags.COMPONENT_NAME.asString(), "observedEndpoint")
							.hasTag(IntegrationObservation.HandlerTags.COMPONENT_TYPE.asString(), "handler")
							.hasKindEqualTo(Span.Kind.CONSUMER))
					.hasASpanWithName("queueChannel send", spanAssert -> spanAssert
							.hasTag(IntegrationObservation.ProducerTags.COMPONENT_NAME.asString(), "queueChannel")
							.hasTag(IntegrationObservation.ProducerTags.COMPONENT_TYPE.asString(), "producer")
							.hasKindEqualTo(Span.Kind.PRODUCER))
					.hasSize(3);

			MeterRegistryAssert.assertThat(getMeterRegistry())
					.hasTimerWithNameAndTags("spring.integration.handler",
							KeyValues.of(
									IntegrationObservation.HandlerTags.COMPONENT_NAME.asString(), "observedEndpoint",
									IntegrationObservation.HandlerTags.COMPONENT_TYPE.asString(), "handler",
									"error", "none"));
		};
	}


	@Configuration
	@EnableIntegration
	@EnableIntegrationManagement(
			observationPatterns = {
					"${spring.integration.management.observation-patterns:testInboundGateway,skippedObservationInboundGateway,queueChannel,observedEndpoint}",
					"${spring.integration.management.observation-patterns:}"
			})
	public static class ObservationIntegrationTestConfiguration {

		CountDownLatch observedHandlerLatch = new CountDownLatch(1);

		@Bean
		@GlobalChannelInterceptor
		public ChannelInterceptor observationPropagationInterceptor(ObservationRegistry observationRegistry) {
			return new ObservationPropagationChannelInterceptor(observationRegistry);
		}

		@Bean
		TestMessagingGatewaySupport testInboundGateway(@Qualifier("queueChannel") PollableChannel queueChannel) {
			TestMessagingGatewaySupport messagingGatewaySupport = new TestMessagingGatewaySupport();
			messagingGatewaySupport.setObservationConvention(
					new DefaultMessageRequestReplyReceiverObservationConvention() {

						@Override
						public KeyValues getHighCardinalityKeyValues(MessageRequestReplyReceiverContext context) {
							return KeyValues.of("test.message.id", context.getCarrier().getHeaders().getId().toString());
						}

					});
			messagingGatewaySupport.setRequestChannel(queueChannel);
			return messagingGatewaySupport;
		}

		@Bean
		public PollableChannel queueChannel() {
			return new QueueChannel();
		}


		@Bean
		TestMessagingGatewaySupport skippedObservationInboundGateway() {
			TestMessagingGatewaySupport messagingGatewaySupport = new TestMessagingGatewaySupport();
			messagingGatewaySupport.setRequestChannel(new NullChannel());
			messagingGatewaySupport.setReplyTimeout(0);
			return messagingGatewaySupport;
		}

		@Bean
		@EndpointId("observedEndpoint")
		@ServiceActivator(inputChannel = "queueChannel",
				poller = @Poller(fixedDelay = "100"),
				adviceChain = "observedHandlerAdvice")
		BridgeHandler bridgeHandler() {
			return new BridgeHandler();
		}

		@Bean
		HandleMessageAdvice observedHandlerAdvice() {
			return invocation -> {
				try {
					return invocation.proceed();
				}
				finally {
					this.observedHandlerLatch.countDown();
				}
			};
		}

	}

	private static class TestMessagingGatewaySupport extends MessagingGatewaySupport {

		@Nullable
		Message<?> process(Message<?> request) {
			return sendAndReceiveMessage(request);
		}

	}

}
