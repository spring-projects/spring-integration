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

import org.junit.Test;

import org.springframework.integration.annotation.Header;

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


	private static interface TestService {

		void sendPayload(String payload);

		void sendPayloadAndHeader(String payload, @Header("foo") String foo);

		void sendPayloadAndOptionalHeader(String payload, @Header(value="foo", required=false) String foo);

	}

}
