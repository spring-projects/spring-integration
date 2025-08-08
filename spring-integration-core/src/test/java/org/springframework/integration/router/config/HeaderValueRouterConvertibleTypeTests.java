/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router.config;

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
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class HeaderValueRouterConvertibleTypeTests {

	@Autowired
	private MessageChannel inputChannel;

	@Autowired
	private PollableChannel trueChannel;

	@Autowired
	private PollableChannel falseChannel;

	@Test
	public void testBooleanValueMappingToChannels() {
		Message<?> trueMessage = MessageBuilder.withPayload(1)
				.setHeader("testHeader", true).build();
		Message<?> falseMessage = MessageBuilder.withPayload(0)
				.setHeader("testHeader", false).build();
		inputChannel.send(trueMessage);
		inputChannel.send(falseMessage);
		Message<?> trueResult = trueChannel.receive();
		assertThat(trueResult).isNotNull();
		assertThat(trueResult.getPayload()).isEqualTo(1);
		Message<?> falseResult = falseChannel.receive();
		assertThat(falseResult).isNotNull();
		assertThat(falseResult.getPayload()).isEqualTo(0);
	}

}
