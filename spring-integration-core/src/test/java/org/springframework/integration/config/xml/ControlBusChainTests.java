/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ControlBusChainTests {

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
		assertThat(output.receive(0).getPayload()).isEqualTo("catbar");
		assertThat(output.receive(0)).isNull();
	}

	public static class Service {

		@ManagedOperation
		public String convert(String input) {
			return "cat";
		}

	}

	public static class AdapterService {

		public Message<String> receive() {
			return new GenericMessage<>(new Date().toString());
		}

	}

}
