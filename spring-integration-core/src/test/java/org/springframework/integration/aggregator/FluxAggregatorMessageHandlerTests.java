/*
 * Copyright 2019-2024 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 5.2
 */
@SuppressWarnings("unchecked")
class FluxAggregatorMessageHandlerTests {

	@Test
	void testDefaultAggregation() {
		QueueChannel resultChannel = new QueueChannel();
		FluxAggregatorMessageHandler fluxAggregatorMessageHandler = new FluxAggregatorMessageHandler();
		fluxAggregatorMessageHandler.setOutputChannel(resultChannel);
		fluxAggregatorMessageHandler.start();

		for (int i = 0; i < 20; i++) {
			Message<?> messageToAggregate =
					MessageBuilder.withPayload("" + i)
							.setCorrelationId(i % 2)
							.setSequenceSize(10)
							.build();
			fluxAggregatorMessageHandler.handleMessage(messageToAggregate);
		}

		Message<?> result = resultChannel.receive(10_000);
		assertThat(result).isNotNull()
				.extracting(Message::getHeaders)
				.satisfies((headers) ->
						assertThat(headers)
								.containsEntry(IntegrationMessageHeaderAccessor.CORRELATION_ID, 0));

		Object payload = result.getPayload();
		assertThat(payload).isInstanceOf(Flux.class);

		Flux<Message<?>> window = (Flux<Message<?>>) payload;

		StepVerifier.create(
						window.map(Message::getPayload)
								.cast(String.class))
				.expectNextSequence(
						IntStream.iterate(0, i -> i + 2)
								.limit(10)
								.mapToObj(Objects::toString)
								.collect(Collectors.toList()))
				.verifyComplete();

		result = resultChannel.receive(10_000);
		assertThat(result).isNotNull()
				.extracting(Message::getHeaders)
				.satisfies((headers) ->
						assertThat(headers)
								.containsEntry(IntegrationMessageHeaderAccessor.CORRELATION_ID, 1));

		payload = result.getPayload();
		window = (Flux<Message<?>>) payload;

		StepVerifier.create(
						window.map(Message::getPayload)
								.cast(String.class))
				.expectNextSequence(
						IntStream.iterate(1, i -> i + 2)
								.limit(10)
								.mapToObj(Objects::toString)
								.collect(Collectors.toList()))
				.verifyComplete();

		fluxAggregatorMessageHandler.stop();
	}

	@Test
	void testCustomCombineFunction() {
		QueueChannel resultChannel = new QueueChannel();
		FluxAggregatorMessageHandler fluxAggregatorMessageHandler = new FluxAggregatorMessageHandler();
		fluxAggregatorMessageHandler.setOutputChannel(resultChannel);
		fluxAggregatorMessageHandler.setWindowSize(10);
		fluxAggregatorMessageHandler.setCombineFunction(
				(messageFlux) ->
						messageFlux
								.map(Message::getPayload)
								.collectList()
								.map(GenericMessage::new));
		fluxAggregatorMessageHandler.start();

		for (int i = 0; i < 20; i++) {
			Message<?> messageToAggregate =
					MessageBuilder.withPayload(i)
							.setCorrelationId(i % 2)
							.build();
			fluxAggregatorMessageHandler.handleMessage(messageToAggregate);
		}

		Message<?> result = resultChannel.receive(10_000);
		assertThat(result).isNotNull();

		Object payload = result.getPayload();
		assertThat(payload)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.containsExactly(
						IntStream.iterate(0, i -> i + 2)
								.limit(10)
								.boxed()
								.toArray());

		result = resultChannel.receive(10_000);
		assertThat(result).isNotNull();

		payload = result.getPayload();
		assertThat(payload)
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.containsExactly(
						IntStream.iterate(1, i -> i + 2)
								.limit(10)
								.boxed()
								.toArray());

		fluxAggregatorMessageHandler.stop();
	}

