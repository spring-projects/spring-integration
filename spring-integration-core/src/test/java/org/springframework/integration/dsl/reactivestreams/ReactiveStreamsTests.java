/*
 * Copyright 2016 the original author or authors.
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Level;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.test.rule.Log4jLevelAdjuster;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import reactor.core.publisher.Flux;


/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class ReactiveStreamsTests {

	@Rule
	public Log4jLevelAdjuster adjuster = new Log4jLevelAdjuster(Level.DEBUG, "org.springframework.integration");

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
		assertArrayEquals(new String[] { "A", "B", "C", "D", "E", "F" }, strings);
		this.messageSource.stop();
	}

	@Test
	public void testPollableReactiveFlow() throws Exception {
		this.inputChannel.send(new GenericMessage<>("1,2,3,4,5"));

		CountDownLatch warmUpLatch = new CountDownLatch(3);
		CountDownLatch latch = new CountDownLatch(6);

		Flux.from(this.pollablePublisher)
				.filter(m -> m.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER))
				.doOnNext(p -> latch.countDown())
				.doOnNext(p -> warmUpLatch.countDown())
				.take(6)
				.subscribe();

		Future<List<Integer>> future =
				Executors.newSingleThreadExecutor().submit(() ->
						Flux.just("11,12,13")
								.map(v -> v.split(","))
								.flatMapIterable(Arrays::asList)
								.map(Integer::parseInt)
								.<Message<Integer>>map(GenericMessage<Integer>::new)
								.concatWith(this.pollablePublisher)
								.map(Message::getPayload)
								.take(7)
								.log("org.springframework.integration.flux")
								.collectList()
								.block(Duration.ofSeconds(10)));

		assertTrue(warmUpLatch.await(10, TimeUnit.SECONDS));

		this.inputChannel.send(new GenericMessage<>("6,7,8,9,10"));

		assertTrue(latch.await(10, TimeUnit.SECONDS));
		List<Integer> integers = future.get(20, TimeUnit.SECONDS);

		assertNotNull(integers);
		assertEquals(7, integers.size());
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
