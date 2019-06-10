/*
 * Copyright 2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.util.Loggers;

/**
 * @author Artem Bilan
 *
 * @since 5.2
 */
class FluxAggregatorMessageHandlerTests {

	@Test
	void testDefaultAggregation() {
		QueueChannel resultChannel = new QueueChannel();
		FluxAggregatorMessageHandler fluxAggregatorMessageHandler = new FluxAggregatorMessageHandler();
		fluxAggregatorMessageHandler.setOutputChannel(resultChannel);

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
						assertThat((MessageHeaders) headers)
								.containsEntry(IntegrationMessageHeaderAccessor.CORRELATION_ID, 0));

		Object payload = result.getPayload();
		assertThat(payload).isInstanceOf(Flux.class);

		Loggers.useVerboseConsoleLoggers();

		@SuppressWarnings("unchecked")
		Flux<Message<?>> window = (Flux<Message<?>>) payload;

		StepVerifier.create(
				window.map(Message::getPayload)
						.cast(String.class)
						.log())
				.expectNext("0", "2", "4", "6", "8", "10", "12", "14", "16", "18", "20")
				/*.expectNextSequence(
						IntStream.iterate(0, i -> i + 2)
								.limit(10)
								.mapToObj(Objects::toString)
								.collect(Collectors.toList()))*/
				.thenCancel()
				.verify();
	}

}
