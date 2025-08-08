/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class GatewayWithPayloadExpressionTests {

	@Autowired
	private SampleGateway gateway;

	@Autowired
	private SampleAnnotatedGateway annotatedGateway;

	@Autowired
	private PollableChannel input;

	@Test
	public void simpleExpression() {
		gateway.send1("foo");
		Message<?> result = input.receive(0);
		assertThat(result.getPayload()).isEqualTo("foobar");
	}

	@Test
	public void beanResolvingExpression() {
		gateway.send2("foo");
		Message<?> result = input.receive(0);
		assertThat(result.getPayload()).isEqualTo(324);
	}

	@Test
	public void payloadAnnotationExpression() {
		annotatedGateway.send("foo", "bar");
		Message<?> result = input.receive(0);
		assertThat(result.getPayload()).isEqualTo("foobar");
	}

	@Test
	public void noArgMethodWithPayloadExpression() {
		gateway.send3();
		Message<?> result = input.receive(0);
		assertThat(result.getPayload()).isEqualTo("send3");
	}

	public interface SampleGateway {

		void send1(String value);

		void send2(String value);

		void send3();

	}

	public interface SampleAnnotatedGateway {

		@Payload("args[0] + args[1]")
		void send(String value1, String value2);

	}

	public static class TestBean {

		public int sum(String s) {
			int sum = 0;
			for (byte b : s.getBytes()) {
				sum += b;
			}
			return sum;
		}

	}

}
