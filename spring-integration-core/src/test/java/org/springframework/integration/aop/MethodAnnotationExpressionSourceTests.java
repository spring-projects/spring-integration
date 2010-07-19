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

package org.springframework.integration.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Test;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class MethodAnnotationExpressionSourceTests {

	private final MethodAnnotationExpressionSource source = new MethodAnnotationExpressionSource();

	@Test
	public void defaultBindings() {
		Method method = getMethod("methodWithExpressionAnnotationOnly", String.class, int.class);
		String expressionString = source.getPayloadExpression(method);
		assertEquals("testExpression1", expressionString);
		assertEquals(2, source.getArgumentVariableNames(method).length);
		assertEquals("arg1", source.getArgumentVariableNames(method)[0]);
		assertEquals("arg2", source.getArgumentVariableNames(method)[1]);
		Map<String, String> headerMap = source.getHeaderExpressions(method);
		assertNotNull(headerMap);
		assertEquals(0, headerMap.size());
		assertEquals(ExpressionSource.DEFAULT_ARGUMENT_MAP_VARIABLE_NAME, source.getArgumentMapVariableName(method));
		assertEquals(ExpressionSource.DEFAULT_EXCEPTION_VARIABLE_NAME, source.getExceptionVariableName(method));
		assertEquals(ExpressionSource.DEFAULT_RETURN_VALUE_VARIABLE_NAME, source.getReturnValueVariableName(method));
	}

	@Test
	public void annotationBindings() {
		Method method = getMethod("methodWithExpressionBinding", String.class, int.class);
		String expressionString = source.getPayloadExpression(method);
		assertEquals("testExpression2", expressionString);
		assertEquals(2, source.getArgumentVariableNames(method).length);
		assertEquals("s", source.getArgumentVariableNames(method)[0]);
		assertEquals("i", source.getArgumentVariableNames(method)[1]);
		assertEquals("argz", source.getArgumentMapVariableName(method));
		assertEquals("x", source.getExceptionVariableName(method));
		assertEquals("result", source.getReturnValueVariableName(method));
	}

	@Test
	public void channelName() {
		Method method = getMethod("methodWithChannelAndReturnAsPayload");
		String channelName = source.getChannelName(method);
		assertEquals("foo", channelName);
	}


	private static Method getMethod(String name, Class<?> ... params) {
		try {
			return MethodAnnotationExpressionSourceTests.class.getMethod(name, params);
		}
		catch (Exception e) {
			throw new RuntimeException("failed to resolve method", e);
		}
	}


	@Publisher(payload="testExpression1")
	public void methodWithExpressionAnnotationOnly(String arg1, int arg2) {
	}

	@Publisher(payload="#return", channel="foo")
	public void methodWithChannelAndReturnAsPayload() {
	}

	@Publisher(payload="testExpression2")
	@ExpressionBinding(argumentVariableNames="s, i", argumentMapVariableName="argz",
			exceptionVariableName="x", returnValueVariableName="result")
	public void methodWithExpressionBinding(String arg1, int arg2) {
	}

}
