/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.history.MessageHistory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class MessageHistoryParameterTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Test
	public void test() {
		input.send(new GenericMessage<>("foo"));
		assertThat(output.receive(10000)).isNotNull();
	}

	public static class MessageHistoryAwareTransformer {

		@Transformer
		public Object transform(@Headers MessageHeaders headers,
				@Header("history") MessageHistory history, @Payload Object payload) {

			return payload;
		}

	}

}
