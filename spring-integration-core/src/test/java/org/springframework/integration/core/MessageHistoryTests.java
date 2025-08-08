/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.core;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MessageHistoryTests {

	@Test
	public void addComponents() {
		GenericMessage<String> original = new GenericMessage<>("foo");
		assertThat(MessageHistory.read(original)).isNull();
		Message<String> result1 = MessageHistory.write(original, new TestComponent(1));
		MessageHistory history1 = MessageHistory.read(result1);
		assertThat(history1).isNotNull();
		assertThat(history1.toString()).isEqualTo("testComponent-1");
		Message<String> result2 = MessageHistory.write(result1, new TestComponent(2));
		MessageHistory history2 = MessageHistory.read(result2);
		assertThat(history2).isNotNull();
		assertThat(history2.toString()).isEqualTo("testComponent-1,testComponent-2");
	}

	@Test
	public void verifyImmutability() {
		Message<?> message = MessageHistory.write(MessageBuilder.withPayload("test").build(), new TestComponent(1));
		MessageHistory history = MessageHistory.read(message);
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> history.add(new Properties()));
	}

	@Test
	public void testCorrectMutableMessageAfterWrite() {
		MutableMessage<String> original = new MutableMessage<>("foo");
		assertThat(MessageHistory.read(original)).isNull();
		Message<String> result1 = MessageHistory.write(original, new TestComponent(1));
		assertThat(result1).isInstanceOf(MutableMessage.class);
		assertThat(result1).isSameAs(original);
		MessageHistory history1 = MessageHistory.read(result1);
		assertThat(history1).isNotNull();
		assertThat(history1.toString()).isEqualTo("testComponent-1");
		Message<String> result2 = MessageHistory.write(result1, new TestComponent(2));
		assertThat(result2).isSameAs(original);
		MessageHistory history2 = MessageHistory.read(result2);
		assertThat(history2).isNotNull();
		assertThat(history2.toString()).isEqualTo("testComponent-1,testComponent-2");
	}

	@Test
	public void testCorrectErrorMessageAfterWrite() {
		Message<?> originalMessage = new GenericMessage<>("test");
		RuntimeException payload = new RuntimeException();
		ErrorMessage original = new ErrorMessage(payload, originalMessage);
		assertThat(MessageHistory.read(original)).isNull();
		Message<Throwable> result1 = MessageHistory.write(original, new TestComponent(1));
		assertThat(result1).isInstanceOf(ErrorMessage.class);
		assertThat(result1).isNotSameAs(original);
		assertThat(result1.getPayload()).isSameAs(original.getPayload());
		assertThat(result1).extracting("originalMessage").isSameAs(originalMessage);
		MessageHistory history1 = MessageHistory.read(result1);
		assertThat(history1).isNotNull();
		assertThat(history1.toString()).isEqualTo("testComponent-1");
		Message<Throwable> result2 = MessageHistory.write(result1, new TestComponent(2));
		assertThat(result2).isInstanceOf(ErrorMessage.class);
		assertThat(result2).isNotSameAs(original);
		assertThat(result2).isSameAs(result1);
		assertThat(result2.getPayload()).isSameAs(original.getPayload());
		assertThat(result1).extracting("originalMessage").isSameAs(originalMessage);
		MessageHistory history2 = MessageHistory.read(result2);
		assertThat(history2).isNotNull();
		assertThat(history2.toString()).isEqualTo("testComponent-1,testComponent-2");
	}

	@Test
	public void testCorrectAdviceMessageAfterWrite() {
		Message<?> inputMessage = new GenericMessage<>("input");
		AdviceMessage<String> original = new AdviceMessage<>("foo", inputMessage);
		assertThat(MessageHistory.read(original)).isNull();
		Message<String> result1 = MessageHistory.write(original, new TestComponent(1));
		assertThat(result1).isInstanceOf(AdviceMessage.class);
		assertThat(result1).isNotSameAs(original);
		assertThat(result1.getPayload()).isSameAs(original.getPayload());
		assertThat(((AdviceMessage<?>) result1).getInputMessage()).isSameAs(original.getInputMessage());
		MessageHistory history1 = MessageHistory.read(result1);
		assertThat(history1).isNotNull();
		assertThat(history1.toString()).isEqualTo("testComponent-1");
		Message<String> result2 = MessageHistory.write(result1, new TestComponent(2));
		assertThat(result2).isInstanceOf(AdviceMessage.class);
		assertThat(result2).isNotSameAs(original);
		assertThat(result2.getPayload()).isSameAs(original.getPayload());
		assertThat(((AdviceMessage<?>) result2).getInputMessage()).isSameAs(original.getInputMessage());
		assertThat(result2).isSameAs(result1);
		MessageHistory history2 = MessageHistory.read(result2);
		assertThat(history2).isNotNull();
		assertThat(history2.toString()).isEqualTo("testComponent-1,testComponent-2");
	}

	private record TestComponent(int id) implements NamedComponent {

		@Override
		public String getComponentName() {
			return "testComponent-" + this.id;
		}

		@Override
		public String getComponentType() {
			return "type-" + this.id;
		}

	}

}
