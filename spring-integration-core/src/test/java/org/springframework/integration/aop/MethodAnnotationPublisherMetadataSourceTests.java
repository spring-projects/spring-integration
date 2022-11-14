/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.aop;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.expression.Expression;
import org.springframework.integration.annotation.Publisher;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Cameron Mayfield
 * @author Chengchen Ji
 *
 * @since 2.0
 */
public class MethodAnnotationPublisherMetadataSourceTests {

	private final MethodAnnotationPublisherMetadataSource source = new MethodAnnotationPublisherMetadataSource();

	@Test
	public void channelNameAndExplicitReturnValuePayload() {
		Method method = getMethod("methodWithChannelAndExplicitReturnAsPayload");
		String channelName = source.getChannelName(method);
		Expression payloadExpression = source.getExpressionForPayload(method);
		assertThat(channelName).isEqualTo("foo");
		assertThat(payloadExpression.getExpressionString()).isEqualTo("#return");
	}

	@Test
	public void channelNameAndEmptyPayloadAnnotation() {
		Method method = getMethod("methodWithChannelAndEmptyPayloadAnnotation");
		String channelName = source.getChannelName(method);
		Expression payloadExpression = source.getExpressionForPayload(method);
		assertThat(channelName).isEqualTo("foo");
		assertThat(payloadExpression.getExpressionString()).isEqualTo("#return");
	}

	@Test
	public void payloadButNoHeaders() {
		Method method = getMethod("methodWithPayloadAnnotation", String.class, int.class);
		String expressionString = source.getExpressionForPayload(method).getExpressionString();
		assertThat(expressionString).isEqualTo("testExpression1");
		Map<String, Expression> headerMap = source.getExpressionsForHeaders(method);
		assertThat(headerMap).isNotNull();
		assertThat(headerMap.size()).isEqualTo(0);
	}

	@Test
	public void payloadAndHeaders() {
		Method method = getMethod("methodWithHeaderAnnotations", String.class, String.class, String.class);
		String expressionString = source.getExpressionForPayload(method).getExpressionString();
		assertThat(expressionString).isEqualTo("testExpression2");
		Map<String, Expression> headerMap = source.getExpressionsForHeaders(method);
		assertThat(headerMap).isNotNull();
		assertThat(headerMap.size()).isEqualTo(2);
		assertThat(headerMap.get("foo").getExpressionString()).isEqualTo("#args[1]");
		assertThat(headerMap.get("bar").getExpressionString()).isEqualTo("#args[2]");
	}

	@Test
	public void expressionsAreConcurrentHashMap() {
		assertThat(ReflectionTestUtils.getField(source, "channels"))
				.as("Expressions should be concurrent to allow startup").isInstanceOf(ConcurrentHashMap.class);
		assertThat(ReflectionTestUtils.getField(source, "payloadExpressions"))
				.as("Expressions should be concurrent to allow startup").isInstanceOf(ConcurrentHashMap.class);
		assertThat(ReflectionTestUtils.getField(source, "headersExpressions"))
				.as("Expressions should be concurrent to allow startup").isInstanceOf(ConcurrentHashMap.class);
	}

	@Test
	public void voidReturnWithValidPayloadExpression() {
		Method method = getMethod("methodWithVoidReturnAndMethodNameAsPayload");
		String channelName = source.getChannelName(method);
		String payloadExpression = source.getExpressionForPayload(method).getExpressionString();
		assertThat(channelName).isEqualTo("foo");
		assertThat(payloadExpression).isEqualTo("#method");
	}

	@Test(expected = IllegalArgumentException.class)
	public void voidReturnWithInvalidPayloadExpression() {
		Method method = getMethod("methodWithVoidReturnAndReturnValueAsPayload");
		source.getExpressionForPayload(method);
	}

	@Test
	public void voidReturnAndParameterPayloadAnnotation() {
		Method method = getMethod("methodWithVoidReturnAndParameterPayloadAnnotation", String.class);
		String payloadExpression = source.getExpressionForPayload(method).getExpressionString();
		assertThat(payloadExpression).isEqualTo("#args[0]");
	}

