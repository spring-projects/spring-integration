/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator.integration;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class MethodInvokingAggregatorReturningMessageTests {

	@Autowired
	DirectChannel pojoInput;

	@Autowired
	DirectChannel defaultInput;

	@Autowired
	PollableChannel pojoOutput;

	@Autowired
	PollableChannel defaultOutput;

	@Test
	public void messageReturningPojoAggregatorResultIsNotWrappedInAnotherMessage() {
		List<String> payload = Collections.singletonList("test");
		this.pojoInput.send(MessageBuilder.withPayload(payload).build());
		Message<?> result = this.pojoOutput.receive();
		assertThat(Message.class.isAssignableFrom(result.getPayload().getClass())).isFalse();
	}

	@Test
	public void defaultAggregatorResultIsNotWrappedInAnotherMessage() {
		List<String> payload = Collections.singletonList("test");
		this.defaultInput.send(MessageBuilder.withPayload(payload).build());
		Message<?> result = this.defaultOutput.receive();
		assertThat(Message.class.isAssignableFrom(result.getPayload().getClass())).isFalse();
	}

	@SuppressWarnings("unused")
	private static class TestAggregator {

		public Message<?> aggregate(final List<Message<?>> messages) {
			List<String> payload = Collections.singletonList("foo");
			return MessageBuilder.withPayload(payload).setHeader("bar", 123).build();
		}

	}

}
