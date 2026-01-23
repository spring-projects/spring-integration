/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.integration.dsl.reactivestreams;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.RetryingTest;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.endpoint.ReactiveMessageSourceProducer;
import org.springframework.integration.endpoint.ReactiveStreamsConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReactiveStreamsTests {

	@Autowired
	@Qualifier("reactiveFlow")
	private Publisher<Message<String>> publisher;

	@Autowired
	@Qualifier("pollableReactiveFlow")
	private Publisher<Message<Integer>> pollablePublisher;

	@Autowired
	private AbstractEndpoint reactiveTransformer;

	@Autowired
	@Qualifier("reactiveStreamsMessageSource")
	private Lifecycle messageSource;

	@Autowired
	@Qualifier("inputChannel")
	private MessageChannel inputChannel;

	@Autowired
	private IntegrationFlowContext integrationFlowContext;

	@Autowired
	private MessageChannel singleChannel;

	@Autowired
	private Publisher<Message<String>> singleChannelFlow;

	@Autowired
	private MessageChannel fixedSubscriberChannel;

	@Autowired
	private Publisher<Message<String>> fixedSubscriberChannelFlow;

	@Test
	void testReactiveFlow() throws Exception {
		assertThat(this.messageSource.isRunning()).isFalse();
		List<String> results = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(6);
		Disposable disposable =
				Flux.from(this.publisher)
						.map(m -> m.getPayload().toUpperCase())
						.subscribe(p -> {
							results.add(p);
							latch.countDown();
						});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		String[] strings = results.toArray(new String[0]);
		assertThat(strings).isEqualTo(new String[] {"A", "B", "C", "D", "E", "F"});

		disposable.dispose();
		assertThat(this.messageSource.isRunning()).isFalse();
		assertThat(TestUtils.<Long>getPropertyValue(this.messageSource, "messagingTemplate.sendTimeout"))
				.isEqualTo(256L);
	}

	@RetryingTest(10)
	void testPollableReactiveFlow() throws Exception {
		assertThat(this.reactiveTransformer).isInstanceOf(ReactiveStreamsConsumer.class);
		this.reactiveTransformer.setTaskScheduler(new SimpleAsyncTaskScheduler());
		this.inputChannel.send(new GenericMessage<>("1,2,3,4,5"));

		CountDownLatch latch = new CountDownLatch(6);

		Flux.from(this.pollablePublisher)
				.take(6)
				.filter(m -> m.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER))
				.doOnNext(p -> latch.countDown())
				.subscribe();

		ExecutorService exec = Executors.newSingleThreadExecutor();
		Future<List<Integer>> future =
				exec.submit(() ->
						Flux.just("11,12,13")
								.map(v -> v.split(","))
								.flatMapIterable(Arrays::asList)
								.map(Integer::parseInt)
								.<Message<Integer>>map(GenericMessage::new)
								.concatWith(this.pollablePublisher)
								.take(7)
								.map(Message::getPayload)
								.collectList()
								.block(Duration.ofSeconds(30))
				);

		this.inputChannel.send(new GenericMessage<>("6,7,8,9,10"));

		assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
		List<Integer> integers = future.get(30, TimeUnit.SECONDS);

		assertThat(integers).isNotNull();
		assertThat(integers.size()).isEqualTo(7);
		exec.shutdownNow();
	}

	@Test
	void testFromPublisher() {
		Flux<Message<?>> messageFlux =
				Flux.just("1,2,3,4")
						.map(v -> v.split(","))
						.flatMapIterable(Arrays::asList)
						.map(Integer::parseInt)
						.map(GenericMessage::new);

		QueueChannel resultChannel = new QueueChannel();

		IntegrationFlow integrationFlow =
				IntegrationFlow.from(messageFlux)
						.<Integer, Integer>transform(p -> p * 2)
						.channel(resultChannel)
						.get();

		this.integrationFlowContext.registration(integrationFlow)
				.register();

		for (int i = 0; i < 4; i++) {
			Message<?> receive = resultChannel.receive(10000);
			assertThat(receive).isNotNull();
			assertThat(receive.getPayload()).isEqualTo((i + 1) * 2);
		}
	}

	@Test
	void testFluxTransform() {
		QueueChannel resultChannel = new QueueChannel();

		IntegrationFlow integrationFlow = f -> f
				.splitWith((splitter) -> splitter.delimiters(","))
				.<String, String>fluxTransform(flux -> flux
						.map(Message::getPayload)
						.map(String::toUpperCase))
				.aggregate(a -> a
						.outputProcessor(group -> group
								.getMessages()
								.stream()
								.map(Message::getPayload)
								.map(String.class::cast)
								.collect(Collectors.joining(","))))
				.channel(resultChannel);

		IntegrationFlowContext.IntegrationFlowRegistration integrationFlowRegistration =
				this.integrationFlowContext
						.registration(integrationFlow)
						.register();

		MessageChannel inputChannel = integrationFlowRegistration.getInputChannel();
		inputChannel.send(new GenericMessage<>("a,b,c,d,e"));

		Message<?> receive = resultChannel.receive(10_000);

		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("A,B,C,D,E");

		integrationFlowRegistration.destroy();
	}

	@Test
	void singleChannelFlowTest() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		Flux.from(this.singleChannelFlow)
				.map(m -> m.getPayload().toUpperCase())
				.subscribe(p -> latch.countDown());
		this.singleChannel.send(new GenericMessage<>("foo"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void fixedSubscriberChannelFlowTest() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		Flux.from(this.fixedSubscriberChannelFlow)
				.map(m -> m.getPayload().toUpperCase())
				.subscribe(p -> latch.countDown());
		this.fixedSubscriberChannel.send(new GenericMessage<>("bar"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Autowired
	MessageProducerSupport testMessageProducer;

	@Autowired
	Publisher<Message<String>> messageProducerFlow;

	@Test
	void messageProducerIsNotStartedAutomatically() {
		assertThat(this.testMessageProducer.isRunning()).isFalse();

		Flux<String> flux =
				Flux.from(this.messageProducerFlow)
						.map(Message::getPayload);

		StepVerifier.create(flux)
				.expectNext("test")
				.expectNext("test")
				.thenCancel()
				.verify(Duration.ofSeconds(10));
	}

	@Autowired
	QueueChannel fromPublisherResult;

	@Autowired
	ConfigurableApplicationContext applicationContext;

	@Test
	void verifyFluxMessageChannelRestart() {
		for (long i = 0; i < 3L; i++) {
			assertThat(this.fromPublisherResult.receive(10_000)).extracting(Message::getPayload).isEqualTo(i);
		}

		this.applicationContext.stop();

		this.fromPublisherResult.purge(null);

		this.applicationContext.start();

		// The applicationContext restart causes all the endpoint to be started,
		// while we really don't have a subscription to this producer
		this.testMessageProducer.stop();

		for (long i = 0; i < 3L; i++) {
			assertThat(this.fromPublisherResult.receive(10_000)).extracting(Message::getPayload).isEqualTo(i);
		}
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		private final AtomicBoolean invoked = new AtomicBoolean();

		@Bean
		public Publisher<Message<String>> reactiveFlow() {
			return IntegrationFlow
					.from(() -> new GenericMessage<>("a,b,c,d,e,f"),
							e -> e.poller(p ->
											p.trigger(ctx ->
													this.invoked.getAndSet(true) ? null : Instant.now().plusMillis(1000)))
									.sendTimeout(256)
									.id("reactiveStreamsMessageSource"))
					.split(String.class, p -> p.split(","))
					.log()
					.toReactivePublisher(true);
		}

		@Bean
		public Publisher<Message<Integer>> pollableReactiveFlow() {
			return IntegrationFlow
					.from("inputChannel")
					.splitWith(s -> s.delimiters(","))
					.transformWith(t -> t
							.<String, Integer>transformer(Integer::parseInt)
							.reactive(flux -> flux.publishOn(Schedulers.parallel()))
							.id("reactiveTransformer"))
					.channel(MessageChannels.queue())
					.log()
					.toReactivePublisher();
		}

		@Bean
		public Publisher<Message<String>> singleChannelFlow() {
			return IntegrationFlow
					.from(MessageChannels.direct("singleChannel"))
					.log()
					.toReactivePublisher();
		}

		@Bean
		public Publisher<Message<String>> fixedSubscriberChannelFlow() {
			return IntegrationFlow
					.from("fixedSubscriberChannel", true)
					.log()
					.toReactivePublisher();
		}

		@Bean
		public Publisher<Message<String>> messageProducerFlow() {
			TestMessageProducerSpec testMessageProducerSpec =
					new TestMessageProducerSpec(new ReactiveMessageSourceProducer(() -> new GenericMessage<>("test")))
							.id("testMessageProducer");

			return IntegrationFlow
					.from(testMessageProducerSpec)
					.toReactivePublisher(true);
		}

		@Bean
		IntegrationFlow fromPublisher() {
			return IntegrationFlow.from(Flux.interval(Duration.ofMillis(100)).map(GenericMessage::new))
					.channel(c -> c.queue("fromPublisherResult"))
					.get();
		}

	}

	private static class TestMessageProducerSpec
			extends MessageProducerSpec<TestMessageProducerSpec, ReactiveMessageSourceProducer> {

		TestMessageProducerSpec(ReactiveMessageSourceProducer producer) {
			super(producer);
		}

	}

}
