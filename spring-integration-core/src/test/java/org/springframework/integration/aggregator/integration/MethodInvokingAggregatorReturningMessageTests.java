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
