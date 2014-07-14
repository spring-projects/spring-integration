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

package org.springframework.integration.aggregator.integration;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.CorrelationStrategy;
import org.springframework.integration.annotation.ReleaseStrategy;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AnnotationAggregatorTests {

	@Autowired
	DirectChannel input;

	@Autowired
	PollableChannel output;


	@Test
	public void testAggregationWithAnnotationStrategies() {
		input.send(MessageBuilder.withPayload("a").build());
		input.send(MessageBuilder.withPayload("b").build());
		@SuppressWarnings("unchecked")
		Message<String> result = (Message<String>) output.receive();
		String payload = result.getPayload();
		assertTrue("Wrong payload: "+payload, payload.matches(".*Payload.*?=a.*"));
		assertTrue("Wrong payload: "+payload, payload.matches(".*Payload.*?=b.*"));
	}

	@SuppressWarnings("unused")
	private static class TestAggregator {

		@Aggregator
		public Message<?> aggregate(final List<Message<?>> messages) {
			return MessageBuilder.withPayload(messages.toString()).build();
		}

		@ReleaseStrategy
		public boolean release(final List<Message<?>> messages) {
			return messages.size()>1;
		}

		@CorrelationStrategy
		public Object getKey(Message<?> message) {
			return "1";
		}
	}

}
