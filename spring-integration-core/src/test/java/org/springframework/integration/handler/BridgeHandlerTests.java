/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.junit.jupiter.api.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.predicate.MessagePredicate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Artem Bilan
 */
public class BridgeHandlerTests {

	private final BridgeHandler handler = new BridgeHandler();

	@Test
	public void simpleBridge() {
		QueueChannel outputChannel = new QueueChannel();
		this.handler.setOutputChannel(outputChannel);
		Message<?> request = new GenericMessage<>("test");
		this.handler.handleMessage(request);
		Message<?> reply = outputChannel.receive(0);
		assertThat(reply)
				.isNotNull()
				.matches(new MessagePredicate(request));
	}

	@Test
	public void missingOutputChannelVerifiedAtRuntime() {
		Message<?> request = new GenericMessage<>("test");

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.handler.handleMessage(request))
				.withCauseInstanceOf(DestinationResolutionException.class);
	}

	@Test
	public void missingOutputChannelAllowedForReplyChannelMessages() {
		PollableChannel replyChannel = new QueueChannel();
		Message<String> request = MessageBuilder.withPayload("tst").setReplyChannel(replyChannel).build();
		this.handler.handleMessage(request);
		assertThat(replyChannel.receive(10_000))
				.isNotNull()
				.matches(new MessagePredicate(request));
	}

}
