/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class HeaderValueRouterParserTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private TestServiceA testServiceA;

	@Autowired
	private TestServiceB testServiceB;

	@Test
	public void testHeaderValuesAsChannels() {
		context.start();
		MessageBuilder<?> channel1MessageBuilder = MessageBuilder.withPayload("");
		channel1MessageBuilder.setHeader("testHeader", "channel1");
		Message<?> message1 = channel1MessageBuilder.build();
		MessageBuilder<?> channel2MessageBuilder = MessageBuilder.withPayload("");
		channel2MessageBuilder.setHeader("testHeader", "channel2");
		Message<?> message2 = channel2MessageBuilder.build();
		testServiceA.foo(message1);
		testServiceA.foo(message2);
		PollableChannel channel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel channel2 = (PollableChannel) context.getBean("channel2");
		message1 = channel1.receive();
		assertThat(message1.getHeaders().get("testHeader")).isEqualTo("channel1");
		message2 = channel2.receive();
		assertThat(message2.getHeaders().get("testHeader")).isEqualTo("channel2");
	}

	@Test
	public void testHeaderValuesWithMapResolver() {
		context.start();
		MessageBuilder<?> channel1MessageBuilder = MessageBuilder.withPayload("");
		channel1MessageBuilder.setHeader("testHeader", "1");
		Message<?> message1 = channel1MessageBuilder.build();
		MessageBuilder<?> channel2MessageBuilder = MessageBuilder.withPayload("");
		channel2MessageBuilder.setHeader("testHeader", "2");
		Message<?> message2 = channel2MessageBuilder.build();
		testServiceB.foo(message1);
		testServiceB.foo(message2);
		PollableChannel channel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel channel2 = (PollableChannel) context.getBean("channel2");
		message1 = channel1.receive();
		assertThat(message1.getHeaders().get("testHeader")).isEqualTo("1");
		message2 = channel2.receive();
		assertThat(message2.getHeaders().get("testHeader")).isEqualTo("2");
	}

	public interface TestServiceA {

		void foo(Message<?> message);

	}

	public interface TestServiceB {

		void foo(Message<?> message);

	}

}
