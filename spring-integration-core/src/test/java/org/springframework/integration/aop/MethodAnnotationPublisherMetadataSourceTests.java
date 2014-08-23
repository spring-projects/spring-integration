/*
 * Copyright 2002-2014 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Test;

import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.integration.annotation.Publisher;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class MethodAnnotationPublisherMetadataSourceTests {

	private final MethodAnnotationPublisherMetadataSource source = new MethodAnnotationPublisherMetadataSource();


	@Test
	public void channelNameAndExplicitReturnValuePayload() {
		Method method = getMethod("methodWithChannelAndExplicitReturnAsPayload");
		String channelName = source.getChannelName(method);
		String payloadExpression = source.getPayloadExpression(method);
		assertEquals("foo", channelName);
		assertEquals("#return", payloadExpression);
	}

	@Test
	public void channelNameAndEmptyPayloadAnnotation() {
		Method method = getMethod("methodWithChannelAndEmptyPayloadAnnotation");
		String channelName = source.getChannelName(method);
		String payloadExpression = source.getPayloadExpression(method);
		assertEquals("foo", channelName);
		assertEquals("#return", payloadExpression);
	}

	@Test
	public void payloadButNoHeaders() {
		Method method = getMethod("methodWithPayloadAnnotation", String.class, int.class);
		String expressionString = source.getPayloadExpression(method);
		assertEquals("testExpression1", expressionString);
		Map<String, String> headerMap = source.getHeaderExpressions(method);
		assertNotNull(headerMap);
		assertEquals(0, headerMap.size());
	}

	@Test
	public void payloadAndHeaders() {
		Method method = getMethod("methodWithHeaderAnnotations", String.class, String.class, String.class);
		String expressionString = source.getPayloadExpression(method);
		assertEquals("testExpression2", expressionString);
		Map<String, String> headerMap = source.getHeaderExpressions(method);
		assertNotNull(headerMap);
		assertEquals(2, headerMap.size());
		assertEquals("#args[1]", headerMap.get("foo"));
		assertEquals("#args[2]", headerMap.get("bar"));
	}

	@Test
	public void voidReturnWithValidPayloadExpression() {
		Method method = getMethod("methodWithVoidReturnAndMethodNameAsPayload");
		String channelName = source.getChannelName(method);
		String payloadExpression = source.getPayloadExpression(method);
		assertEquals("foo", channelName);
		assertEquals("#method", payloadExpression);
	}

	@Test(expected = IllegalArgumentException.class)
	public void voidReturnWithInvalidPayloadExpression() {
		Method method = getMethod("methodWithVoidReturnAndReturnValueAsPayload");
		source.getPayloadExpression(method);
	}

	@Test
	public void voidReturnAndParameterPayloadAnnotation() {
		Method method = getMethod("methodWithVoidReturnAndParameterPayloadAnnotation", String.class);
		String payloadExpression = source.getPayloadExpression(method);
		assertEquals("#args[0]", payloadExpression);
	}

	@Test(expected = IllegalArgumentException.class)
	public void voidReturnAndNoPayloadAnnotation() {
		Method method = getMethod("methodWithVoidReturnAndNoPayloadAnnotation", String.class);
		source.getPayloadExpression(method);
	}

	private static Method getMethod(String name, Class<?> ... params) {
		try {
			return MethodAnnotationPublisherMetadataSourceTests.class.getMethod(name, params);
		}
		catch (Exception e) {
			throw new RuntimeException("failed to resolve method", e);
		}
	}


	@Publisher
	@Payload("testExpression1")
	public void methodWithPayloadAnnotation(String arg1, int arg2) {
	}

	@Publisher(channel="foo")
	@Payload("#return")
	public String methodWithChannelAndExplicitReturnAsPayload() {
		return "hello";
	}

	@Publisher(channel="foo")
	@Payload
	public String methodWithChannelAndEmptyPayloadAnnotation() {
		return "hello";
	}


	@Publisher(channel="foo")
	@Payload("#method")
	public void methodWithVoidReturnAndMethodNameAsPayload() {
	}

	@Publisher(channel="foo")
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

}
