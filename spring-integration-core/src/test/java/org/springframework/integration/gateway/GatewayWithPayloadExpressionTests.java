/*
 * Copyright 2002-2010 the original author or authors.
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

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.support.converter.MessageConversionException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GatewayWithPayloadExpressionTests {

	@Autowired
	private SampleGateway gateway;

	@Autowired
	private SampleAnnotatedGateway annotatedGateway;

	@Autowired
	private GatewayWithMapThatIsNotHeaders gatewayWithMapThatIsNotHeaders;

	@Autowired
	private PollableChannel input;


	@Test
	public void simpleExpression() throws Exception {
		gateway.send1("foo");
		Message<?> result = input.receive(0);
		assertEquals("foobar", result.getPayload());
	}

	@Test
	public void beanResolvingExpression() throws Exception {
		gateway.send2("foo");
		Message<?> result = input.receive(0);
		assertEquals(324, result.getPayload());
	}

	@Test
	public void payloadAnnotationExpression() throws Exception {
		annotatedGateway.send("foo", "bar");
		Message<?> result = input.receive(0);
		assertEquals("foobar", result.getPayload());
	}

	@Test
	public void noArgMethodWithPayloadExpression() throws Exception {
		gateway.send3();
		Message<?> result = input.receive(0);
		assertEquals("send3", result.getPayload());
	}

	@Test
	public void gatewayWithMapThatIsNotHeaders() throws Exception {
		Map<Integer, Object> map = new HashMap<Integer, Object>();
		map.put(1,  "Hello");
		Map<String, Object> headersA = new HashMap<String, Object>();
		headersA.put("foo",  "FOO");
		headersA.put("bar",  "BAR");
		Map<String, Object> headersB = new HashMap<String, Object>();
		headersB.put("baz",  "BAZ");

		gatewayWithMapThatIsNotHeaders.sendHeaderMapNoAnnotationNoExpression(headersA);
		Message<?> result = input.receive(0);
		System.out.println(result);
		assertEquals(headersA, result.getPayload());
		assertNull(result.getHeaders().get("foo"));
		assertNull(result.getHeaders().get("bar"));

		gatewayWithMapThatIsNotHeaders.sendNonHeaderMapNoAnnotationNoExpression(map);
		result = input.receive(0);
		assertEquals(map, result.getPayload());

		try {
			gatewayWithMapThatIsNotHeaders.sendMapPojoNoAnnotationNoExpression(map, "Hello");
			fail();
		}
		catch (MessageConversionException e) {
			// expected/ignore
		}

		gatewayWithMapThatIsNotHeaders.sendMapPojoAnnotatedHeader(map, "Hello");
		result = input.receive(0);
		assertEquals(map, result.getPayload());
		assertEquals("Hello", result.getHeaders().get("foo"));

		gatewayWithMapThatIsNotHeaders.sendMapPojoExpressionHeader(map, "Hello");
		result = input.receive(0);
		assertEquals(map, result.getPayload());
		assertEquals("Hello", result.getHeaders().get("foo"));

		gatewayWithMapThatIsNotHeaders.sendMapMapNoAnnotationNoExpressionOneMatchingHeaders(map, headersA);
		result = input.receive(0);
		assertEquals(map, result.getPayload());
		assertEquals("FOO", result.getHeaders().get("foo"));
		assertEquals("BAR", result.getHeaders().get("bar"));

		gatewayWithMapThatIsNotHeaders.sendMapMapNoAnnotationNoExpressionTwoMatchingHeaders(headersA, headersA);
		result = input.receive(0);
		assertEquals(headersA, result.getPayload());
		assertEquals("FOO", result.getHeaders().get("foo"));
		assertEquals("BAR", result.getHeaders().get("bar"));

		gatewayWithMapThatIsNotHeaders.sendMapMapMapNoAnnotationNoExpressionThreeMatchingHeaders(headersA, headersA, headersB);
		result = input.receive(0);
		assertEquals(headersA, result.getPayload());
		assertEquals("FOO", result.getHeaders().get("foo"));
		assertEquals("BAR", result.getHeaders().get("bar"));
		assertEquals("BAZ", result.getHeaders().get("baz"));

		try {
			gatewayWithMapThatIsNotHeaders.sendMapMapMapTwoMatchingPayload(map, map, headersA);
			fail();
		}
		catch (MessageConversionException e) {
			// expected/ignore
		}

		gatewayWithMapThatIsNotHeaders.sendMapMapMapWithExpression(map, map, headersA);
		result = input.receive(0);
		assertEquals(map, ((Object[])result.getPayload())[0]);
		assertEquals(map, ((Object[])result.getPayload())[1]);
		assertEquals(headersA, result.getHeaders().get("foo"));
	}

	public static interface GatewayWithMapThatIsNotHeaders {

		public void sendHeaderMapNoAnnotationNoExpression(Map<String, Object> map);

		public void sendNonHeaderMapNoAnnotationNoExpression(Map<Integer, Object> map);

		public void sendMapPojoNoAnnotationNoExpression(Map<Integer, Object> map, String foo);

		public void sendMapPojoAnnotatedHeader(Map<Integer, Object> map, @Header("foo") String foo);

		public void sendMapPojoExpressionHeader(Map<Integer, Object> map, @Header("foo") String foo);

		public void sendMapMapNoAnnotationNoExpressionOneMatchingHeaders(Map<Integer, Object> map, Map<String, Object> headers);

		public void sendMapMapNoAnnotationNoExpressionTwoMatchingHeaders(Map<String, Object> map, Map<String, Object> headers);

		// will merge between two or override
		public void sendMapMapMapNoAnnotationNoExpressionThreeMatchingHeaders(Map<String, Object> map1, Map<String, Object> map2, Map<String, Object> headers);

		public void sendMapMapMapTwoMatchingPayload(Map<Integer, Object> map1, Map<Integer, Object> map2, Map<String, Object> headers);

		public void sendMapMapMapWithExpression(Map<Integer, Object> map1, Map<Integer, Object> map2, Map<String, Object> headers);
	}


	public static interface SampleGateway {

		void send1(String value);

		void send2(String value);

		void send3();
	}


	public static interface SampleAnnotatedGateway {

		@Payload("#args[0] + #args[1]")
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
