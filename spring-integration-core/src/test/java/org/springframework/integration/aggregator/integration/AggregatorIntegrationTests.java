/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.aggregator.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Iwein Fuld
 * @author Alex Peters
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 */
@SpringJUnitConfig
@DirtiesContext
public class AggregatorIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MessageChannel input;

	@Autowired
	private MessageChannel expiringAggregatorInput;

	@Autowired
	private MessageChannel nonExpiringAggregatorInput;

	@Autowired
	private MessageChannel groupTimeoutAggregatorInput;

	@Autowired
	private MessageChannel groupTimeoutExpressionAggregatorInput;

	@Autowired
	private MessageChannel zeroGroupTimeoutExpressionAggregatorInput;

	@Autowired
	private QueueChannel output;

	@Autowired
	private PollableChannel discard;

	@Autowired
	private QueueChannel errors;

	@Test
	public void testVanillaAggregation() {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			input.send(new GenericMessage<>(i, headers));
		}
		Message<?> receive = output.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(1 + 2 + 3 + 4);
	}

	@Test
	public void testNonExpiringAggregator() {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			nonExpiringAggregatorInput.send(new GenericMessage<>(i, headers));
		}
		assertThat(output.receive(0)).isNotNull();

		assertThat(discard.receive(0)).isNull();

		for (int i = 5; i < 10; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			nonExpiringAggregatorInput.send(new GenericMessage<>(i, headers));
		}
		assertThat(output.receive(0)).isNull();

		assertThat(discard.receive(0)).isNotNull();
		assertThat(discard.receive(0)).isNotNull();
		assertThat(discard.receive(0)).isNotNull();
		assertThat(discard.receive(0)).isNotNull();
		assertThat(discard.receive(0)).isNotNull();
	}

	@Test
	public void testExpiringAggregator() {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			expiringAggregatorInput.send(new GenericMessage<>(i, headers));
		}
		assertThat(output.receive(0)).isNotNull();

		assertThat(discard.receive(0)).isNull();

		for (int i = 5; i < 10; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			expiringAggregatorInput.send(new GenericMessage<>(i, headers));
		}
		assertThat(output.receive(0)).isNotNull();

		assertThat(discard.receive(0)).isNull();

	}

	@Test
	public void testGroupTimeoutScheduling() throws Exception {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			this.groupTimeoutAggregatorInput.send(new GenericMessage<>(i, headers));

			//Wait until 'group-timeout' does its stuff.
			MessageGroupStore mgs = TestUtils.getPropertyValue(this.context.getBean("gta.handler"), "messageStore",
					MessageGroupStore.class);
			int n = 0;
			while (n++ < 100 && mgs.getMessageGroupCount() > 0) {
				Thread.sleep(100);
			}
			assertThat(n < 100).as("Group did not complete").isTrue();
			assertThat(this.output.receive(10000)).isNotNull();
			assertThat(this.discard.receive(0)).isNull();
		}
	}

	@Test
	public void testGroupTimeoutReschedulingOnMessageDeliveryException() throws Exception {
		for (int i = 0; i < 5; i++) {
			this.output.send(new GenericMessage<>("fake message"));
		}

		Map<String, Object> headers = stubHeaders(1, 2, 1);
		this.groupTimeoutAggregatorInput.send(new GenericMessage<>(1, headers));

		//Wait until 'group-timeout' does its stuff.
		MessageGroupStore mgs = TestUtils.getPropertyValue(this.context.getBean("gta.handler"), "messageStore",
				MessageGroupStore.class);
		int n = 0;
		while (n++ < 100 && mgs.getMessageGroupCount() > 0) {
			Thread.sleep(100);
			if (n == 10) {
				TestUtils.getPropertyValue(this.output, "queue", Queue.class).clear();
			}
		}
		assertThat(n < 100).as("Group did not complete").isTrue();
		Message<?> receive = this.output.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo(Collections.singletonList(1));
		assertThat(this.discard.receive(0)).isNull();
	}

	@Test
	public void testGroupTimeoutExpressionScheduling() {
		// Since group-timeout-expression="size() >= 2 ? 100 : null". The first message won't be scheduled to
		// 'forceComplete'
		this.groupTimeoutExpressionAggregatorInput.send(new GenericMessage<>(1, stubHeaders(1, 6, 1)));
		assertThat(this.output.receive(0)).isNull();
		assertThat(this.discard.receive(0)).isNull();

		// As far as 'group.size() >= 2' it will be scheduled to 'forceComplete'
		this.groupTimeoutExpressionAggregatorInput.send(new GenericMessage<>(2, stubHeaders(2, 6, 1)));
		assertThat(this.output.receive(0)).isNull();
		Message<?> receive = this.output.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(((Collection<?>) receive.getPayload()).size()).isEqualTo(2);
		assertThat(this.discard.receive(0)).isNull();

		// The same with these three messages
		this.groupTimeoutExpressionAggregatorInput.send(new GenericMessage<>(3, stubHeaders(3, 6, 1)));
		assertThat(this.output.receive(0)).isNull();
		assertThat(this.discard.receive(0)).isNull();

		this.groupTimeoutExpressionAggregatorInput.send(new GenericMessage<>(4, stubHeaders(4, 6, 1)));
		assertThat(this.output.receive(0)).isNull();
		assertThat(this.discard.receive(0)).isNull();

		this.groupTimeoutExpressionAggregatorInput.send(new GenericMessage<>(5, stubHeaders(5, 6, 1)));
		assertThat(this.output.receive(0)).isNull();
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(((Collection<?>) receive.getPayload()).size()).isEqualTo(3);
		assertThat(this.discard.receive(0)).isNull();

		// The last message in the sequence - normal release by provided 'ReleaseStrategy'
		this.groupTimeoutExpressionAggregatorInput.send(new GenericMessage<>(6, stubHeaders(6, 6, 1)));
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(((Collection<?>) receive.getPayload()).size()).isEqualTo(1);
		assertThat(this.discard.receive(0)).isNull();
	}

	@Test
	public void testZeroGroupTimeoutExpressionScheduling() {
		try {
			this.output.purge(null);
			this.errors.purge(null);
			GenericMessage<String> message = new GenericMessage<>("foo");
			this.output.send(message);
			this.output.send(message);
			this.output.send(message);
			this.output.send(message);
			this.output.send(message);
			this.zeroGroupTimeoutExpressionAggregatorInput.send(new GenericMessage<>(1, stubHeaders(1, 2, 1)));
			ErrorMessage em = (ErrorMessage) this.errors.receive(10000);
			assertThat(em).isNotNull();
			assertThat(em.getPayload().getMessage().toLowerCase())
					.contains("failed to send message to channel")
					.contains("output")
					.contains("within timeout: 10");
		}
		finally {
			this.output.purge(null);
			this.errors.purge(null);
		}
	}

	// configured in context associated with this test
	private Map<String, Object> stubHeaders(int sequenceNumber, int sequenceSize, int correlationId) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, sequenceNumber);
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, sequenceSize);
		headers.put(IntegrationMessageHeaderAccessor.CORRELATION_ID, correlationId);
		return headers;
	}

	public static Function<MessageGroup, Map<String, Object>> firstMessageHeaders() {
		return (messageGroup) -> messageGroup.getOne().getHeaders();
	}

	public static class SummingAggregator {

		public Integer sum(List<Integer> numbers) {
			int result = 0;
			for (Integer number : numbers) {
				result += number;
			}
			return result;
		}

	}

}
