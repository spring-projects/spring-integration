/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.splitter;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
class DefaultSplitterTests {

	@Test
	void splitMessageWithArrayPayload() {
		String[] payload = new String[] {"x", "y", "z"};
		Message<String[]> message = MessageBuilder.withPayload(payload).build();
		QueueChannel replyChannel = new QueueChannel();
		DefaultMessageSplitter splitter = new DefaultMessageSplitter();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		assertThat(replies.size()).isEqualTo(3);
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("x");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("y");
		Message<?> reply3 = replies.get(2);
		assertThat(reply3).isNotNull();
		assertThat(reply3.getPayload()).isEqualTo("z");
	}

	@Test
	void splitMessageWithCollectionPayload() {
		List<String> payload = Arrays.asList("x", "y", "z");
		Message<List<String>> message = MessageBuilder.withPayload(payload).build();
		QueueChannel replyChannel = new QueueChannel();
		DefaultMessageSplitter splitter = new DefaultMessageSplitter();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		assertThat(replies.size()).isEqualTo(3);
		Message<?> reply1 = replies.get(0);
		assertThat(reply1).isNotNull();
		assertThat(reply1.getPayload()).isEqualTo("x");
		Message<?> reply2 = replies.get(1);
		assertThat(reply2).isNotNull();
		assertThat(reply2.getPayload()).isEqualTo("y");
		Message<?> reply3 = replies.get(2);
		assertThat(reply3).isNotNull();
		assertThat(reply3.getPayload()).isEqualTo("z");
	}

	@Test
	void correlationIdCopiedFromMessageId() {
		Message<String> message = MessageBuilder.withPayload("test").build();
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel(1);
		DefaultMessageSplitter splitter = new DefaultMessageSplitter();
		splitter.setOutputChannel(outputChannel);
		EventDrivenConsumer endpoint = new EventDrivenConsumer(inputChannel, splitter);
		endpoint.start();
		assertThat(inputChannel.send(message)).isTrue();
		Message<?> reply = outputChannel.receive(0);
		assertThat(new IntegrationMessageHeaderAccessor(reply).getCorrelationId())
				.isEqualTo(message.getHeaders().getId());
	}

	@Test
	void splitMessageWithEmptyCollectionPayload() {
		Message<List<String>> message = MessageBuilder.withPayload(Collections.<String>emptyList()).build();
		QueueChannel replyChannel = new QueueChannel();
		DefaultMessageSplitter splitter = new DefaultMessageSplitter();
		splitter.setOutputChannel(replyChannel);
		splitter.handleMessage(message);
		Message<?> output = replyChannel.receive(15);
		assertThat(output).isNull();
	}

	@Test
	void splitStream() {
		Message<?> message = new GenericMessage<>(
				Stream.generate(Math::random)
						.limit(10));
		QueueChannel outputChannel = new QueueChannel();
		DefaultMessageSplitter splitter = new DefaultMessageSplitter();
		splitter.setOutputChannel(outputChannel);
		splitter.handleMessage(message);
		for (int i = 0; i < 10; i++) {
			Message<?> reply = outputChannel.receive(0);
			assertThat(new IntegrationMessageHeaderAccessor(reply).getCorrelationId())
					.isEqualTo(message.getHeaders().getId());
		}
		assertThat(outputChannel.receive(10)).isNull();
	}

	@Test
	void splitFlux() {
		Message<?> message = new GenericMessage<>(
				Flux
						.generate(() -> 0,
								(state, sink) -> {
									if (state == 10) {
										sink.complete();
									}
									else {
										sink.next(state);
									}
									return ++state;
								}));
		QueueChannel outputChannel = new QueueChannel();
		DefaultMessageSplitter splitter = new DefaultMessageSplitter();
		splitter.setOutputChannel(outputChannel);
		splitter.handleMessage(message);
		for (int i = 0; i < 10; i++) {
			Message<?> reply = outputChannel.receive(0);
			assertThat(new IntegrationMessageHeaderAccessor(reply).getCorrelationId())
					.isEqualTo(message.getHeaders().getId());
		}
		assertThat(outputChannel.receive(10)).isNull();
	}

	@Test
	void splitArrayPayloadReactive() {
		Message<?> message = new GenericMessage<>(new String[] {"x", "y", "z"});
		FluxMessageChannel replyChannel = new FluxMessageChannel();

		Flux<String> testFlux =
				Flux.from(replyChannel)
						.map(Message::getPayload)
						.cast(String.class);

		StepVerifier verifier =
				StepVerifier.create(testFlux)
						.expectNext("x", "y", "z")
						.expectNoEvent(Duration.ofMillis(100))
						.thenCancel()
						.verifyLater();

		DefaultMessageSplitter splitter = new DefaultMessageSplitter();
		splitter.setOutputChannel(replyChannel);

		splitter.handleMessage(message);

		verifier.verify(Duration.ofSeconds(1));
	}

	@Test
	void splitStreamReactive() {
		Message<?> message = new GenericMessage<>(Stream.of("x", "y", "z"));
		FluxMessageChannel replyChannel = new FluxMessageChannel();
		Flux<String> testFlux =
				Flux.from(replyChannel)
						.map(Message::getPayload)
						.cast(String.class);

		StepVerifier verifier =
				StepVerifier.create(testFlux)
						.expectNext("x", "y", "z")
						.expectNoEvent(Duration.ofMillis(100))
						.thenCancel()
						.verifyLater();

		DefaultMessageSplitter splitter = new DefaultMessageSplitter();
		splitter.setOutputChannel(replyChannel);

		splitter.handleMessage(message);

		verifier.verify(Duration.ofSeconds(1));
	}

	@Test
	void splitFluxReactive() {
		Message<?> message = new GenericMessage<>(Flux.just("x", "y", "z"));
		FluxMessageChannel replyChannel = new FluxMessageChannel();
		Flux<String> testFlux =
				Flux.from(replyChannel)
						.map(Message::getPayload)
						.cast(String.class);

		StepVerifier verifier =
				StepVerifier.create(testFlux)
						.expectNext("x", "y", "z")
						.expectNoEvent(Duration.ofMillis(100))
						.thenCancel()
						.verifyLater();

		DefaultMessageSplitter splitter = new DefaultMessageSplitter();
		splitter.setOutputChannel(replyChannel);

		splitter.handleMessage(message);

		verifier.verify(Duration.ofSeconds(1));
	}

}
