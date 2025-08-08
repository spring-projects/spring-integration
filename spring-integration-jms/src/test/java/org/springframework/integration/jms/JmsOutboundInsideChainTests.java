/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 */
@SpringJUnitConfig
@DirtiesContext
public class JmsOutboundInsideChainTests extends ActiveMQMultiContextTests {

	@Autowired
	private MessageChannel outboundChainChannel;

	@Autowired
	private PollableChannel receiveChannel;

	@Autowired
	private MessageChannel outboundGatewayChainChannel;

	@Autowired
	private PollableChannel repliesChannel;

	@Test
	public void testJmsOutboundChannelInsideChain() {
		String testString = "test";
		Message<String> shippedMessage = MessageBuilder.withPayload(testString).build();
		this.outboundChainChannel.send(shippedMessage);
		Message<?> receivedMessage = this.receiveChannel.receive(2000);
		assertThat(receivedMessage.getPayload()).isEqualTo(testString);
	}

	@Test
	public void testJmsOutboundGatewayRequiresReply() {
		this.outboundGatewayChainChannel.send(MessageBuilder.withPayload("test").build());
		assertThat(this.repliesChannel.receive(2000)).isNotNull();

		this.outboundGatewayChainChannel.send(MessageBuilder.withPayload("test").build());
		assertThat(this.repliesChannel.receive(2000)).isNull();
	}

}
