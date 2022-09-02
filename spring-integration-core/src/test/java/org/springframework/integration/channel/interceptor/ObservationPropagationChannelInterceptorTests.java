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

package org.springframework.integration.channel.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.support.management.IntegrationManagement;
import org.springframework.integration.support.management.observation.MessageSenderContext;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import io.micrometer.tracing.test.simple.SimpleTracer;
import io.micrometer.tracing.test.simple.SpansAssert;
import io.micrometer.tracing.test.simple.TracerAssert;

/**
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig
public class ObservationPropagationChannelInterceptorTests {

	@Autowired
	ObservationRegistry observationRegistry;

	@Autowired
	SimpleTracer simpleTracer;

	@Autowired
	SubscribableChannel directChannel;

	@Autowired
	SubscribableChannel executorChannel;

	@Autowired
	PollableChannel queueChannel;

	@Autowired
	DirectChannel testConsumer;

	@Autowired
	ExecutorChannel testTracingChannel;

	@BeforeEach
	void setup() {
		this.simpleTracer.getSpans().clear();
	}

	@Test
	void observationPropagatedOverDirectChannel() throws InterruptedException {
		AtomicReference<Observation.Scope> scopeReference = new AtomicReference<>();
		CountDownLatch handleLatch = new CountDownLatch(1);
		this.directChannel.subscribe(m -> {
			scopeReference.set(this.observationRegistry.getCurrentObservationScope());
			handleLatch.countDown();
		});

		AtomicReference<Observation.Scope> originalScope = new AtomicReference<>();

		Observation.createNotStarted("test1", this.observationRegistry)
				.observe(() -> {
					originalScope.set(this.observationRegistry.getCurrentObservationScope());
					this.directChannel.send(new GenericMessage<>("test"));
				});

		assertThat(handleLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(scopeReference.get())
				.isNotNull()
				.isSameAs(originalScope.get());

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.doesNotHaveAnyRemainingCurrentObservation();

		TracerAssert.assertThat(this.simpleTracer)
				.onlySpan()
				.hasNameEqualTo("test1");
	}

	@Test
	void observationPropagatedOverExecutorChannel() throws InterruptedException {
		AtomicReference<Observation.Scope> scopeReference = new AtomicReference<>();
		CountDownLatch handleLatch = new CountDownLatch(1);
		this.executorChannel.subscribe(m -> {
			scopeReference.set(this.observationRegistry.getCurrentObservationScope());
			handleLatch.countDown();
		});

		AtomicReference<Observation.Scope> originalScope = new AtomicReference<>();

		Observation.createNotStarted("test2", this.observationRegistry)
				.observe(() -> {
					originalScope.set(this.observationRegistry.getCurrentObservationScope());
					this.executorChannel.send(new GenericMessage<>("test"));
				});

		assertThat(handleLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(scopeReference.get())
				.isNotNull()
				.isNotSameAs(originalScope.get());

		assertThat(scopeReference.get().getCurrentObservation())
				.isSameAs(originalScope.get().getCurrentObservation());

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.doesNotHaveAnyRemainingCurrentObservation();

		TracerAssert.assertThat(this.simpleTracer)
				.onlySpan()
				.hasNameEqualTo("test2");
	}

	@Test
	void observationPropagatedOverQueueChannel() throws InterruptedException {
		AtomicReference<Observation.Scope> scopeReference = new AtomicReference<>();
		CountDownLatch handleLatch = new CountDownLatch(1);
		this.testConsumer.subscribe(m -> {
			scopeReference.set(this.observationRegistry.getCurrentObservationScope());
			handleLatch.countDown();
		});

		AtomicReference<Observation.Scope> originalScope = new AtomicReference<>();

		Observation.createNotStarted("test3", this.observationRegistry)
				.observe(() -> {
					originalScope.set(this.observationRegistry.getCurrentObservationScope());
					this.queueChannel.send(new GenericMessage<>("test"));
				});

		assertThat(handleLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(scopeReference.get())
				.isNotNull()
				.isNotSameAs(originalScope.get());

		assertThat(scopeReference.get().getCurrentObservation())
				.isSameAs(originalScope.get().getCurrentObservation());

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.doesNotHaveAnyRemainingCurrentObservation();

		TracerAssert.assertThat(this.simpleTracer)
				.onlySpan()
				.hasNameEqualTo("test3");
	}

	@Test
	void observationContextPropagatedOverDirectChannel() {
		BridgeHandler handler = new BridgeHandler();
		handler.registerObservationRegistry(this.observationRegistry);
		handler.setBeanName("testBridge");
		this.testTracingChannel.subscribe(handler);

		QueueChannel replyChannel = new QueueChannel();

		MutableMessage<String> message =
				(MutableMessage<String>) MutableMessageBuilder.withPayload("test")
						.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
						.build();

		Observation.createNotStarted("sending", new MessageSenderContext(message), this.observationRegistry)
				.observe(() -> this.testTracingChannel.send(message));

		Message<?> receive = replyChannel.receive();

		assertThat(receive).isNotNull()
				.extracting(Message::getHeaders)
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsEntry("foo", "some foo value")
				.containsEntry("bar", "some bar value");

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.doesNotHaveAnyRemainingCurrentObservation();

		TracerAssert.assertThat(this.simpleTracer)
				.reportedSpans()
				.hasSize(2)
				.satisfies(simpleSpans -> assertSpans(simpleSpans)
						.hasASpanWithName("sending")
						.assertThatASpanWithNameEqualTo(IntegrationManagement.CONSUME_OBSERVATION_NAME)
						.hasTag("foo", "some foo value")
						.hasTag("bar", "some bar value")
						.hasTag("type", "handler")
						.hasTag("name", "testBridge")
						.hasKindEqualTo(Span.Kind.CONSUMER));
	}

	@SuppressWarnings("unchecked")
	private static SpansAssert assertSpans(Collection<? extends FinishedSpan> actual) {
		return SpansAssert.assertThat((Collection<FinishedSpan>) actual);
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		SimpleTracer simpleTracer() {
			return new SimpleTracer();
		}

		@Bean
		ObservationRegistry observationRegistry(Tracer tracer, Propagator propagator) {
			TestObservationRegistry observationRegistry = TestObservationRegistry.create();
			observationRegistry.observationConfig().observationHandler(
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
		@GlobalChannelInterceptor(patterns = "*Channel")
		public ChannelInterceptor observationPropagationInterceptor(ObservationRegistry observationRegistry) {
			return new ObservationPropagationChannelInterceptor(observationRegistry);
		}

		@Bean
		@BridgeTo(value = "testConsumer", poller = @Poller(fixedDelay = "100"))
		public PollableChannel queueChannel() {
			return new QueueChannel();
		}

		@Bean
		public SubscribableChannel executorChannel() {
			return new ExecutorChannel(Executors.newSingleThreadExecutor());
		}

		@Bean
		public SubscribableChannel directChannel() {
			return new DirectChannel();
		}

		@Bean
		public DirectChannel testConsumer() {
			return new DirectChannel();
		}

		@Bean
		public ExecutorChannel testTracingChannel() {
			return new ExecutorChannel(Executors.newSingleThreadExecutor());
		}

		@Bean
		public Propagator propagator(Tracer tracer) {
			return new Propagator() {

				// List of headers required for tracing propagation
				@Override
				public List<String> fields() {
					return Arrays.asList("foo", "bar");
				}

				// This is called on the producer side when the message is being sent
				// Normally we would pass information from tracing context - for tests we don't need to
				@Override
				public <C> void inject(TraceContext context, @Nullable C carrier, Setter<C> setter) {
					setter.set(carrier, "foo", "some foo value");
					setter.set(carrier, "bar", "some bar value");
				}

				// This is called on the consumer side when the message is consumed
				// Normally we would use tools like Extractor from tracing but for tests we are just manually creating a span
				@Override
				public <C> Span.Builder extract(C carrier, Getter<C> getter) {
					String foo = getter.get(carrier, "foo");
					String bar = getter.get(carrier, "bar");
					return tracer.spanBuilder().tag("foo", foo).tag("bar", bar);
				}
			};
		}

	}

}
