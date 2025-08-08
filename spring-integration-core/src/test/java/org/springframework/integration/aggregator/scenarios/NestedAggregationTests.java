/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator.scenarios;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Dave Syer
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class NestedAggregationTests {

	@Autowired
	DirectChannel splitter;

	@Autowired
	DirectChannel router;

	@Test
	public void testAggregatorWithNestedSplitter() {
		Message<?> input = new GenericMessage<>(
				Arrays.asList(
						Arrays.asList("foo", "bar", "spam"),
						Arrays.asList("bar", "foo")));
		List<String> result = sendAndReceiveMessage(splitter, 2000, input);
		assertThat(result).as("Expected result and got null").isNotNull();
		assertThat(result.toString()).isEqualTo("[[foo, bar, spam], [bar, foo]]");
	}

	@Test
	public void testAggregatorWithNestedRouter() {
		Message<?> input = new GenericMessage<>(Arrays.asList("bar", "foo"));
		List<String> result = sendAndReceiveMessage(router, 2000, input);
		assertThat(result).as("Expected result and got null").isNotNull();
		assertThat(result.toString()).isEqualTo("[[bar, foo], [bar, foo]]");
	}

	private List<String> sendAndReceiveMessage(DirectChannel channel, int timeout, Message<?> input) {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(timeout);

		@SuppressWarnings("unchecked")
		Message<List<String>> message = (Message<List<String>>) messagingTemplate.sendAndReceive(channel, input);

		return message == null ? null : message.getPayload();

	}

}
