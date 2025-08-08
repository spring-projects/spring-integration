/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.mockito.Mockito.verify;

/**
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class ServiceActivatorOnMockitoMockTests {

	@Autowired
	@Qualifier("in")
	MessageChannel in;

	@Autowired
	@Qualifier("out")
	PollableChannel out;

	public static class SingleAnnotatedMethodOnClass {

		@ServiceActivator
		public String move(String s) {
			return s;
		}

	}

	@Autowired
	SingleAnnotatedMethodOnClass singleAnnotatedMethodOnClass;

	@Test
	public void shouldInvokeMockedSingleAnnotatedMethodOnClass() {
		in.send(MessageBuilder.withPayload("singleAnnotatedMethodOnClass").build());
		verify(singleAnnotatedMethodOnClass).move("singleAnnotatedMethodOnClass");
	}

	public static class SingleMethodOnClass {

		public String move(String s) {
			return s;
		}

	}

	@Autowired
	SingleMethodOnClass singleMethodOnClass;

	@Test
	public void shouldInvokeMockedSingleMethodOnClass() {
		in.send(MessageBuilder.withPayload("SingleMethodOnClass").build());
		verify(singleMethodOnClass).move("SingleMethodOnClass");
	}

	@Autowired
	SingleMethodAcceptingHeaderOnClass singleMethodAcceptingHeaderOnClass;

	@Test
	public void shouldInvokeMockedSingleMethodAcceptingHeaderOnClass() {
		in.send(MessageBuilder.withPayload("SingleMethodAcceptingHeaderOnClass")
				.setHeader("s", "SingleMethodAcceptingHeaderOnClass")
				.build());
		verify(singleMethodAcceptingHeaderOnClass).move("SingleMethodAcceptingHeaderOnClass");
	}

	public static class SingleMethodAcceptingHeaderOnClass {

		public String move(@Header("s") String s) {
			return s;
		}

	}

}
