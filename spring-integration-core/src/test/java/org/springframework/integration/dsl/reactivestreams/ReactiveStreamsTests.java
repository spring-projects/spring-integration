/*
 * Copyright 2016-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.ReactiveStreamsConsumer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;


/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
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
		List<String> results = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(6);
		Flux.from(this.publisher)
				.map(m -> m.getPayload().toUpperCase())
				.subscribe(p -> {
					results.add(p);
					latch.countDown();
				});
		this.messageSource.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		String[] strings = results.toArray(new String[0]);
		assertThat(strings).isEqualTo(new String[]{ "A", "B", "C", "D", "E", "F" });
		this.messageSource.stop();
	}

	@Test
	void testPollableReactiveFlow() throws Exception {
		assertThat(this.reactiveTransformer).isInstanceOf(ReactiveStreamsConsumer.class);
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
								.block(Duration.ofSeconds(10))
				);

		this.inputChannel.send(new GenericMessage<>("6,7,8,9,10"));

		assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
		List<Integer> integers = future.get(20, TimeUnit.SECONDS);

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
				IntegrationFlows.from(messageFlux)
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
				.split((splitter) -> splitter.delimiters(","))
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

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		private final AtomicBoolean invoked = new AtomicBoolean();

		@Bean
		public Publisher<Message<String>> reactiveFlow() {
			return IntegrationFlows
					.from(() -> new GenericMessage<>("a,b,c,d,e,f"),
							e -> e.poller(p -> p.trigger(ctx -> this.invoked.getAndSet(true) ? null : new Date()))
									.autoStartup(false)
									.id("reactiveStreamsMessageSource"))
					.split(String.class, p -> p.split(","))
					.log()
					.toReactivePublisher();
		}

		@Bean
		public Publisher<Message<Integer>> pollableReactiveFlow() {
			return IntegrationFlows
					.from("inputChannel")
					.split(s -> s.delimiters(","))
					.<String, Integer>transform(Integer::parseInt,
							e -> e.reactive(flux -> flux.publishOn(Schedulers.parallel())).id("reactiveTransformer"))
					.channel(MessageChannels.queue())
					.log()
					.toReactivePublisher();
		}

		@Bean
		public Publisher<Message<String>> singleChannelFlow() {
			return IntegrationFlows
					.from(MessageChannels.direct("singleChannel"))
					.log()
					.toReactivePublisher();
		}

		@Bean
		public Publisher<Message<String>> fixedSubscriberChannelFlow() {
			return IntegrationFlows
					.from("fixedSubscriberChannel", true)
					.log()
					.toReactivePublisher();
		}

	}

}
