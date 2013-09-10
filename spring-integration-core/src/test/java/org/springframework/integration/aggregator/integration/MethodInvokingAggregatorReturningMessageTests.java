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

import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MethodInvokingAggregatorReturningMessageTests {

	@Autowired
	DirectChannel pojoInput;

	@Autowired
	DirectChannel defaultInput;

	@Autowired
	PollableChannel pojoOutput;

	@Autowired
	PollableChannel defaultOutput;


	@Test // INT-1107
	public void messageReturningPojoAggregatorResultIsNotWrappedInAnotherMessage() {
		List<String> payload = Collections.singletonList("test");
		pojoInput.send(MessageBuilder.withPayload(payload).build());
		Message<?> result = pojoOutput.receive();
		assertFalse(Message.class.isAssignableFrom(result.getPayload().getClass()));
	}

	@Test
	public void defaultAggregatorResultIsNotWrappedInAnotherMessage() {
		List<String> payload = Collections.singletonList("test");
		defaultInput.send(MessageBuilder.withPayload(payload).build());
		Message<?> result = defaultOutput.receive();
		assertFalse(Message.class.isAssignableFrom(result.getPayload().getClass()));
	}


	@SuppressWarnings("unused")
	private static class TestAggregator {

		public Message<?> aggregate(final List<Message<?>> messages) {
			List<String> payload = Collections.singletonList("foo");
			return MessageBuilder.withPayload(payload).setHeader("bar", 123).build();
		}
	}

}
