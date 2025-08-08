/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class ControlBusPollerTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Test
	public void testDefaultEvaluationContext() {
		Message<?> message =
				MessageBuilder.withPayload("@service.convert('aardvark')+headers.foo")
						.setHeader("foo", "bar")
						.build();
		this.input.send(message);
		assertThat(output.receive(1000).getPayload()).isEqualTo("catbar");
		assertThat(output.receive(0)).isNull();
	}

	public static class Service {

		public String convert(String input) {
			return "cat";
		}

	}

}
