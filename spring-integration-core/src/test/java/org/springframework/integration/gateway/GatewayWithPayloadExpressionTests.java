/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.core.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
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
	public void payloadExpressionMap() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("foo", "FOO");
		gateway.send4(map);
		Message<?> result = input.receive(0);
		assertEquals(map, ((Object[])result.getPayload())[0]);
		assertNull(result.getHeaders().get("foo"));
	}

	@Test
	public void payloadExpressionMaps() throws Exception {
		Map<String, Object> mapA = new HashMap<String, Object>();
		mapA.put("foo", "FOO");
		Map<String, Object> mapB = new HashMap<String, Object>();
		mapB.put("bar", "BAR");
		gateway.send5(mapA, mapB);
		Message<?> result = input.receive(0);
		assertEquals(mapA, ((Object[])result.getPayload())[0]);
		assertEquals(mapB, ((Object[])result.getPayload())[1]);
	}

	@Test
	public void payloadExpressionMapsOneIsNotHeaders() throws Exception {
		Map<String, Object> mapA = new HashMap<String, Object>();
		mapA.put("foo", "FOO");
		Map<Object, Object> mapB = new HashMap<Object, Object>();
		mapB.put(1, "1");
		gateway.send6(mapA, mapB);
		Message<?> result = input.receive(0);
		System.out.println(result);
		assertEquals(mapA, ((Object[])result.getPayload())[0]);
		assertEquals(mapB, ((Object[])result.getPayload())[1]);
	}


	public static interface SampleGateway {

		void send1(String value);

		void send2(String value);

		void send3();

		void send4(Map<String, ?> map);

		void send5(Map<String, ?> mapA, Map<String, ?> mapB);

		void send6(Map<String, ?> mapA, Map<Object, ?> mapB);
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
