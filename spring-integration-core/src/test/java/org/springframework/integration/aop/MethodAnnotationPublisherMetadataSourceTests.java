/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.expression.Expression;
import org.springframework.integration.annotation.Publisher;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * @author Mark Fisher
 * @author Artem Bilan
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
		assertEquals("foo", channelName);
		assertEquals("#return", payloadExpression.getExpressionString());
	}

	@Test
	public void channelNameAndEmptyPayloadAnnotation() {
		Method method = getMethod("methodWithChannelAndEmptyPayloadAnnotation");
		String channelName = source.getChannelName(method);
		Expression payloadExpression = source.getExpressionForPayload(method);
		assertEquals("foo", channelName);
		assertEquals("#return", payloadExpression.getExpressionString());
	}

	@Test
	public void payloadButNoHeaders() {
		Method method = getMethod("methodWithPayloadAnnotation", String.class, int.class);
		String expressionString = source.getExpressionForPayload(method).getExpressionString();
		assertEquals("testExpression1", expressionString);
		Map<String, Expression> headerMap = source.getExpressionsForHeaders(method);
		assertNotNull(headerMap);
		assertEquals(0, headerMap.size());
	}

	@Test
	public void payloadAndHeaders() {
		Method method = getMethod("methodWithHeaderAnnotations", String.class, String.class, String.class);
		String expressionString = source.getExpressionForPayload(method).getExpressionString();
		assertEquals("testExpression2", expressionString);
		Map<String, Expression> headerMap = source.getExpressionsForHeaders(method);
		assertNotNull(headerMap);
		assertEquals(2, headerMap.size());
		assertEquals("#args[1]", headerMap.get("foo").getExpressionString());
		assertEquals("#args[2]", headerMap.get("bar").getExpressionString());
	}

	@Test
	public void voidReturnWithValidPayloadExpression() {
		Method method = getMethod("methodWithVoidReturnAndMethodNameAsPayload");
		String channelName = source.getChannelName(method);
		String payloadExpression = source.getExpressionForPayload(method).getExpressionString();
		assertEquals("foo", channelName);
		assertEquals("#method", payloadExpression);
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
		assertEquals("#args[0]", payloadExpression);
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
		assertEquals("foo", channelName);
	}

	@Test
	public void explicitAnnotationAttributeOverrideOnDeclaringClass() {
		Method method = getMethodFromTestClass("methodWithAnnotationOnTheDeclaringClass");
		String channelName = source.getChannelName(method);
		assertEquals("bar", channelName);
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
	public void methodWithHeaderAnnotations(String arg1, @Header("foo") String h1, @Header("bar") String h2) {
	}

	@Publisher
	public void methodWithVoidReturnAndParameterPayloadAnnotation(@Payload String payload) {
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
