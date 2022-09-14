/*
 * Copyright 2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.EndpointId;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.ObservationPropagationChannelInterceptor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;

import io.micrometer.common.KeyValues;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.simple.SpansAssert;

/**
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class IntegrationObservabilityZipkinTests extends SampleTestRunner {

	@Override
	public TracingSetup[] getTracingSetup() {
		return new TracingSetup[]{ TracingSetup.IN_MEMORY_BRAVE, TracingSetup.ZIPKIN_BRAVE };
	}

	@Override
	public SampleTestRunnerConsumer yourCode() {
		return (bb, meterRegistry) -> {
			ObservationRegistry observationRegistry = getObservationRegistry();
			try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
				applicationContext.registerBean(ObservationRegistry.class, () -> observationRegistry);
				applicationContext.register(ObservationIntegrationTestConfiguration.class);
				applicationContext.refresh();

				PollableChannel queueChannel = applicationContext.getBean("queueChannel", PollableChannel.class);
				PollableChannel replyChannel = new QueueChannel();

				MutableMessage<String> testMessage =
						(MutableMessage<String>) MutableMessageBuilder.withPayload("test data")
								.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
								.build();

				Observation.createNotStarted("Test send", new MessageSenderContext(testMessage), observationRegistry)
						.observe(() -> queueChannel.send(testMessage));

				Message<?> receive = replyChannel.receive(10_000);
				assertThat(receive).isNotNull()
						.extracting("payload").isEqualTo("test data");
			}

			SpansAssert.assertThat(bb.getFinishedSpans())
					.haveSameTraceId()
					.hasASpanWithName("Test send", spanAssert -> spanAssert.hasKindEqualTo(Span.Kind.PRODUCER))
					.hasASpanWithName("observedEndpoint receive", spanAssert -> spanAssert
							.hasTag(IntegrationObservation.HandlerTags.COMPONENT_NAME.asString(), "observedEndpoint")
							.hasTag(IntegrationObservation.HandlerTags.COMPONENT_TYPE.asString(), "handler")
							.hasKindEqualTo(Span.Kind.CONSUMER))
					.hasSize(2);

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
	@EnableIntegrationManagement
	public static class ObservationIntegrationTestConfiguration {

		@Bean
		@GlobalChannelInterceptor
		public ChannelInterceptor observationPropagationInterceptor(ObservationRegistry observationRegistry) {
			return new ObservationPropagationChannelInterceptor(observationRegistry);
		}

		@Bean
		@BridgeTo(poller = @Poller(fixedDelay = "100"))
		@EndpointId("observedEndpoint")
		public PollableChannel queueChannel() {
			return new QueueChannel();
		}

	}

}
