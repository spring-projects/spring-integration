/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.aggregator.scenarios;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests courtesy of Sean Crotty (INT-1093)
 *
 * @author Iwein Fuld
 */
@ContextConfiguration(locations = {"aggregation-resend-config.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregationResendTest {

	@Autowired
	DirectChannel input_for_aggregator_with_explicit_timeout;

	@Autowired
	DirectChannel input_for_aggregator_without_explicit_timeout;

	@Autowired
	PollableChannel reply;

	@Test
	/**
	 * We expect to get back only one Message from the aggregator. We set no
	 * explicit timeout value on the aggregator. But apparently is automatically
	 * times out after 60 seconds. So What we'll see is that we get one aggregate
	 * Message back immediately. Then we'll get another 3 after the 60 seconds.
	 */
	public void testAggregatorWithoutExplicitTimeoutReturnsOnlyOneMessage() throws Exception {
		sendMessage(input_for_aggregator_with_explicit_timeout, 2000);
	}

	@Test
	/**
	 * We expect to get back only one Message from the aggregator. We set an
	 * explicit timeout value of 1 second on the aggregator. What we'll see is
	 * that we get one aggregate Message back immediately. Then we'll get another
	 * 3 after the 1 second.
	 */
	public void testAggregatorWithTimeoutReturnsOnlyOneMessage() throws Exception {
		sendMessage(input_for_aggregator_without_explicit_timeout, 62000);
	}

	private void sendMessage(DirectChannel channel, int waitSeconds) {
		List<String> list = new ArrayList<String>();
		list.add("foo");
		list.add("bar");
		list.add("baz");

		reply.purge(null);
		channel.send(MessageBuilder.withPayload(list).setReplyChannel(reply).build());

		Message<?> replyMessage;
		int messageCount = 0;
		do {
			replyMessage = reply.receive(waitSeconds);
			if (null != replyMessage) {
				System.out.println("Message Received: " + replyMessage);
				messageCount++;
			}
		} while (null != replyMessage);

		Assert.assertEquals(1, messageCount);
	}
}
