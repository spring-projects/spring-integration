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
import org.springframework.messaging.handler.annotation.Payload;
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
