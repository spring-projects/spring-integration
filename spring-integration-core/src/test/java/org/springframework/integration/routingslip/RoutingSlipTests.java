/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.routingslip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RoutingSlipTests {

	@Autowired
	private MessageChannel input;

	@Test
	@SuppressWarnings("unchecked")
	public void testRoutingSlip() {
		PollableChannel replyChannel = new QueueChannel();
		Message<List<String>> request = MessageBuilder.withPayload(Arrays.asList("test1", "test2"))
				.setReplyChannel(replyChannel)
				.setHeader("myRoutingSlipChannel", "channel5").build();
		this.input.send(request);
		Message<?> reply = replyChannel.receive(10000);
		assertNotNull(reply);
		List<Message<?>> messages = (List<Message<?>>) reply.getPayload();
		for (Message<?> message : messages) {
			Map<List<String>, Integer> routingSlip = message.getHeaders()
					.get(IntegrationMessageHeaderAccessor.ROUTING_SLIP, Map.class);
			assertEquals(routingSlip.keySet().iterator().next().size(), routingSlip.values().iterator().next().intValue());
			MessageHistory messageHistory = MessageHistory.read(message);
			List<String> channelNames = Arrays.asList("input", "split", "process", "channel1", "channel2",
					"channel3", "channel4", "channel5", "aggregate");
			int i = 0;
			for (Properties properties : messageHistory) {
				assertTrue(channelNames.contains(properties.getProperty("name")));
			}
		}
	}

	public static class TestRoutingSlipRoutePojo {

		final String[] channels = {"channel2", "channel3"};

		private int i = 0;

		public String get(Message<?> requestMessage, Object reply) {
			try {
				return this.channels[i++];
			}
			catch (Exception e) {
				return null;
			}
		}

	}

	public static class TestRoutingSlipRouteStrategy implements RoutingSlipRouteStrategy {

		private AtomicBoolean invoked = new AtomicBoolean();

		@Override
		public String getNextPath(Message<?> requestMessage, Object reply) {
			return !invoked.getAndSet(true) ? "channel4" : null;
		}

	}

}
