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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class GatewayMethodInboundMessageMapperToMessageTests {

	@Test
	public void toMessageWithPayload() throws Exception {
		Method method = TestService.class.getMethod("sendPayload", String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = mapper.toMessage(new Object[] {"test"});
		assertThat(message.getPayload()).isEqualTo("test");
	}

	@Test
	public void toMessageWithTooManyParameters() throws Exception {
		Method method = TestService.class.getMethod("sendPayload", String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> mapper.toMessage(new Object[] {"test", "oops"}));
	}

	@Test
	public void toMessageWithEmptyParameterArray() throws Exception {
		Method method = TestService.class.getMethod("sendPayload", String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> mapper.toMessage(new Object[] {}));
	}

	@Test
	public void toMessageWithPayloadAndHeader() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeader", String.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = mapper.toMessage(new Object[] {"test", "bar"});
		assertThat(message.getPayload()).isEqualTo("test");
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	public void toMessageWithPayloadAndRequiredHeaderButNullValue() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeader", String.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		assertThatExceptionOfType(MessageMappingException.class)
				.isThrownBy(() -> mapper.toMessage(new Object[] {"test", null}));
	}

	@Test
	public void toMessageWithPayloadAndOptionalHeaderWithValueProvided() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndOptionalHeader", String.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = mapper.toMessage(new Object[] {"test", "bar"});
		assertThat(message.getPayload()).isEqualTo("test");
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	public void toMessageWithPayloadAndOptionalHeaderWithNullValue() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndOptionalHeader", String.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = mapper.toMessage(new Object[] {"test", null});
		assertThat(message.getPayload()).isEqualTo("test");
		assertThat(message.getHeaders().get("foo")).isNull();
	}

	@Test
	public void toMessageWithPayloadAndHeadersMap() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeadersMap", String.class, Map.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Map<String, Object> headers = new HashMap<>();
		headers.put("abc", 123);
		headers.put("def", 456);
		Message<?> message = mapper.toMessage(new Object[] {"test", headers});
		assertThat(message.getPayload()).isEqualTo("test");
		assertThat(message.getHeaders().get("abc")).isEqualTo(123);
		assertThat(message.getHeaders().get("def")).isEqualTo(456);
	}

	@Test
	public void toMessageWithPayloadAndNullHeadersMap() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeadersMap", String.class, Map.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = mapper.toMessage(new Object[] {"test", null});
		assertThat(message.getPayload()).isEqualTo("test");
	}

	@Test
	public void toMessageWithPayloadAndHeadersMapWithNonStringKey() throws Exception {
		Method method = TestService.class.getMethod(
				"sendPayloadAndHeadersMap", String.class, Map.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Map<Integer, String> headers = new HashMap<>();
		headers.put(123, "abc");
		assertThatExceptionOfType(MessageMappingException.class)
				.isThrownBy(() -> mapper.toMessage(new Object[] {"test", headers}));
	}

	@Test
	public void toMessageWithMessageParameter() throws Exception {
		Method method = TestService.class.getMethod("sendMessage", Message.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		Message<?> message = mapper.toMessage(new Object[] {inputMessage});
		assertThat(message.getPayload()).isEqualTo("test message");
	}

	@Test
	public void toMessageWithMessageParameterAndHeader() throws Exception {
		Method method = TestService.class.getMethod("sendMessageAndHeader", Message.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		Message<?> message = mapper.toMessage(new Object[] {inputMessage, "bar"});
		assertThat(message.getPayload()).isEqualTo("test message");
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	public void toMessageWithMessageParameterAndRequiredHeaderButNullValue() throws Exception {
		Method method = TestService.class.getMethod("sendMessageAndHeader", Message.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		assertThatExceptionOfType(MessageMappingException.class)
				.isThrownBy(() -> mapper.toMessage(new Object[] {inputMessage, null}));
	}

	@Test
	public void toMessageWithMessageParameterAndOptionalHeaderWithValue() throws Exception {
		Method method = TestService.class.getMethod("sendMessageAndOptionalHeader", Message.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		Message<?> message = mapper.toMessage(new Object[] {inputMessage, "bar"});
		assertThat(message.getPayload()).isEqualTo("test message");
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	public void toMessageWithMessageParameterAndOptionalHeaderWithNull() throws Exception {
		Method method = TestService.class.getMethod("sendMessageAndOptionalHeader", Message.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Message<?> inputMessage = MessageBuilder.withPayload("test message").build();
		Message<?> message = mapper.toMessage(new Object[] {inputMessage, null});
		assertThat(message.getPayload()).isEqualTo("test message");
		assertThat(message.getHeaders().get("foo")).isNull();
	}

	@Test
	public void noArgs() throws Exception {
		Method method = TestService.class.getMethod("noArgs");
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		assertThatExceptionOfType(MessageMappingException.class)
				.isThrownBy(() -> mapper.toMessage(new Object[] {}));
	}

	@Test
	public void onlyHeaders() throws Exception {
		Method method = TestService.class.getMethod("onlyHeaders", String.class, String.class);
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		assertThatExceptionOfType(MessageMappingException.class)
				.isThrownBy(() -> mapper.toMessage(new Object[] {"abc", "def"}));
	}

	@Test
	public void toMessageWithPayloadAndHeaders() throws Exception {
		Method method = TestService.class.getMethod("sendPayload", String.class);
		Map<String, Expression> headers = new HashMap<>();
		headers.put("foo", new LiteralExpression("foo"));
		headers.put("bar", new SpelExpressionParser().parseExpression("6 * 7"));
		headers.put("baz", new LiteralExpression("hello"));
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method, headers);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = mapper.toMessage(new Object[] {"test"});
		assertThat(message.getPayload()).isEqualTo("test");
		assertThat(message.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(message.getHeaders().get("bar")).isEqualTo(42);
	}

	@Test
	public void toMessageWithNonHeaderMapPayloadExpressionA() throws Exception {
		Method method = TestService.class.getMethod("sendNonHeadersMap", Map.class);
		Map<Integer, Object> map = new HashMap<>();
		map.put(1, "One");
		map.put(2, "Two");
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		mapper.setPayloadExpression(new LiteralExpression("hello"));
		Message<?> message = mapper.toMessage(new Object[] {map});
		assertThat(message.getPayload()).isEqualTo("hello");
	}

	@Test
	public void toMessageWithNonHeaderMapPayloadExpressionB() throws Exception {
		Method method = TestService.class.getMethod("sendNonHeadersMap", Map.class);
		Map<Integer, Object> map = new HashMap<>();
		map.put(1, "One");
		map.put(2, "Two");
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		mapper.setPayloadExpression(new FunctionExpression<MethodArgsHolder>((methodArgs) -> methodArgs.getArgs()[0]));
		Message<?> message = mapper.toMessage(new Object[] {map});
		assertThat(message.getPayload()).isEqualTo(map);
	}

	@Test
	public void toMessageWithNonHeaderMapPayloadAnnotation() throws Exception {
		Method method = TestService.class.getMethod("sendNonHeadersMapWithPayloadAnnotation", Map.class);
		Map<Integer, Object> map = new HashMap<>();
		map.put(1, "One");
		map.put(2, "Two");
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = mapper.toMessage(new Object[] {map});
		assertThat(message.getPayload()).isEqualTo(map);
	}

	@Test
	public void toMessageWithTwoMapsOneNonHeaderPayloadExpression() throws Exception {
		Method method = TestService.class.getMethod("sendNonHeadersMapFirstArgument", Map.class, Map.class);
		Map<Integer, Object> mapA = new HashMap<>();
		mapA.put(1, "One");
		mapA.put(2, "Two");
		Map<String, Object> mapB = new HashMap<>();
		mapB.put("1", "ONE");
		mapB.put("2", "TWO");
		GatewayMethodInboundMessageMapper mapper = new GatewayMethodInboundMessageMapper(method);
		mapper.setBeanFactory(mock(BeanFactory.class));
		mapper.setPayloadExpression(new FunctionExpression<MethodArgsHolder>((methodArgs) -> methodArgs.getArgs()[0]));
		Message<?> message = mapper.toMessage(new Object[] {mapA, mapB});
		assertThat(message.getPayload()).isEqualTo(mapA);
		assertThat(message.getHeaders().get("1")).isEqualTo(mapB.get("1"));
		assertThat(message.getHeaders().get("2")).isEqualTo(mapB.get("2"));
	}

	private interface TestService {

		void sendPayload(String payload);

		void sendPayloadAndHeader(String payload, @Header("foo") String foo);

		void sendPayloadAndOptionalHeader(String payload, @Header(value = "foo", required = false) String foo);

		void sendPayloadAndHeadersMap(String payload, @Headers Map<String, Object> headers);

		void sendMessage(Message<?> message);

		void sendMessageAndHeader(Message<?> message, @Header("foo") String foo);

		void sendMessageAndOptionalHeader(Message<?> message, @Header(value = "foo", required = false) String foo);

		// invalid methods

		void noArgs();

		void onlyHeaders(@Header("foo") String foo, @Header("bar") String bar);

		void sendNonHeadersMap(Map<Integer, Object> map);

		@Payload("args[0]")
		void sendNonHeadersMapWithPayloadAnnotation(Map<Integer, Object> map);

		void sendNonHeadersMapFirstArgument(Map<Integer, Object> mapA, Map<String, Object> mapB);

	}

}
