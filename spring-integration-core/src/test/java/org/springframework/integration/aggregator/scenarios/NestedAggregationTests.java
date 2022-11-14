/*
 * Copyright 2002-2022 the original author or authors.
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
