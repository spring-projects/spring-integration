/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl.reactivestreams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import java.util.logging.Level;

import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import reactor.core.publisher.Flux;


/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class ReactiveStreamsTests {

	@Autowired
	@Qualifier("reactiveFlow")
	private Publisher<Message<String>> publisher;

	@Autowired
	@Qualifier("pollableReactiveFlow")
	private Publisher<Message<Integer>> pollablePublisher;

	@Autowired
	@Qualifier("reactiveStreamsMessageSource")
	private Lifecycle messageSource;

	@Autowired
	@Qualifier("inputChannel")
	private MessageChannel inputChannel;

	@Autowired
	private IntegrationFlowContext integrationFlowContext;

	@Test
	public void testReactiveFlow() throws Exception {
		List<String> results = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(6);
		Flux.from(this.publisher)
				.map(m -> m.getPayload().toUpperCase())
				.subscribe(p -> {
					results.add(p);
					latch.countDown();
				});
		this.messageSource.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		String[] strings = results.toArray(new String[results.size()]);
		assertArrayEquals(new String[] {"A", "B", "C", "D", "E", "F"}, strings);
		this.messageSource.stop();
	}

	@Test
	public void testPollableReactiveFlow() throws Exception {
		this.inputChannel.send(new GenericMessage<>("1,2,3,4,5"));

		CountDownLatch latch = new CountDownLatch(6);

		Flux.from(this.pollablePublisher)
				.take(6)
				.filter(m -> m.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER))
				.log("org.springframework.integration.flux2", Level.WARNING)
				.doOnNext(p -> latch.countDown())
				.subscribe();

		ExecutorService exec = Executors.newSingleThreadExecutor();
		Future<List<Integer>> future =
				exec.submit(() ->
						Flux.just("11,12,13")
								.map(v -> v.split(","))
								.flatMapIterable(Arrays::asList)
								.map(Integer::parseInt)
								.<Message<Integer>>map(GenericMessage<Integer>::new)
								.concatWith(this.pollablePublisher)
								.take(7)
								.map(Message::getPayload)
								.log("org.springframework.integration.flux", Level.WARNING)
								.collectList()
								.block(Duration.ofSeconds(10))
				);

		this.inputChannel.send(new GenericMessage<>("6,7,8,9,10"));

		assertTrue(latch.await(20, TimeUnit.SECONDS));
		List<Integer> integers = future.get(20, TimeUnit.SECONDS);

		assertNotNull(integers);
		assertEquals(7, integers.size());
		exec.shutdownNow();
	}

	@Test
	public void testFromPublisher() {
		Flux<Message<?>> messageFlux = Flux.just("1,2,3,4")
				.map(v -> v.split(","))
				.flatMapIterable(Arrays::asList)
				.map(Integer::parseInt)
				.log("org.springframework.integration.flux")
				.map(GenericMessage<Integer>::new);

		QueueChannel resultChannel = new QueueChannel();

		IntegrationFlow integrationFlow =
				IntegrationFlows.from(messageFlux)
						.log("org.springframework.integration.flux2")
						.<Integer, Integer>transform(p -> p * 2)
						.channel(resultChannel)
						.get();

		this.integrationFlowContext.registration(integrationFlow)
				.register();

		for (int i = 0; i < 4; i++) {
			Message<?> receive = resultChannel.receive(10000);
			assertNotNull(receive);
			assertEquals((i + 1) * 2, receive.getPayload());
		}
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
					.toReactivePublisher();
		}

		@Bean
		public Publisher<Message<Integer>> pollableReactiveFlow() {
			return IntegrationFlows
					.from("inputChannel")
					.split(s -> s.delimiters(","))
					.<String, Integer>transform(Integer::parseInt)
					.channel(MessageChannels.queue())
					.toReactivePublisher();
		}

	}

}
