/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.integration.annotation.Header;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class HeaderEnrichedGatewayTests {

	@Autowired
	private SampleGateway gatewayWithHeaderValues;

	@Autowired
	private SampleGateway gatewayWithHeaderExpressions;

	@Autowired
	private PollableChannel channel;

	private Object testPayload;


	@Test
	public void validateHeaderValueMappings() throws Exception {
		testPayload = "hello";
		gatewayWithHeaderValues.sendString((String) testPayload);
		Message<?> message1 = channel.receive(0);
		assertEquals(testPayload, message1.getPayload());
		assertEquals("foo", message1.getHeaders().get("foo"));
		assertEquals("bar", message1.getHeaders().get("bar"));
		assertNull(message1.getHeaders().get("baz"));

		testPayload = 123;
		gatewayWithHeaderValues.sendInteger((Integer) testPayload);
		Message<?> message2 = channel.receive(0);
		assertEquals(testPayload, message2.getPayload());
		assertEquals("foo", message2.getHeaders().get("foo"));
		assertEquals("bar", message2.getHeaders().get("bar"));
		assertNull(message2.getHeaders().get("baz"));

		testPayload = "withAnnotatedHeaders";
		gatewayWithHeaderValues.sendStringWithParameterHeaders((String) testPayload, "headerA", "headerB");
		Message<?> message3 = channel.receive(0);
		assertEquals("foo", message3.getHeaders().get("foo"));
		assertEquals("bar", message3.getHeaders().get("bar"));
		assertEquals("headerA", message3.getHeaders().get("headerA"));
		assertEquals("headerB", message3.getHeaders().get("headerB"));
	}

	@Test
	public void validateHeaderExpressionMappings() throws Exception {
		testPayload = "hello";
		gatewayWithHeaderExpressions.sendString((String) testPayload);
		Message<?> message1 = channel.receive(0);
		assertEquals(testPayload, message1.getPayload());
		assertEquals(42, message1.getHeaders().get("foo"));
		assertEquals("foobar", message1.getHeaders().get("bar"));
		assertNull(message1.getHeaders().get("baz"));

		testPayload = 123;
		gatewayWithHeaderExpressions.sendInteger((Integer) testPayload);
		Message<?> message2 = channel.receive(0);
		assertEquals(testPayload, message2.getPayload());
		assertEquals(42, message2.getHeaders().get("foo"));
		assertEquals("foobar", message2.getHeaders().get("bar"));
		assertNull(message2.getHeaders().get("baz"));

		testPayload = "withAnnotatedHeaders";
		gatewayWithHeaderExpressions.sendStringWithParameterHeaders((String) testPayload, "headerA", "headerB");
		Message<?> message3 = channel.receive(0);
		assertEquals(42, message3.getHeaders().get("foo"));
		assertEquals("foobar", message3.getHeaders().get("bar"));
		assertEquals("headerA", message3.getHeaders().get("headerA"));
		assertEquals("headerB", message3.getHeaders().get("headerB"));
	}


	public static interface SampleGateway {

		public void sendString(String value);

		public void sendInteger(Integer value);

		public void sendStringWithParameterHeaders(String value,
				@Header("headerA") String headerA, @Header("headerB") String headerB);
	}

}
