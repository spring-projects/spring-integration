/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class MockHandlerTests {

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	private TestInterface mock;

	@Test
	public void configOk() {
		QueueChannel output = new QueueChannel();
		Mockito.when(mock.test("foo")).thenReturn("bar");
		input.send(MessageBuilder.withPayload("foo").setReplyChannel(output).build());
		Message<?> result = output.receive(0);
		assertThat(result.getPayload()).isEqualTo("bar");
	}

	public interface TestInterface {

		String test(String input);

	}

}
