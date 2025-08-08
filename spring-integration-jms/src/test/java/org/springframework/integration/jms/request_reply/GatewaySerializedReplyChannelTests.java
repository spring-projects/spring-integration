/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.jms.request_reply;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class GatewaySerializedReplyChannelTests extends ActiveMQMultiContextTests {

	@Autowired
	MessageChannel input;

	@Autowired
	PollableChannel output;

	@Test
	public void test() {
		input.send(new GenericMessage<>("foo"));
		Message<?> reply = output.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("echo:foo");
	}

}
