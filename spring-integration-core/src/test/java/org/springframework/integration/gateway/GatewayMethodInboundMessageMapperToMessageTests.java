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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class GatewayMethodInboundMessageMapperToMessageTests {

	@Test
	public void toMessageWithPayload() throws Exception {
		Method method = TestService.class.getMethod("sendPayload", String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { "test" });
		assertEquals("test", message.getPayload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void toMessageWithTooManyParameters() throws Exception {
		Method method = TestService.class.getMethod("sendPayload", String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.toMessage(new Object[] { "test" , "oops" });
	}

	@Test(expected = IllegalArgumentException.class)
	public void toMessageWithEmptyParameterArray() throws Exception {
		Method method = TestService.class.getMethod("sendPayload", String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.toMessage(new Object[] {});
	}

	@Test
	public void toMessageWithPayloadAndHeader() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeader", String.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { "test", "bar" });
		assertEquals("test", message.getPayload());
		assertEquals("bar", message.getHeaders().get("foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void toMessageWithPayloadAndRequiredHeaderButNullValue() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeader", String.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.toMessage(new Object[] { "test", null });
	}

	@Test
	public void toMessageWithPayloadAndOptionalHeaderWithValueProvided() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndOptionalHeader", String.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { "test", "bar" });
		assertEquals("test", message.getPayload());
		assertEquals("bar", message.getHeaders().get("foo"));
	}

	@Test
	public void toMessageWithPayloadAndOptionalHeaderWithNullValue() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndOptionalHeader", String.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { "test", null });
		assertEquals("test", message.getPayload());
		assertNull(message.getHeaders().get("foo"));
	}

	@Test
	public void toMessageWithPayloadAndHeadersMap() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeadersMap", String.class, Map.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("abc", 123);
		headers.put("def", 456);
		Message<?> message = mapper.toMessage(new Object[] { "test", headers });
		assertEquals("test", message.getPayload());
		assertEquals(123, message.getHeaders().get("abc"));
		assertEquals(456, message.getHeaders().get("def"));
	}

	@Test
	public void toMessageWithPayloadAndNullHeadersMap() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeadersMap", String.class, Map.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { "test", null });
		assertEquals("test", message.getPayload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void toMessageWithPayloadAndHeadersMapWithNonStringKey() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeadersMap", String.class, Map.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Map<Integer, String> headers = new HashMap<Integer, String>();
		headers.put(123, "abc");
		mapper.toMessage(new Object[] { "test", headers });
	}

	@Test
	public void toMessageWithMessageParameter() throws Exception {
		Method method = TestService.class.getMethod("sendMessage", Message.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		Message<?> message = mapper.toMessage(new Object[] { inputMessage });
		assertEquals("test message", message.getPayload());
	}

	@Test
	public void toMessageWithMessageParameterAndHeader() throws Exception {
		Method method = TestService.class.getMethod("sendMessageAndHeader", Message.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		Message<?> message = mapper.toMessage(new Object[] { inputMessage, "bar" });
		assertEquals("test message", message.getPayload());
		assertEquals("bar", message.getHeaders().get("foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void toMessageWithMessageParameterAndRequiredHeaderButNullValue() throws Exception {
		Method method = TestService.class.getMethod("sendMessageAndHeader", Message.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		mapper.toMessage(new Object[] { inputMessage, null });
	}

	@Test
	public void toMessageWithMessageParameterAndOptionalHeaderWithValue() throws Exception {
		Method method = TestService.class.getMethod("sendMessageAndOptionalHeader", Message.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		Message<?> message = mapper.toMessage(new Object[] { inputMessage, "bar" });
		assertEquals("test message", message.getPayload());
		assertEquals("bar", message.getHeaders().get("foo"));
	}

	@Test
	public void toMessageWithMessageParameterAndOptionalHeaderWithNull() throws Exception {
		Method method = TestService.class.getMethod("sendMessageAndOptionalHeader", Message.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		Message<?> message = mapper.toMessage(new Object[] { inputMessage, null });
		assertEquals("test message", message.getPayload());
		assertNull(message.getHeaders().get("foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void noArgs() throws Exception {
		Method method = TestService.class.getMethod("noArgs", new Class<?>[] {});
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.toMessage(new Object[] {});
	}

	@Test(expected = IllegalArgumentException.class)
	public void onlyHeaders() throws Exception {
		Method method = TestService.class.getMethod("onlyHeaders", String.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.toMessage(new Object[] { "abc", "def" });
	}

	@Test
	public void toMessageWithPayloadAndHeaders() throws Exception {
		Method method = TestService.class.getMethod("sendPayload", String.class);
		Map<String, Expression> headers = new HashMap<String, Expression>();
		headers.put("foo", new LiteralExpression("foo"));
		headers.put("bar", new SpelExpressionParser().parseExpression("6 * 7"));
		headers.put("baz", new LiteralExpression("hello"));
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method, headers);
		Message<?> message = mapper.toMessage(new Object[] { "test" });
		assertEquals("test", message.getPayload());
		assertEquals("foo", message.getHeaders().get("foo"));
		assertEquals(42, message.getHeaders().get("bar"));
	}

	@Test
	public void toMessageWithPayloadMap() throws Exception {
		Method method = TestService.class.getMethod("sendPayloadMap", Map.class);
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put(1, "hello");

		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { map });
		assertEquals(map, message.getPayload());
		assertNull(message.getHeaders().get(1));
	}

	@Test
	public void toMessageWithPayloadMapAndHeaders() throws Exception {
		Method method = TestService.class.getMethod("sendPayloadMapAndHeadersMap", Map.class, Map.class);
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put(1, "hello");

		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "FOO");

		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { map, headers });
		assertEquals(map, message.getPayload());
		assertEquals("FOO", message.getHeaders().get("foo"));
	}

	@Test
	public void toMessageWithPayloadMapAndHeaders2() throws Exception {
		Method method = TestService.class.getMethod("sendPayloadMapAndHeadersMap2", Map.class, Map.class);
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put(1, "hello");

		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "FOO");

		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { headers, map });
		assertEquals(map, message.getPayload());
		assertEquals("FOO", message.getHeaders().get("foo"));
	}


	private static interface TestService {

		void sendPayload(String payload);

		void sendPayloadAndHeader(String payload, @Header("foo") String foo);

		void sendPayloadAndOptionalHeader(String payload, @Header(value="foo", required=false) String foo);

		void sendPayloadAndHeadersMap(String payload, @Headers Map<String, Object> headers);

		void sendPayloadMap(Map<Object, Object> map);

		void sendPayloadMapAndHeadersMap(Map<Object, Object> map, Map<String, Object> headers);

		void sendPayloadMapAndHeadersMap2(Map<String, Object> headers, Map<Object, Object> map);

		void sendMessage(Message<?> message);

		void sendMessageAndHeader(Message<?> message, @Header("foo") String foo);

		void sendMessageAndOptionalHeader(Message<?> message, @Header(value="foo", required=false) String foo);

		// invalid methods

		void noArgs();

		void onlyHeaders(@Header("foo") String foo, @Header("bar") String bar);

	}

}
