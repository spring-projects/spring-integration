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

package org.springframework.integration.dsl.correlation;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.MessageTriggerAction;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class CorrelationHandlerTests {

	private static final String BARRIER = "barrier";

	@Autowired
	@Qualifier("splitResequenceFlow.input")
	private MessageChannel splitInput;


	@Autowired
	@Qualifier("splitAggregateInput")
	private MessageChannel splitAggregateInput;

	@Autowired
	@Qualifier("publishSubscribeFlow.input")
	private MessageChannel subscriberAggregateFlowInput;

	@Autowired
	private PollableChannel subscriberAggregateResult;

	@Autowired
	@Qualifier("barrierFlow.input")
	private MessageChannel barrierFlowInput;

	@Autowired
	private PollableChannel barrierResults;

	@Autowired
	private PollableChannel releaseChannel;

	@Test
	public void testSplitterResequencer() {
		QueueChannel replyChannel = new QueueChannel();

		this.splitInput.send(MessageBuilder.withPayload("")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build());

		for (int i = 0; i < 12; i++) {
			Message<?> receive = replyChannel.receive(2000);
			assertNotNull(receive);
			assertFalse(receive.getHeaders().containsKey("foo"));
			assertTrue(receive.getHeaders().containsKey("FOO"));
			assertEquals("BAR", receive.getHeaders().get("FOO"));
			assertEquals(i + 1, receive.getPayload());
		}
	}

	@Test
	public void testSplitterAggregator() {
		List<Character> payload = Arrays.asList('a', 'b', 'c', 'd', 'e');

		QueueChannel replyChannel = new QueueChannel();
		this.splitAggregateInput.send(MessageBuilder.withPayload(payload)
				.setReplyChannel(replyChannel)
				.build());

		Message<?> receive = replyChannel.receive(2000);
		assertNotNull(receive);
		assertThat(receive.getPayload(), instanceOf(List.class));
		@SuppressWarnings("unchecked")
		List<Object> result = (List<Object>) receive.getPayload();
		for (int i = 0; i < payload.size(); i++) {
			assertEquals(payload.get(i), result.get(i));
		}
	}

	@Test
	public void testSubscriberAggregateFlow() {
		this.subscriberAggregateFlowInput.send(new GenericMessage<>("test"));

		Message<?> receive1 = this.subscriberAggregateResult.receive(10000);
		assertNotNull(receive1);
		assertEquals("Hello World!", receive1.getPayload());
	}


	@Test
	public void testBarrier() {
		Message<?> releasing = MessageBuilder.withPayload("bar").setHeader(BARRIER, "foo").build();
		this.releaseChannel.send(releasing);
		Message<?> suspending = MessageBuilder.withPayload("foo").setHeader(BARRIER, "foo").build();
		this.barrierFlowInput.send(suspending);
		Message<?> out = this.barrierResults.receive(10000);
		assertNotNull(out);
		assertEquals("bar", out.getPayload());
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public Executor taskExecutor() {
			ThreadPoolTaskExecutor tpte = new ThreadPoolTaskExecutor();
			tpte.setCorePoolSize(50);
			return tpte;
		}

		@Bean
		public TestSplitterPojo testSplitterData() {
			List<String> first = new ArrayList<>();
			first.add("1,2,3");
			first.add("4,5,6");

			List<String> second = new ArrayList<>();
			second.add("7,8,9");
			second.add("10,11,12");

			return new TestSplitterPojo(first, second);
		}

		@Bean
		public MessageChannelSpec<?, ?> executorChannel() {
			return MessageChannels.executor(taskExecutor());
		}

		@Bean
		@SuppressWarnings("rawtypes")
		public IntegrationFlow splitResequenceFlow(MessageChannel executorChannel) {
			return f -> f.enrichHeaders(s -> s.header("FOO", "BAR"))
					.split("testSplitterData", "buildList", c -> c.applySequence(false))
					.channel(executorChannel)
					.split(Message.class, Message::getPayload, c -> c.applySequence(false))
					.channel(MessageChannels.executor(taskExecutor()))
					.split(s -> s
							.applySequence(false)
							.delimiters(","))
					.channel(MessageChannels.executor(taskExecutor()))
					.<String, Integer>transform(Integer::parseInt)
					.enrichHeaders(h ->
							h.headerFunction(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Message::getPayload))
					.resequence(r -> r.releasePartialSequences(true).correlationExpression("'foo'"))
					.headerFilter("foo", false);
		}


		@Bean
		public IntegrationFlow splitAggregateFlow() {
			return IntegrationFlows.from("splitAggregateInput", true)
					.split()
					.channel(MessageChannels.executor(taskExecutor()))
					.resequence()
					.aggregate()
					.get();
		}

		@Bean
		public IntegrationFlow publishSubscribeFlow() {
			return flow -> flow
					.publishSubscribeChannel(s -> s
							.applySequence(true)
							.subscribe(f -> f
									.handle((p, h) -> "Hello")
									.channel("publishSubscribeAggregateFlow.input"))
							.subscribe(f -> f
									.handle((p, h) -> "World!")
									.channel("publishSubscribeAggregateFlow.input"))
					);
		}

		@Bean
		public IntegrationFlow publishSubscribeAggregateFlow() {
			return flow -> flow
					.aggregate(a -> a.outputProcessor(g -> g.getMessages()
							.stream()
							.map(m -> (String) m.getPayload())
							.collect(Collectors.joining(" "))))
					.channel(MessageChannels.queue("subscriberAggregateResult"));
		}

		@Bean
		public MessageChannelSpec<?, ?> barrierResults() {
			return MessageChannels.queue("barrierResults");
		}

		@Bean
		public IntegrationFlow barrierFlow() {
			return f -> f
					.barrier(10000, b -> b
							.correlationStrategy(new HeaderAttributeCorrelationStrategy(BARRIER))
							.outputProcessor(g ->
									g.getMessages()
											.stream()
											.skip(1)
											.findFirst()
											.get()))
					.channel("barrierResults");
		}

		@Bean
		public MessageChannelSpec<?, ?> releaseChannel() {
			return MessageChannels.queue("releaseChannel");
		}

		@Bean
		@DependsOn("barrierFlow")
		public IntegrationFlow releaseBarrierFlow(MessageTriggerAction barrierTriggerAction) {
			return IntegrationFlows.from(releaseChannel())
					.trigger(barrierTriggerAction,
							e -> e.poller(p -> p.fixedDelay(100)))
					.get();
		}

	}

	private static final class TestSplitterPojo {

		final List<String> first;

		final List<String> second;

		TestSplitterPojo(List<String> first, List<String> second) {
			this.first = first;
			this.second = second;
		}

		@SuppressWarnings("unused")
		public List<String> getFirst() {
			return first;
		}

		@SuppressWarnings("unused")
		public List<String> getSecond() {
			return second;
		}

		@SuppressWarnings("unused")
		public List<List<String>> buildList() {
			return Arrays.asList(this.first, this.second);
		}

	}

}
