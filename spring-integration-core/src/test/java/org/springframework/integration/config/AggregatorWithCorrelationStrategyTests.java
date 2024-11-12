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

package org.springframework.integration.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marius Bogoevici
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class AggregatorWithCorrelationStrategyTests {

	@Autowired
	@Qualifier("inputChannel")
	MessageChannel inputChannel;

	@Autowired
	@Qualifier("outputChannel")
	PollableChannel outputChannel;

	@Autowired
	@Qualifier("pojoInputChannel")
	MessageChannel pojoInputChannel;

	@Autowired
	@Qualifier("pojoOutputChannel")
	PollableChannel pojoOutputChannel;

	@Test
	public void testCorrelationAndCompletion() {
		inputChannel.send(MessageBuilder.withPayload("A1").build());
		inputChannel.send(MessageBuilder.withPayload("B2").build());
		inputChannel.send(MessageBuilder.withPayload("C3").build());
		inputChannel.send(MessageBuilder.withPayload("A4").build());
		inputChannel.send(MessageBuilder.withPayload("B5").build());
		inputChannel.send(MessageBuilder.withPayload("C6").build());
		inputChannel.send(MessageBuilder.withPayload("A7").build());
		inputChannel.send(MessageBuilder.withPayload("B8").build());
		inputChannel.send(MessageBuilder.withPayload("C9").build());
		receiveAndCompare(outputChannel, "A1", "A4", "A7");
		receiveAndCompare(outputChannel, "B2", "B5", "B8");
		receiveAndCompare(outputChannel, "C3", "C6", "C9");
	}

	@Test
	public void testCorrelationAndCompletionWithPojo() {
		// the test verifies how a pojo strategy is applied
		// Strings are correlated by their first letter, integers are correlated
		// by the last digit
		pojoInputChannel.send(MessageBuilder.withPayload("X1").build());
		pojoInputChannel.send(MessageBuilder.withPayload(93).build());
		pojoInputChannel.send(MessageBuilder.withPayload("X4").build());
		pojoInputChannel.send(MessageBuilder.withPayload(113).build());
		pojoInputChannel.send(MessageBuilder.withPayload("X7").build());
		pojoInputChannel.send(MessageBuilder.withPayload(213).build());
		receiveAndCompare(pojoOutputChannel, "X1", "X4", "X7");
		receiveAndCompare(pojoOutputChannel, "93", "113", "213");
	}

	private void receiveAndCompare(PollableChannel outputChannel, String... expectedValues) {
		Message<?> message = outputChannel.receive(500);
		assertThat(message).isNotNull();
		for (String expectedValue : expectedValues) {
			assertThat((String) message.getPayload()).contains(expectedValue);
		}
	}

	public static class MessageCountReleaseStrategy implements ReleaseStrategy {

		private final int expectedSize;

		public MessageCountReleaseStrategy(int expectedSize) {
			this.expectedSize = expectedSize;
		}

		public boolean canRelease(MessageGroup messages) {
			return messages.size() == expectedSize;
		}

	}

	public static class FirstLetterCorrelationStrategy implements CorrelationStrategy {

		public Object getCorrelationKey(Message<?> message) {
			return message.getPayload().toString().subSequence(0, 1);
		}

	}

	public static class PojoCorrelationStrategy {

		public String correlate(String message) {
			return message.substring(0, 1);
		}

		public String correlate(Integer message) {
			return Integer.toString(message % 10);
		}

	}

	public static class SimpleAggregator {

		@Aggregator
		public String concatenate(List<Object> payloads) {
			StringBuilder buffer = new StringBuilder();
			for (Object payload : payloads) {
				buffer.append(payload.toString());
			}
			return buffer.toString();
		}

	}

}
