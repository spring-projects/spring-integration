/*
 * Copyright 2019-2022 the original author or authors.
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

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 5.3
 */
class ReactiveMessageHandlerTests {

	private AtomicBoolean handled = new AtomicBoolean();

	private QueueChannel output = new QueueChannel();

	@BeforeEach
	void setUp() {
		handled.set(false);
	}

	@Test
	void messageHandledOnSubscribe() {
		assertThat(handled.get()).isFalse();
		TestReactiveMessageHandler handler = new TestReactiveMessageHandler();
		handler.afterPropertiesSet();
		Message<?> message = new GenericMessage<>("");
		handler.handleMessage(message).subscribe();
		assertThat(handled.get()).isTrue();
	}

	@Test
	void messageTracked() {
		assertThat(handled.get()).isFalse();
		TestReactiveMessageHandler handler = new TestReactiveMessageHandler();
		handler.setShouldTrack(true);
		handler.setComponentName("test-message-handler");
		handler.afterPropertiesSet();
		Message<?> message = new GenericMessage<>("");
		handler.handleMessage(message).subscribe();
		assertThat(handled.get()).isTrue();
		Message<?> received = output.receive(1000);
		assertThat(received).isNotNull();
		MessageHistory history = received.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class);
		assertThat(history).size().isOne();
	}

	class TestReactiveMessageHandler extends AbstractReactiveMessageHandler {

		@Override
		protected Mono<Void> handleMessageInternal(Message<?> message) {

			return Mono.fromSupplier(() -> {
				handled.getAndSet(true);
				output.send(message);
				return handled.get();
			}).then();
		}

	}

}
