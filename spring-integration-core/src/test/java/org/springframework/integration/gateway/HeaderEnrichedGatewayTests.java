/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class HeaderEnrichedGatewayTests {

	@Autowired
	private SampleGateway gatewayWithHeaderValues;

	@Autowired
	private SampleGateway gatewayWithHeaderExpressions;

	@Autowired
	private PollableChannel channel;

	private Object testPayload;

	@Test
	public void validateHeaderValueMappings() {
		testPayload = "hello";
		gatewayWithHeaderValues.sendString((String) testPayload);
		Message<?> message1 = channel.receive(0);
		assertThat(message1.getPayload()).isEqualTo(testPayload);
		assertThat(message1.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(message1.getHeaders().get("bar")).isEqualTo("bar");
		assertThat(message1.getHeaders().get("baz")).isNull();

		testPayload = 123;
		gatewayWithHeaderValues.sendInteger((Integer) testPayload);
		Message<?> message2 = channel.receive(0);
		assertThat(message2.getPayload()).isEqualTo(testPayload);
		assertThat(message2.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(message2.getHeaders().get("bar")).isEqualTo("bar");
		assertThat(message2.getHeaders().get("baz")).isNull();

		testPayload = "withAnnotatedHeaders";
		gatewayWithHeaderValues.sendStringWithParameterHeaders((String) testPayload, "headerA", "headerB");
		Message<?> message3 = channel.receive(0);
		assertThat(message3.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(message3.getHeaders().get("bar")).isEqualTo("bar");
		assertThat(message3.getHeaders().get("headerA")).isEqualTo("headerA");
		assertThat(message3.getHeaders().get("headerB")).isEqualTo("headerB");
	}

	@Test
	public void validateHeaderExpressionMappings() {
		testPayload = "hello";
		gatewayWithHeaderExpressions.sendString((String) testPayload);
		Message<?> message1 = channel.receive(0);
		assertThat(message1.getPayload()).isEqualTo(testPayload);
		assertThat(message1.getHeaders().get("foo")).isEqualTo(42);
		assertThat(message1.getHeaders().get("bar")).isEqualTo("foobar");
		assertThat(message1.getHeaders().get("baz")).isNull();

		testPayload = 123;
		gatewayWithHeaderExpressions.sendInteger((Integer) testPayload);
		Message<?> message2 = channel.receive(0);
		assertThat(message2.getPayload()).isEqualTo(testPayload);
		assertThat(message2.getHeaders().get("foo")).isEqualTo(42);
		assertThat(message2.getHeaders().get("bar")).isEqualTo("foobar");
		assertThat(message2.getHeaders().get("baz")).isNull();

		testPayload = "withAnnotatedHeaders";
		gatewayWithHeaderExpressions.sendStringWithParameterHeaders((String) testPayload, "headerA", "headerB");
		Message<?> message3 = channel.receive(0);
		assertThat(message3.getHeaders().get("foo")).isEqualTo(42);
		assertThat(message3.getHeaders().get("bar")).isEqualTo("foobar");
		assertThat(message3.getHeaders().get("headerA")).isEqualTo("headerA");
		assertThat(message3.getHeaders().get("headerB")).isEqualTo("headerB");
	}

	public interface SampleGateway {

		void sendString(String value);

		void sendInteger(Integer value);

		void sendStringWithParameterHeaders(String value,
				@Header("headerA") String headerA, @Header("headerB") String headerB);

	}

}
