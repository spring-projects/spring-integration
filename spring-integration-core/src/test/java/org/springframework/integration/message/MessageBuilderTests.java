/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.support.MutableMessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MessageBuilderTests {

	@Autowired
	private MessageChannel in;

	@Autowired
	private PollableChannel out;

	@Autowired
	private MessageBuilderFactory messageBuilderFactory;

	@Test(expected = IllegalArgumentException.class) // priority must be an Integer
	public void testPriorityHeader() {
		MessageBuilder.withPayload("ha").setHeader("priority", "10").build();
	}

	@Test
	public void testSimpleMessageCreation() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test
	public void testHeaderValues() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar")
				.setHeader("count", 123)
				.build();
		assertThat(message.getHeaders().get("foo", String.class)).isEqualTo("bar");
		assertThat(message.getHeaders().get("count", Integer.class)).isEqualTo(123);
	}

	@Test
	public void testCopiedHeaderValues() {
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setHeader("foo", "1")
				.setHeader("bar", "2")
				.build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.copyHeaders(message1.getHeaders())
				.setHeader("foo", "42")
				.setHeaderIfAbsent("bar", "99")
				.build();
		assertThat(message1.getPayload()).isEqualTo("test1");
		assertThat(message2.getPayload()).isEqualTo("test2");
		assertThat(message1.getHeaders().get("foo")).isEqualTo("1");
		assertThat(message2.getHeaders().get("foo")).isEqualTo("42");
		assertThat(message1.getHeaders().get("bar")).isEqualTo("2");
		assertThat(message2.getHeaders().get("bar")).isEqualTo("2");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIdHeaderValueReadOnly() {
		UUID id = UUID.randomUUID();
		MessageBuilder.withPayload("test").setHeader(MessageHeaders.ID, id);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTimestampValueReadOnly() {
		Long timestamp = 12345L;
		MessageBuilder.withPayload("test").setHeader(MessageHeaders.TIMESTAMP, timestamp).build();
	}

	@Test
	public void copyHeadersIfAbsent() {
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setHeader("foo", 123)
				.copyHeadersIfAbsent(message1.getHeaders())
				.build();
		assertThat(message2.getPayload()).isEqualTo("test2");
		assertThat(message2.getHeaders().get("foo")).isEqualTo(123);
	}

	@Test
	public void createFromMessage() {
		Message<String> message1 = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.fromMessage(message1).build();
		assertThat(message2.getPayload()).isEqualTo("test");
		assertThat(message2.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	public void createIdRegenerated() {
		Message<String> message1 = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.fromMessage(message1).setHeader("another", 1).build();
		assertThat(message2.getHeaders().get("foo")).isEqualTo("bar");
		assertThat(message2.getHeaders().getId()).isNotSameAs(message1.getHeaders().getId());
	}

	@Test
	public void mutate() {
		assertThat(this.messageBuilderFactory instanceof MutableMessageBuilderFactory).isTrue();
		in.send(new GenericMessage<>("foo"));
		Message<?> m1 = out.receive(0);
		Message<?> m2 = out.receive(0);
		assertThat(m1).isInstanceOf(MutableMessage.class);
		assertThat(m1 == m2).isTrue();
	}

	@Test
	public void mutable() {
		MutableMessageBuilder<String> builder = MutableMessageBuilder.withPayload("test");
		Message<String> message1 = builder
				.setHeader("foo", "bar").build();
		Message<String> message2 = MutableMessageBuilder.fromMessage(message1).setHeader("another", 1).build();
		assertThat(message2.getHeaders().get("foo")).isEqualTo("bar");
		assertThat(message2.getHeaders().getId()).isSameAs(message1.getHeaders().getId());
		assertThat(message2 == message1).isTrue();
	}

	@Test
	public void mutableFromImmutable() {
		Message<String> message1 = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MutableMessageBuilder.fromMessage(message1).setHeader("another", 1).build();
		assertThat(message2.getHeaders().get("foo")).isEqualTo("bar");
		assertThat(message2.getHeaders().getId()).isSameAs(message1.getHeaders().getId());
		assertThat(message2).isNotSameAs(message1);
		assertThat(message2 == message1).isFalse();
	}

	@Test
	public void mutableFromImmutableMutate() {
		Message<String> message1 = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar").build();
		Message<String> message2 = new MutableMessageBuilderFactory().fromMessage(message1).setHeader("another", 1)
				.build();
		assertThat(message2.getHeaders().get("foo")).isEqualTo("bar");
		assertThat(message2.getHeaders().getId()).isSameAs(message1.getHeaders().getId());
		assertThat(message2).isNotSameAs(message1);
		assertThat(message2 == message1).isFalse();
	}

	@Test
	public void testPriority() {
		Message<Integer> importantMessage = MessageBuilder.withPayload(1)
				.setPriority(123).build();
		assertThat(new IntegrationMessageHeaderAccessor(importantMessage).getPriority()).isEqualTo(123);
	}

	@Test
	public void testNonDestructiveSet() {
		Message<Integer> message1 = MessageBuilder.withPayload(1)
				.setPriority(42).build();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1)
				.setHeaderIfAbsent(IntegrationMessageHeaderAccessor.PRIORITY, 13)
				.build();
		assertThat(new IntegrationMessageHeaderAccessor(message2).getPriority()).isEqualTo(42);
	}

	@Test
	public void testExpirationDateSetAsLong() {
		Long past = System.currentTimeMillis() - (60 * 1000);
		Message<Integer> expiredMessage = MessageBuilder.withPayload(1)
				.setExpirationDate(past).build();
		assertThat(new IntegrationMessageHeaderAccessor(expiredMessage).getExpirationDate()).isEqualTo(past);
	}

	@Test
	public void testExpirationDateSetAsDate() {
		Long past = System.currentTimeMillis() - (60 * 1000);
		Message<Integer> expiredMessage = MessageBuilder.withPayload(1)
				.setExpirationDate(new Date(past)).build();
		assertThat(new IntegrationMessageHeaderAccessor(expiredMessage).getExpirationDate()).isEqualTo(past);
	}

	@Test
	public void testRemove() {
		Message<Integer> message1 = MessageBuilder.withPayload(1)
				.setHeader("foo", "bar").build();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1)
				.removeHeader("foo")
				.build();
		assertThat(message2.getHeaders().containsKey("foo")).isFalse();
	}

	@Test
	public void testSettingToNullRemoves() {
		Message<Integer> message1 = MessageBuilder.withPayload(1)
				.setHeader("foo", "bar").build();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1)
				.setHeader("foo", null)
				.build();
		assertThat(message2.getHeaders().containsKey("foo")).isFalse();
	}

	@Test
	public void testPushAndPopSequenceDetails() {
		Message<Integer> message1 = MessageBuilder.withPayload(1).pushSequenceDetails("foo", 1, 2).build();
		assertThat(message1.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1).pushSequenceDetails("bar", 1, 1).build();
		assertThat(message2.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isTrue();
		Message<Integer> message3 = MessageBuilder.fromMessage(message2).popSequenceDetails().build();
		assertThat(message3.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
	}

	@Test
	public void testPushAndPopSequenceDetailsWhenNoCorrelationId() {
		Message<Integer> message1 = MessageBuilder.withPayload(1).build();
		assertThat(message1.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1).pushSequenceDetails("bar", 1, 1).build();
		assertThat(message2.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
		Message<Integer> message3 = MessageBuilder.fromMessage(message2).popSequenceDetails().build();
		assertThat(message3.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
	}

	@Test
	public void testPopSequenceDetailsWhenNotPopped() {
		Message<Integer> message1 = MessageBuilder.withPayload(1).build();
		assertThat(message1.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1).popSequenceDetails().build();
		assertThat(message2.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
	}

	@Test
	public void testPushAndPopSequenceDetailsWhenNoSequence() {
		Message<Integer> message1 = MessageBuilder.withPayload(1).setCorrelationId("foo").build();
		assertThat(message1.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1).pushSequenceDetails("bar", 1, 1).build();
		assertThat(message2.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isTrue();
		Message<Integer> message3 = MessageBuilder.fromMessage(message2).popSequenceDetails().build();
		assertThat(message3.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
	}

	@Test
	public void testPushAndPopSequenceDetailsMutable() {
		Message<Integer> message1 = MutableMessageBuilder.withPayload(1).pushSequenceDetails("foo", 1, 2).build();
		assertThat(message1.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
		Message<Integer> message2 = MutableMessageBuilder.fromMessage(message1).pushSequenceDetails("bar", 1, 1)
				.build();
		assertThat(message2.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isTrue();
		Message<Integer> message3 = MutableMessageBuilder.fromMessage(message2).popSequenceDetails().build();
		assertThat(message3.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
	}

	@Test
	public void testPushAndPopSequenceDetailsWhenNoCorrelationIdMutable() {
		Message<Integer> message1 = MutableMessageBuilder.withPayload(1).build();
		assertThat(message1.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
		Message<Integer> message2 = MutableMessageBuilder.fromMessage(message1).pushSequenceDetails("bar", 1, 1)
				.build();
		assertThat(message2.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
		Message<Integer> message3 = MutableMessageBuilder.fromMessage(message2).popSequenceDetails().build();
		assertThat(message3.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
	}

	@Test
	public void testPopSequenceDetailsWhenNotPoppedMutable() {
		Message<Integer> message1 = MutableMessageBuilder.withPayload(1).build();
		assertThat(message1.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
		Message<Integer> message2 = MutableMessageBuilder.fromMessage(message1).popSequenceDetails().build();
		assertThat(message2.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
	}

	@Test
	public void testPushAndPopSequenceDetailsWhenNoSequenceMutable() {
		Message<Integer> message1 = MutableMessageBuilder.withPayload(1).setCorrelationId("foo").build();
		assertThat(message1.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
		Message<Integer> message2 = MutableMessageBuilder.fromMessage(message1).pushSequenceDetails("bar", 1, 1)
				.build();
		assertThat(message2.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isTrue();
		Message<Integer> message3 = MutableMessageBuilder.fromMessage(message2).popSequenceDetails().build();
		assertThat(message3.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isFalse();
	}

	@Test
	public void testNotModifiedSameMessage() {
		Message<?> original = MessageBuilder.withPayload("foo").build();
		Message<?> result = MessageBuilder.fromMessage(original).build();
		assertThat(result).isEqualTo(original);
	}

	@Test
	public void testContainsHeaderNotModifiedSameMessage() {
		Message<?> original = MessageBuilder.withPayload("foo").setHeader("bar", 42).build();
		Message<?> result = MessageBuilder.fromMessage(original).build();
		assertThat(result).isEqualTo(original);
	}

	@Test
	public void testSameHeaderValueAddedNotModifiedSameMessage() {
		Message<?> original = MessageBuilder.withPayload("foo").setHeader("bar", 42).build();
		Message<?> result = MessageBuilder.fromMessage(original).setHeader("bar", 42).build();
		assertThat(result).isEqualTo(original);
	}

	@Test
	public void testCopySameHeaderValuesNotModifiedSameMessage() {
		Date current = new Date();
		Map<String, Object> originalHeaders = new HashMap<>();
		originalHeaders.put("b", "xyz");
		originalHeaders.put("c", current);
		Message<?> original = MessageBuilder.withPayload("foo").setHeader("a", 123).copyHeaders(originalHeaders)
				.build();
		Map<String, Object> newHeaders = new HashMap<>();
		newHeaders.put("a", 123);
		newHeaders.put("b", "xyz");
		newHeaders.put("c", current);
		Message<?> result = MessageBuilder.fromMessage(original).copyHeaders(newHeaders).build();
		assertThat(result).isEqualTo(original);
	}

	@Test
	public void testSequenceNumberAsLong() {
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Long.MAX_VALUE)
				.build();

		@SuppressWarnings("unused")
		Integer sequenceNumber = new IntegrationMessageHeaderAccessor(message).getSequenceNumber();
	}

	@Test
	public void testNoIdAndTimestampHeaders() {
		Message<String> message =
				MutableMessageBuilder.withPayload("foo", false)
						.pushSequenceDetails("bar", 1, 1)
						.build();

		assertThat(message.getHeaders())
				.containsKeys(IntegrationMessageHeaderAccessor.CORRELATION_ID,
						IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER,
						IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)
				.doesNotContainKeys(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS,
						MessageHeaders.ID,
						MessageHeaders.TIMESTAMP);
	}

}
