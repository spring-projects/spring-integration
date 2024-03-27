/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.dsl.correlation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.FluxAggregatorMessageHandler;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.handler.MessageTriggerAction;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class CorrelationHandlerTests {

	private static final String BARRIER = "barrier";

	@Autowired
	@Qualifier("splitResequenceFlow.input")
	private MessageChannel splitInput;

	@Autowired
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

	@Autowired
	private PollableChannel discardChannel;

	@Test
	public void testSplitterResequencer() {
		QueueChannel replyChannel = new QueueChannel();

		this.splitInput.send(MessageBuilder.withPayload("")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build());

		for (int i = 0; i < 12; i++) {
			Message<?> receive = replyChannel.receive(2000);
			assertThat(receive).isNotNull();
			assertThat(receive.getHeaders().containsKey("foo")).isFalse();
			assertThat(receive.getHeaders().containsKey("FOO")).isTrue();
			assertThat(receive.getHeaders().get("FOO")).isEqualTo("BAR");
			assertThat(receive.getPayload()).isEqualTo(i + 1);
		}
	}

	@Test
	public void testSplitterAggregator() {
		List<String> payload = Arrays.asList("a", "b", "c", "d", "e");

		QueueChannel replyChannel = new QueueChannel();
		this.splitAggregateInput.send(MessageBuilder.withPayload(payload)
				.setReplyChannel(replyChannel)
				.build());

		Message<?> receive = replyChannel.receive(2000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<Object> result = (List<Object>) receive.getPayload();
		for (int i = 0; i < payload.size(); i++) {
			assertThat(result.get(i)).isInstanceOf(TextNode.class);
			assertThat(result.get(i)).isEqualTo(TextNode.valueOf(payload.get(i)));
		}
	}

	@Test
	public void testSubscriberAggregateFlow() {
		this.subscriberAggregateFlowInput.send(new GenericMessage<>("test"));

		Message<?> receive = this.subscriberAggregateResult.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("Hello World!");
		assertThat(receive.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	public void testBarrier() {
		Message<?> releasing = MessageBuilder.withPayload("bar").setHeader(BARRIER, "foo").build();
		this.releaseChannel.send(releasing);
		Message<?> suspending = MessageBuilder.withPayload("foo").setHeader(BARRIER, "foo").build();
		this.barrierFlowInput.send(suspending);
		Message<?> out = this.barrierResults.receive(10000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo("bar");
	}

	@Test
	public void testSplitterDiscard() {
		this.splitAggregateInput.send(new GenericMessage<>(new ArrayList<>()));
		Message<?> receive = this.discardChannel.receive(10_000);
		assertThat(receive)
				.isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(ArrayNode.class)
				.extracting(new String[] {"_children"})
				.element(0)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.hasSize(0);
	}

	@Autowired
	private IntegrationFlowContext integrationFlowContext;

	@Test
	public void testFluxAggregator() {
		IntegrationFlow testFlow =
				(flow) -> flow
						.split()
						.channel(MessageChannels.flux())
						.handle(new FluxAggregatorMessageHandler());

		IntegrationFlowContext.IntegrationFlowRegistration registration =
				this.integrationFlowContext.registration(testFlow)
						.register();

		@SuppressWarnings("unchecked")
		Flux<Message<?>> window =
				registration.getMessagingTemplate()
						.convertSendAndReceive(new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, Flux.class);

		assertThat(window).isNotNull();

		StepVerifier.create(
						window.map(Message::getPayload)
								.cast(Integer.class))
				.expectNextSequence(IntStream.range(0, 10).boxed().collect(Collectors.toList()))
				.verifyComplete();

		registration.destroy();
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		TestSplitterPojo testSplitterData() {
			List<String> first = new ArrayList<>();
			first.add("1,2,3");
			first.add("4,5,6");

			List<String> second = new ArrayList<>();
			second.add("7,8,9");
			second.add("10,11,12");

			return new TestSplitterPojo(first, second);
		}

		@Bean
		public MessageChannelSpec<?, ?> executorChannel(TaskExecutor taskExecutor) {
			return MessageChannels.executor(taskExecutor);
		}

		@Bean
		@SuppressWarnings("rawtypes")
		public IntegrationFlow splitResequenceFlow(MessageChannel executorChannel, TaskExecutor taskExecutor) {
			return f -> f.enrichHeaders(s -> s.header("FOO", "BAR"))
					.splitWith(s -> s
							.applySequence(false)
							.refName("testSplitterData")
							.method("buildList"))
					.channel(executorChannel)
					.splitWith(s -> s
							.applySequence(false)
							.<Message>function(Message::getPayload)
							.expectedType(Message.class))
					.channel(MessageChannels.executor(taskExecutor))
					.splitWith(s -> s
							.applySequence(false)
							.delimiters(","))
					.channel(MessageChannels.executor(taskExecutor))
					.<String, Integer>transform(Integer::parseInt)
					.enrichHeaders(h ->
							h.headerFunction(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Message::getPayload))
					.resequence(r -> r.releasePartialSequences(true).correlationExpression("'foo'"))
					.headerFilter(headerFilterSpec -> headerFilterSpec.headersToRemove("foo").patternMatch(false));
		}

		@Bean
		public IntegrationFlow splitAggregateFlow() {
			return IntegrationFlow.from("splitAggregateInput", true)
					.transform(Transformers.toJson(ObjectToJsonTransformer.ResultType.NODE))
					.splitWith((splitter) -> splitter
							.discardFlow((subFlow) -> subFlow
									.channel((c) -> c.queue("discardChannel"))))
					.channel(MessageChannels.flux())
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
					.aggregate(a -> a
							.outputProcessor((group) -> group
									.getMessages()
									.stream()
									.map(m -> (String) m.getPayload())
									.collect(Collectors.joining(" ")))
							.headersFunction((group) -> Collections.singletonMap("foo", "bar")))
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
		public IntegrationFlow releaseBarrierFlow(MessageTriggerAction barrierTriggerAction) {
			return IntegrationFlow.from(releaseChannel())
					.trigger(barrierTriggerAction,
							e -> e.poller(p -> p.fixedDelay(100)))
					.get();
		}

	}

	record TestSplitterPojo(List<String> first, List<String> second) {

		@SuppressWarnings("unused")
		public List<List<String>> buildList() {
			return Arrays.asList(this.first, this.second);
		}

	}

}
