/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