	@Test
	void testWindowTimespan() {
		QueueChannel resultChannel = new QueueChannel();
		FluxAggregatorMessageHandler fluxAggregatorMessageHandler = new FluxAggregatorMessageHandler();
		fluxAggregatorMessageHandler.setOutputChannel(resultChannel);
		fluxAggregatorMessageHandler.setWindowTimespan(Duration.ofMillis(100));
		fluxAggregatorMessageHandler.start();

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.submit(() -> {
			for (int i = 0; i < 10; i++) {
				Message<?> messageToAggregate =
						MessageBuilder.withPayload(i)
								.setCorrelationId("1")
								.build();
				fluxAggregatorMessageHandler.handleMessage(messageToAggregate);
				Thread.sleep(20);
			}
			return null;
		});

		Message<?> result = resultChannel.receive(10_000);
		assertThat(result).isNotNull();

		Flux<Message<?>> window = (Flux<Message<?>>) result.getPayload();

		List<Integer> messageList =
				window.map(Message::getPayload)
						.cast(Integer.class)
						.collectList()
						.block(Duration.ofSeconds(10));

		assertThat(messageList)
				.isNotEmpty()
				.hasSizeLessThan(10)
				.contains(0, 1);

		result = resultChannel.receive(10_000);
		assertThat(result).isNotNull();

		window = (Flux<Message<?>>) result.getPayload();

		messageList =
				window.map(Message::getPayload)
						.cast(Integer.class)
						.collectList()
						.block(Duration.ofSeconds(10));

		assertThat(messageList)
				.isNotEmpty()
				.hasSizeLessThan(10)
				.doesNotContain(0, 1);

		fluxAggregatorMessageHandler.stop();

		executorService.shutdown();
	}

	@Test
	void testBoundaryTrigger() {
		QueueChannel resultChannel = new QueueChannel();
		FluxAggregatorMessageHandler fluxAggregatorMessageHandler = new FluxAggregatorMessageHandler();
		fluxAggregatorMessageHandler.setOutputChannel(resultChannel);
		fluxAggregatorMessageHandler.setBoundaryTrigger((message) -> "terminate".equals(message.getPayload()));
		fluxAggregatorMessageHandler.start();

		for (int i = 0; i < 3; i++) {
			Message<?> messageToAggregate =
					MessageBuilder.withPayload("" + i)
							.setCorrelationId("1")
							.build();
			fluxAggregatorMessageHandler.handleMessage(messageToAggregate);
		}

		fluxAggregatorMessageHandler.handleMessage(
				MessageBuilder.withPayload("terminate")
						.setCorrelationId("1")
						.build());

		fluxAggregatorMessageHandler.handleMessage(
				MessageBuilder.withPayload("next")
						.setCorrelationId("1")
						.build());

		Message<?> result = resultChannel.receive(10_000);
		assertThat(result).isNotNull();

		Flux<Message<?>> window = (Flux<Message<?>>) result.getPayload();

		StepVerifier.create(
						window.map(Message::getPayload)
								.cast(String.class))
				.expectNext("0", "1", "2")
				.expectNext("terminate")
				.verifyComplete();

		fluxAggregatorMessageHandler.stop();
	}

	@Test
	void testCustomWindow() {
		QueueChannel resultChannel = new QueueChannel();
		FluxAggregatorMessageHandler fluxAggregatorMessageHandler = new FluxAggregatorMessageHandler();
		fluxAggregatorMessageHandler.setOutputChannel(resultChannel);
		fluxAggregatorMessageHandler.setWindowConfigurer((group) ->
				group.windowWhile((message) ->
						message.getPayload() instanceof Integer));
		fluxAggregatorMessageHandler.start();

		for (int i = 0; i < 3; i++) {
			Message<?> messageToAggregate =
					MessageBuilder.withPayload(i)
							.setCorrelationId("1")
							.build();
			fluxAggregatorMessageHandler.handleMessage(messageToAggregate);
		}

		fluxAggregatorMessageHandler.handleMessage(
				MessageBuilder.withPayload("terminate")
						.setCorrelationId("1")
						.build());

		Message<?> result = resultChannel.receive(10_000);
		assertThat(result).isNotNull();

		Flux<Message<?>> window = (Flux<Message<?>>) result.getPayload();

		StepVerifier.create(
						window.map(Message::getPayload)
								.cast(Integer.class))
				.expectNext(0, 1, 2)
				.verifyComplete();

		fluxAggregatorMessageHandler.stop();
	}

}
