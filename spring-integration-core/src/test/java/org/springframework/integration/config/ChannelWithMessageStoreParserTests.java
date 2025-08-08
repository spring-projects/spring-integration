/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

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
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
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
	@DirtiesContext
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
	@DirtiesContext
	public void testPriorityMessageStore() {
		assertThat(TestUtils.getPropertyValue(this.priorityChannel, "queue.messageGroupStore"))
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
