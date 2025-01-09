/*
 * Copyright 2025 the original author or authors.
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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.simple.SpansAssert;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.EndpointId;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.integration.transaction.DefaultTransactionSynchronizationFactory;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.integration.transaction.TransactionInterceptorBuilder;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Artem Bilan
 *
 * @since 6.5
 */
public class SourcePollingChannelAdapterObservationTests extends SampleTestRunner {

	@Override
	public TracingSetup[] getTracingSetup() {
		return new TracingSetup[] {TracingSetup.IN_MEMORY_BRAVE};
	}

	@Override
	public SampleTestRunnerConsumer yourCode() {
		return (bb, meterRegistry) -> {
			ObservationRegistry observationRegistry = getObservationRegistry();

			try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
				applicationContext.registerBean(ObservationRegistry.class, () -> observationRegistry);
				applicationContext.register(ObservationIntegrationTestConfiguration.class);
				applicationContext.refresh();

				var testConfiguration = applicationContext.getBean(ObservationIntegrationTestConfiguration.class);

				assertThat(testConfiguration.transactionLatch.await(10, TimeUnit.SECONDS)).isTrue();
			}

			await().untilAsserted(() -> assertThat(bb.getFinishedSpans()).hasSize(5));

			SpansAssert.assertThat(bb.getFinishedSpans())
					.haveSameTraceId()
					.hasASpanWithName("dataMessageSource receive", spanAssert -> spanAssert
							.hasTag(IntegrationObservation.GatewayTags.COMPONENT_TYPE.asString(), "message-source")
							.hasKindEqualTo(Span.Kind.CONSUMER))
					.hasASpanWithName("processMessage send", spanAssert -> spanAssert
							.hasTag(IntegrationObservation.ProducerTags.COMPONENT_NAME.asString(), "processMessage")
							.hasTag(IntegrationObservation.ProducerTags.COMPONENT_TYPE.asString(), "producer")
							.hasKindEqualTo(Span.Kind.PRODUCER))
					.hasASpanWithName("dataHandler receive", spanAssert -> spanAssert
							.hasTag(IntegrationObservation.HandlerTags.COMPONENT_NAME.asString(), "dataHandler")
							.hasTag(IntegrationObservation.HandlerTags.COMPONENT_TYPE.asString(), "handler")
							.hasKindEqualTo(Span.Kind.CONSUMER))
					.hasASpanWithName("afterCommit send", spanAssert -> spanAssert
							.hasTag(IntegrationObservation.ProducerTags.COMPONENT_NAME.asString(), "afterCommit")
							.hasTag(IntegrationObservation.ProducerTags.COMPONENT_TYPE.asString(), "producer")
							.hasKindEqualTo(Span.Kind.PRODUCER))
					.hasASpanWithName("commitHandler receive", spanAssert -> spanAssert
							.hasTag(IntegrationObservation.HandlerTags.COMPONENT_NAME.asString(), "commitHandler")
							.hasTag(IntegrationObservation.HandlerTags.COMPONENT_TYPE.asString(), "handler")
							.hasKindEqualTo(Span.Kind.CONSUMER));
		};
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationManagement(observationPatterns = "*")
	public static class ObservationIntegrationTestConfiguration {

		final CountDownLatch transactionLatch = new CountDownLatch(1);

		@Bean
		MessageChannel afterCommit() {
			return new DirectChannel();
		}

		@Bean
		TransactionSynchronizationFactory testTransactionSynchronizationFactory(MessageChannel afterCommit) {
			var processor = new ExpressionEvaluatingTransactionSynchronizationProcessor();
			processor.setAfterCommitChannel(afterCommit);
			return new DefaultTransactionSynchronizationFactory(processor);
		}

		@Bean
		PollerMetadata pollerMetadata(TransactionSynchronizationFactory testTransactionSynchronizationFactory) {
			PollerMetadata pollerMetadata = new PollerMetadata();
			pollerMetadata.setTrigger(new OnlyOnceTrigger());
			TransactionInterceptor transactionInterceptor =
					new TransactionInterceptorBuilder()
							.transactionManager(new PseudoTransactionManager())
							.build();
			pollerMetadata.setAdviceChain(List.of(transactionInterceptor));
			pollerMetadata.setTransactionSynchronizationFactory(testTransactionSynchronizationFactory);
			return pollerMetadata;
		}

		@EndpointId("dataMessageSource")
		@InboundChannelAdapter(channel = "processMessage", poller = @Poller("pollerMetadata"))
		String emitData() {
			return "some data";
		}

		@EndpointId("dataHandler")
		@ServiceActivator(inputChannel = "processMessage")
		void processData(String data) {

		}

		@EndpointId("commitHandler")
		@ServiceActivator(inputChannel = "afterCommit")
		void afterCommit(String data) {
			transactionLatch.countDown();
		}

	}

}
