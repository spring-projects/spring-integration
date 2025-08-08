/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import java.util.Comparator;

import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MessageSequenceComparatorTests {

	@Test
	public void testLessThan() {
		Comparator<Message<?>> comparator = new MessageSequenceComparator();
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceNumber(1).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceNumber(2).build();
		assertThat(comparator.compare(message1, message2)).isEqualTo(-1);
	}

	@Test
	public void testEqual() {
		Comparator<Message<?>> comparator = new MessageSequenceComparator();
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceNumber(3).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceNumber(3).build();
		assertThat(comparator.compare(message1, message2)).isEqualTo(0);
	}

	@Test
	public void testGreaterThan() {
		Comparator<Message<?>> comparator = new MessageSequenceComparator();
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceNumber(5).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceNumber(3).build();
		assertThat(comparator.compare(message1, message2)).isEqualTo(1);
	}

	@Test
	public void testEqualWithDefaultValues() {
		Comparator<Message<?>> comparator = new MessageSequenceComparator();
		Message<String> message1 = MessageBuilder.withPayload("test1").build();
		Message<String> message2 = MessageBuilder.withPayload("test2").build();
		assertThat(comparator.compare(message1, message2)).isEqualTo(0);
	}

}
