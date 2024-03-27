/*
 * Copyright 2023-2024 the original author or authors.
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

package org.springframework.integration.webflux.observation;

import java.util.stream.Collectors;

import brave.Tracing;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.test.TestSpanHandler;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveFinishedSpan;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import io.micrometer.tracing.test.simple.SpansAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.webflux.dsl.WebFlux;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.reactive.server.HttpHandlerConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Artem Bilan
 *
 * @since 6.0.3
 */
@SpringJUnitWebConfig
@DirtiesContext
public class WebFluxObservationPropagationTests {

	private static final TestSpanHandler SPANS = new TestSpanHandler();

	@Autowired
	ObservationRegistry observationRegistry;

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private PollableChannel testChannel;

	@BeforeEach
	void setup() {
		SPANS.clear();
	}

	@Test
	void observationIsPropagatedFromWebFluxToServiceActivator() {
		String testData = "testData";

		this.webTestClient.post().uri("/test")
				.bodyValue(testData)
				.exchange()
				.expectStatus().isOk();

		Message<?> receive = this.testChannel.receive(10_000);
		assertThat(receive).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("Received data: " + testData);

		// There is a race condition when we already have a reply, but the span in the last channel is not closed yet.
		await().untilAsserted(() -> assertThat(SPANS.spans()).hasSize(6));
		SpansAssert.assertThat(SPANS.spans().stream().map(BraveFinishedSpan::fromBrave).collect(Collectors.toList()))
				.haveSameTraceId();
	}

	@Test
	void observationIsPropagatedWebFluxRequestReply() {
		String testData = "TESTDATA";

		this.webTestClient.get().uri("/testRequestReply?name=" + testData)
				.headers(headers -> headers.setBasicAuth("guest", "guest"))
				.exchange()
				.expectBody(String.class)
				.isEqualTo(testData.toLowerCase());

		// There is a race condition when we already have a reply, but the span in the last channel is not closed yet.
		await().untilAsserted(() -> assertThat(SPANS.spans()).hasSize(3));
		SpansAssert.assertThat(SPANS.spans().stream().map(BraveFinishedSpan::fromBrave).collect(Collectors.toList()))
				.haveSameTraceId();
	}

	@Configuration
	@EnableWebFlux
	@EnableIntegration
	@EnableIntegrationManagement(observationPatterns = "*")
	public static class ContextConfiguration {

		@Bean
		public Tracing braveTracing() {
			return Tracing.newBuilder().addSpanHandler(SPANS).build();
		}

		@Bean
		Tracer simpleTracer(Tracing tracing) {
			return new BraveTracer(tracing.tracer(),
					new BraveCurrentTraceContext(ThreadLocalCurrentTraceContext.create()),
					new BraveBaggageManager());
		}

		@Bean
		BravePropagator bravePropagator(Tracing tracing) {
			return new BravePropagator(tracing);
		}

		@Bean
		ObservationRegistry observationRegistry(Tracer tracer, Propagator propagator) {
			ObservationRegistry observationRegistry = ObservationRegistry.create();
			observationRegistry.observationConfig()
					.observationHandler(
							// Composite will pick the first matching handler
							new ObservationHandler.FirstMatchingCompositeObservationHandler(
									// This is responsible for creating a child span on the sender side
									new PropagatingSenderTracingObservationHandler<>(tracer, propagator),
									// This is responsible for creating a span on the receiver side
									new PropagatingReceiverTracingObservationHandler<>(tracer, propagator),
									// This is responsible for creating a default span
									new DefaultTracingObservationHandler(tracer)));
			return observationRegistry;
		}

		@Bean
		WebTestClient webTestClient(ObservationRegistry registry, ApplicationContext applicationContext) {
			HttpHandler httpHandler =
					WebHttpHandlerBuilder.applicationContext(applicationContext)
							.observationRegistry(registry)
							.build();
			return WebTestClient.bindToServer(new HttpHandlerConnector(httpHandler)).build();
		}

		@Bean
		IntegrationFlow webFluxFlow() {
			return IntegrationFlow
					.from(WebFlux.inboundChannelAdapter("/test")
							.requestMapping((mapping) -> mapping.methods(HttpMethod.POST))
							.requestPayloadType(String.class)
							.id("webFluxInbound"))
					.channel(c -> c.flux("requestChannel"))
					.<String>handle((p, h) -> "Received data: " + p, e -> e.id("testHandler"))
					.channel(c -> c.queue("testChannel"))
					.get();
		}

		@Bean
		FluxMessageChannel webFluxRequestChannel() {
			return new FluxMessageChannel();
		}

		@Bean
		IntegrationFlow webFluxRequestReplyFlow(
				@Qualifier("webFluxRequestChannel") FluxMessageChannel webFluxRequestChannel) {

			return IntegrationFlow.from(WebFlux.inboundGateway("/testRequestReply")
							.requestMapping(r -> r.params("name"))
							.payloadExpression("#requestParams.name[0]")
							.requestChannel(webFluxRequestChannel)
							.id("webFluxGateway"))
					.transformWith(t -> t
							.<String, String>transformer(String::toLowerCase)
							.id("testTransformer"))
					.get();
		}

	}

}
