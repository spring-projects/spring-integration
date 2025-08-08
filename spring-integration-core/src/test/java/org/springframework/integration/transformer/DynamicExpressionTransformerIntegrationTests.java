/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

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
public class DynamicExpressionTransformerIntegrationTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Test
	public void transformWithDynamicExpression() {
		Message<?> message = MessageBuilder.withPayload(new TestBean()).setHeader("bar", 123).build();
		this.input.send(message);
		Message<?> result = output.receive(0);
		assertThat(result.getPayload()).isEqualTo("test123");
	}

	static class TestBean {

		public String getFoo() {
			return "test";
		}

	}

}
