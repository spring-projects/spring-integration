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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.test.simple.SimpleTracer;
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

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		SimpleTracer simpleTracer() {
			return new SimpleTracer();
		}

		@Bean
		ObservationRegistry observationRegistry(Tracer tracer) {
			TestObservationRegistry observationRegistry = TestObservationRegistry.create();
			observationRegistry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));
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

	}

}