	@Test(expected = IllegalStateException.class)
	public void voidReturnAndParameterPayloadAnnotationWithExpression() {
		Method method = getMethod("methodWithVoidReturnAndParameterPayloadAnnotationWithExpression", String.class);
		source.getExpressionForPayload(method).getExpressionString();
	}

	@Test(expected = IllegalStateException.class)
	public void voidReturnAndParameterPayloadAnnotationWithValue() {
		Method method = getMethod("methodWithVoidReturnAndParameterPayloadAnnotationWithValue", String.class);
		source.getExpressionForPayload(method).getExpressionString();
	}

	@Test(expected = IllegalArgumentException.class)
	public void voidReturnAndNoPayloadAnnotation() {
		Method method = getMethod("methodWithVoidReturnAndNoPayloadAnnotation", String.class);
		source.getExpressionForPayload(method);
	}

	@Test
	public void explicitAnnotationAttributeOverride() {
		Method method = getMethod("methodWithExplicitAnnotationAttributeOverride");
		String channelName = source.getChannelName(method);
		assertThat(channelName).isEqualTo("foo");
	}

	@Test
	public void explicitAnnotationAttributeOverrideOnDeclaringClass() {
		Method method = getMethodFromTestClass("methodWithAnnotationOnTheDeclaringClass");
		String channelName = source.getChannelName(method);
		assertThat(channelName).isEqualTo("bar");
	}

	private static Method getMethodFromTestClass(String name, Class<?>... params) {
		return getMethodFromClass(TestClass.class, name, params);
	}

	private static Method getMethod(String name, Class<?>... params) {
		return getMethodFromClass(MethodAnnotationPublisherMetadataSourceTests.class, name, params);
	}

	private static Method getMethodFromClass(Class<?> sourceClass, String name, Class<?>... params) {
		try {
			return sourceClass.getMethod(name, params);
		}
		catch (Exception e) {
			throw new RuntimeException("failed to resolve method", e);
		}
	}

	@Publisher
	@Payload("testExpression1")
	public void methodWithPayloadAnnotation(String arg1, int arg2) {
	}

	@Publisher(channel = "foo")
	@Payload("#return")
	public String methodWithChannelAndExplicitReturnAsPayload() {
		return "hello";
	}

	@Publisher(channel = "foo")
	@Payload
	public String methodWithChannelAndEmptyPayloadAnnotation() {
		return "hello";
	}

	@Publisher(channel = "foo")
	@Payload("#method")
	public void methodWithVoidReturnAndMethodNameAsPayload() {
	}

	@Publisher(channel = "foo")
	@Payload("#return")
	public void methodWithVoidReturnAndReturnValueAsPayload() {
	}

	@Publisher
	@Payload("testExpression2")
	public void methodWithHeaderAnnotations(String arg1, @Header("foo") String h1, @Header(name = "bar") String h2) {
	}

	@Publisher
	public void methodWithVoidReturnAndParameterPayloadAnnotation(@Payload String payload) {
	}

	@Publisher
	public void methodWithVoidReturnAndParameterPayloadAnnotationWithExpression(@Payload(expression = "foo") String payload) {
	}

	@Publisher
	public void methodWithVoidReturnAndParameterPayloadAnnotationWithValue(@Payload("foo") String payload) {
	}

	@Publisher
	public void methodWithVoidReturnAndNoPayloadAnnotation(String payload) {
	}

	@Publisher
	@Retention(RetentionPolicy.RUNTIME)
	public @interface CustomPublisher {

		@AliasFor(annotation = Publisher.class, attribute = "channel")
		String custom();

	}

	@CustomPublisher(custom = "foo")
	public void methodWithExplicitAnnotationAttributeOverride() {
	}

	@CustomPublisher(custom = "bar")
	public class TestClass {

		public void methodWithAnnotationOnTheDeclaringClass() {
		}

	}

}
