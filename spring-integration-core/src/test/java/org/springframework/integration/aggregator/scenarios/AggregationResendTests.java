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

package org.springframework.integration.aggregator.scenarios;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests courtesy of Sean Crotty (INT-1093)
 *
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregationResendTests {

	@Autowired
	DirectChannel input_for_aggregator_with_explicit_timeout;

	@Autowired
	DirectChannel input_for_aggregator_without_explicit_timeout;

	@Autowired
	QueueChannel reply;

	/**
	 * We expect to get back only one Message from the aggregator. We set an
	 * explicit timeout value of 1 second on the aggregator. What we'll see is
	 * that we get one aggregate Message back immediately.
	 *
	 * <p>We should <em>not</em> get another 3 after the 1 second.
	 */
	@Test
	@Ignore // timeout is no longer supported
	public void testAggregatorWithoutExplicitTimeoutReturnsOnlyOneMessage() {
		sendMessage(this.input_for_aggregator_with_explicit_timeout, 2000);
	}

	/**
	 * We expect to get back only one Message from the aggregator. We set no
	 * explicit timeout value on the aggregator, but it automatically times out
	 * after 60 seconds. What we'll see is that we get one aggregate Message back
	 * immediately.
	 *
	 * <p>We should <em>not</em> get another 3 after the 60 seconds.
	 */
	@Test
	@Ignore // disabling from normal testing, should be the same behavior whether explicit or default
	public void testAggregatorWithTimeoutReturnsOnlyOneMessage() {
		sendMessage(this.input_for_aggregator_without_explicit_timeout, 62000);
	}

	private void sendMessage(DirectChannel channel, int waitSeconds) {
		List<String> list = new ArrayList<>();
		list.add("foo");
		list.add("bar");
		list.add("baz");

		this.reply.purge(null);
		channel.send(MessageBuilder.withPayload(list).setReplyChannel(reply).build());

		Message<?> replyMessage;
		int messageCount = 0;
		do {
			replyMessage = reply.receive(waitSeconds);
			if (null != replyMessage) {
				messageCount++;
			}
		}
		while (null != replyMessage);

		assertThat(messageCount).isEqualTo(1);
	}

}
