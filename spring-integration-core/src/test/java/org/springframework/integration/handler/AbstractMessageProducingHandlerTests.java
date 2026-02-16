/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.handler;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractMessageProducingHandler}.
 *
 * @author Glenn Renfro
 * @since 6.5.7
 */
public class AbstractMessageProducingHandlerTests {

	private TestMessageProducingHandler handler;

	@BeforeEach
	void setup() {
		this.handler = new TestMessageProducingHandler();
		BeanFactory beanFactory = mock(BeanFactory.class);
		this.handler.setBeanFactory(beanFactory);
		this.handler.afterPropertiesSet();
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldSendNonQueueFluxOutputToConfiguredChannel() {
		QueueChannel outputChannel = new QueueChannel();
		this.handler.setOutputChannel(outputChannel);

		Flux<String> flux = Flux.just("Hello", "World");
		Message<?> inputMessage = MessageBuilder.withPayload(flux).build();

		this.handler.handleMessageInternal(inputMessage);
		Message<?> outputMessage = outputChannel.receive(0);
		assertThat(outputMessage).isNotNull();
		Flux<String> result = (Flux<String>) outputMessage.getPayload();
		StepVerifier.create(result)
				.expectNext("Hello")
				.expectNext("World")
				.thenCancel()
				.verify();
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldSendMonoOutputToConfiguredChannel() {
		QueueChannel outputChannel = new QueueChannel();
		this.handler.setOutputChannel(outputChannel);

		Mono<String> mono = Mono.just("Hello");
		Message<?> inputMessage = MessageBuilder.withPayload(mono).build();

		this.handler.handleMessageInternal(inputMessage);
		Message<?> outputMessage = outputChannel.receive(0);
		assertThat(outputMessage).isNotNull();
		Mono<String> result = (Mono<String>) outputMessage.getPayload();
		StepVerifier.create(result)
				.expectNext("Hello")
				.thenCancel()
				.verify();
	}

	@SuppressWarnings("unchecked")
	@Test
	void shouldSendQueueFluxOutputToConfiguredChannel() {
		QueueChannel outputChannel = new QueueChannel();
		this.handler.setOutputChannel(outputChannel);

		Queue<String> testQueue = new LinkedList<>();
		testQueue.add("Hello");
		testQueue.add("World");
		Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
		testQueue.forEach(item -> sink.tryEmitNext(item));
		sink.tryEmitComplete();
		Message<?> inputMessage = MessageBuilder.withPayload(sink.asFlux()).build();

		this.handler.handleMessageInternal(inputMessage);

		Message<?> outputMessage = outputChannel.receive(0);
		assertThat(outputMessage).isNotNull();
		Flux<String> result = (Flux<String>) outputMessage.getPayload();

		StepVerifier.create(result)
				.expectNext("Hello")
				.expectNext("World")
				.thenCancel()
				.verify();
	}

	@Test
	void shouldHandleCompletableFutureInAsyncMode() {
		QueueChannel outputChannel = new QueueChannel();
		this.handler.setOutputChannel(outputChannel);
		this.handler.setAsync(true);
		CompletableFuture<String> future = new CompletableFuture<>();
		Message<?> inputMessage = MessageBuilder.withPayload(future).build();

		this.handler.handleMessageInternal(inputMessage);

		future.complete("asyncResult");

		Message<?> output = outputChannel.receive(1000);
		assertThat(output).isNotNull();
		assertThat(output.getPayload()).isEqualTo("asyncResult");
	}

	private static class TestMessageProducingHandler extends AbstractMessageProducingHandler {

		@Override
		protected void handleMessageInternal(Message<?> message) {
			sendOutputs(message.getPayload(), message);
		}

	}

}
