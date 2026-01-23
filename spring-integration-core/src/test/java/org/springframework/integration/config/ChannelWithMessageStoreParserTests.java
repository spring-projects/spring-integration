/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.PriorityCapableChannelMessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class ChannelWithMessageStoreParserTests {

	private static final String BASE_PACKAGE = "org.springframework.integration";

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	private TestHandler handler;

	@Autowired
	@Qualifier("messageStore")
	private MessageGroupStore messageGroupStore;

	@Autowired
	@Qualifier("priority")
	private PollableChannel priorityChannel;

	@Autowired
	@Qualifier("priorityMessageStore")
	private MessageGroupStore priorityMessageStore;

	@Test
	public void testActivatorSendsToPersistentQueue() throws Exception {

		input.send(createMessage("123", "id1", 3, 1, null));
		handler.getLatch().await(100, TimeUnit.MILLISECONDS);
		assertThat(handler.getMessageString()).as("The message payload is not correct").isEqualTo("123");
		// The group id for buffered messages is the channel name
		assertThat(messageGroupStore.getMessageGroup("messageStore:output").size()).isEqualTo(1);

		Message<?> result = output.receive(100);
		assertThat(result.getPayload()).isEqualTo("hello");
		assertThat(messageGroupStore.getMessageGroup(BASE_PACKAGE + ".store:output").size()).isEqualTo(0);

	}

	@Test
	public void testPriorityMessageStore() {
		assertThat(TestUtils.<Object>getPropertyValue(this.priorityChannel, "queue.messageGroupStore"))
				.isSameAs(this.priorityMessageStore);
		assertThat(this.priorityChannel).isInstanceOf(PriorityChannel.class);
	}

	private static <T> Message<T> createMessage(T payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel outputChannel) {

		return MessageBuilder.withPayload(payload).setCorrelationId(correlationId).setSequenceSize(sequenceSize)
				.setSequenceNumber(sequenceNumber).setReplyChannel(outputChannel).build();
	}

	public static class DummyPriorityMS extends SimpleMessageStore implements PriorityCapableChannelMessageStore {

		@Override
		public boolean isPriorityEnabled() {
			return true;
		}

	}

}
