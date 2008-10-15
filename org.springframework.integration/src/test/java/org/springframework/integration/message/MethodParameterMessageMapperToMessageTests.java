/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.core.Message;

/**
 * @author Mark Fisher
 */
public class MethodParameterMessageMapperToMessageTests {

	@Test
	public void toMessageWithPayload() throws Exception {
		Method method = TestService.class.getMethod("sendPayload", String.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { "test" });
		assertEquals("test", message.getPayload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void toMessageWithTooManyParameters() throws Exception {
		Method method = TestService.class.getMethod("sendPayload", String.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		mapper.toMessage(new Object[] { "test" , "oops" });
	}

	@Test(expected = IllegalArgumentException.class)
	public void toMessageWithEmptyParameterArray() throws Exception {
		Method method = TestService.class.getMethod("sendPayload", String.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		mapper.toMessage(new Object[] {});
	}

	@Test
	public void toMessageWithPayloadAndHeader() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeader", String.class, String.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { "test", "bar" });
		assertEquals("test", message.getPayload());
		assertEquals("bar", message.getHeaders().get("foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void toMessageWithPayloadAndRequiredHeaderButNullValue() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeader", String.class, String.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		mapper.toMessage(new Object[] { "test", null });
	}

	@Test
	public void toMessageWithPayloadAndOptionalHeaderWithValueProvided() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndOptionalHeader", String.class, String.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { "test", "bar" });
		assertEquals("test", message.getPayload());
		assertEquals("bar", message.getHeaders().get("foo"));
	}

	@Test
	public void toMessageWithPayloadAndOptionalHeaderWithNullValue() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndOptionalHeader", String.class, String.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { "test", null });
		assertEquals("test", message.getPayload());
		assertNull(message.getHeaders().get("foo"));
	}

	@Test
	public void toMessageWithPayloadAndHeadersMap() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeadersMap", String.class, Map.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
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
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		Message<?> message = mapper.toMessage(new Object[] { "test", null });
		assertEquals("test", message.getPayload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void toMessageWithPayloadAndHeadersMapWithNonStringKey() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeadersMap", String.class, Map.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		Map<Integer, String> headers = new HashMap<Integer, String>();
		headers.put(123, "abc");
		mapper.toMessage(new Object[] { "test", headers });
	}

	@Test
	public void toMessageWithMessageParameter() throws Exception {
		Method method = TestService.class.getMethod("sendMessage", Message.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		Message<?> message = mapper.toMessage(new Object[] { inputMessage });
		assertEquals("test message", message.getPayload());
	}

	@Test
	public void toMessageWithMessageParameterAndHeader() throws Exception {
		Method method = TestService.class.getMethod("sendMessageAndHeader", Message.class, String.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		Message<?> message = mapper.toMessage(new Object[] { inputMessage, "bar" });
		assertEquals("test message", message.getPayload());
		assertEquals("bar", message.getHeaders().get("foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void toMessageWithMessageParameterAndRequiredHeaderButNullValue() throws Exception {
		Method method = TestService.class.getMethod("sendMessageAndHeader", Message.class, String.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		mapper.toMessage(new Object[] { inputMessage, null });
	}

	@Test
	public void toMessageWithMessageParameterAndOptionalHeaderWithValue() throws Exception {
		Method method = TestService.class.getMethod("sendMessageAndOptionalHeader", Message.class, String.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		Message<?> message = mapper.toMessage(new Object[] { inputMessage, "bar" });
		assertEquals("test message", message.getPayload());
		assertEquals("bar", message.getHeaders().get("foo"));
	}

	@Test
	public void toMessageWithMessageParameterAndOptionalHeaderWithNull() throws Exception {
		Method method = TestService.class.getMethod("sendMessageAndOptionalHeader", Message.class, String.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		Message<?> message = mapper.toMessage(new Object[] { inputMessage, null });
		assertEquals("test message", message.getPayload());
		assertNull(message.getHeaders().get("foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void noArgs() throws Exception {
		Method method = TestService.class.getMethod("noArgs", new Class<?>[] {});
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		mapper.toMessage(new Object[] {});
	}

	@Test(expected = IllegalArgumentException.class)
	public void onlyHeaders() throws Exception {
		Method method = TestService.class.getMethod("onlyHeaders", String.class, String.class);
		MethodParameterMessageMapper mapper = new MethodParameterMessageMapper(method);
		mapper.toMessage(new Object[] { "abc", "def" });
	}


	private static interface TestService {

		void sendPayload(String payload);

		void sendPayloadAndHeader(String payload, @Header("foo") String foo);

		void sendPayloadAndOptionalHeader(String payload, @Header(value="foo", required=false) String foo);

		void sendPayloadAndHeadersMap(String payload, @Headers Map<String, Object> headers);

		void sendMessage(Message<?> message);

		void sendMessageAndHeader(Message<?> message, @Header("foo") String foo);

		void sendMessageAndOptionalHeader(Message<?> message, @Header(value="foo", required=false) String foo);

		// invalid methods

		void noArgs();

		void onlyHeaders(@Header("foo") String foo, @Header("bar") String bar);

	}

}
