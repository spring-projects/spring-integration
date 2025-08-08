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
public class DynamicExpressionRouterIntegrationTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel even;

	@Autowired
	private PollableChannel odd;

	@Test
	public void dynamicExpressionBasedRouter() {
		TestBean testBean1 = new TestBean(1);
		TestBean testBean2 = new TestBean(2);
		TestBean testBean3 = new TestBean(3);
		TestBean testBean4 = new TestBean(4);
		Message<?> message1 = MessageBuilder.withPayload(testBean1).build();
		Message<?> message2 = MessageBuilder.withPayload(testBean2).build();
		Message<?> message3 = MessageBuilder.withPayload(testBean3).build();
		Message<?> message4 = MessageBuilder.withPayload(testBean4).build();
		this.input.send(message1);
		this.input.send(message2);
		this.input.send(message3);
		this.input.send(message4);
		assertThat(odd.receive(0).getPayload()).isEqualTo(testBean1);
		assertThat(even.receive(0).getPayload()).isEqualTo(testBean2);
		assertThat(odd.receive(0).getPayload()).isEqualTo(testBean3);
		assertThat(even.receive(0).getPayload()).isEqualTo(testBean4);
		assertThat(odd.receive(0)).isNull();
		assertThat(even.receive(0)).isNull();
	}

	record TestBean(int number) {

	}

}
