/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.predicate.MessagePredicate;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class BridgeParserTests {

	@Autowired
	@Qualifier("pollableChannel")
	private PollableChannel pollableChannel;

	@Autowired
	@Qualifier("subscribableChannel")
	private MessageChannel subscribableChannel;

	@Autowired
	@Qualifier("stopperChannel")
	private MessageChannel stopperChannel;

	@Autowired
	@Qualifier("output1")
	private PollableChannel output1;

	@Autowired
	@Qualifier("output2")
	private PollableChannel output2;

	@Autowired
	private EventDrivenConsumer bridgeWithSendTimeout;

	@Test
	public void pollableChannel() {
		Message<?> message = new GenericMessage<>("test1");
		this.pollableChannel.send(message);
		Message<?> reply = this.output1.receive(6000);
		assertThat(reply).isNotNull();
		assertThat(message).matches(new MessagePredicate(reply));
	}

	@Test
	public void subscribableChannel() {
		Message<?> message = new GenericMessage<>("test2");
		this.subscribableChannel.send(message);
		Message<?> reply = this.output2.receive(0);
		assertThat(reply).isNotNull();
		assertThat(message).matches(new MessagePredicate(reply));
	}

	@Test
	public void stopperWithReplyHeader() {
		PollableChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test3").setReplyChannel(replyChannel).build();
		this.stopperChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(message).matches(new MessagePredicate(reply));
	}

	@Test
	public void stopperWithoutReplyHeader() {
		Message<?> message = MessageBuilder.withPayload("test3").build();
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.stopperChannel.send(message));
	}

	@Test
	public void bridgeWithSendTimeout() {
		assertThat(TestUtils.getPropertyValue(bridgeWithSendTimeout, "handler.messagingTemplate.sendTimeout"))
				.isEqualTo(1234L);
	}

}
