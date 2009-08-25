/*
 * Copyright 2002-2009 the original author or authors.
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
		assertEquals(2, source.getArgumentNames(method).length);
		assertEquals("arg1", source.getArgumentNames(method)[0]);
		assertEquals("arg2", source.getArgumentNames(method)[1]);
		String[] headerStrings = source.getHeaderExpressions(method);
		assertNotNull(headerStrings);
		assertEquals(1, headerStrings.length);
		assertEquals("", headerStrings[0]);
		assertEquals(ExpressionSource.DEFAULT_ARGUMENT_MAP_NAME, source.getArgumentMapName(method));
		assertEquals(ExpressionSource.DEFAULT_EXCEPTION_NAME, source.getExceptionName(method));
		assertEquals(ExpressionSource.DEFAULT_RETURN_VALUE_NAME, source.getReturnValueName(method));
	}

	@Test
	public void annotationBindings() {
		Method method = getMethod("methodWithExpressionBinding", String.class, int.class);
		String expressionString = source.getPayloadExpression(method);
		assertEquals("testExpression2", expressionString);
		assertEquals(2, source.getArgumentNames(method).length);
		assertEquals("s", source.getArgumentNames(method)[0]);
		assertEquals("i", source.getArgumentNames(method)[1]);
		assertEquals("argz", source.getArgumentMapName(method));
		assertEquals("x", source.getExceptionName(method));
		assertEquals("result", source.getReturnValueName(method));
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


	@Publisher("testExpression1")
	public void methodWithExpressionAnnotationOnly(String arg1, int arg2) {
	}

	@Publisher(value="#return", channel="foo", headers="bar=123")
	public void methodWithChannelAndReturnAsPayload() {
	}

	@Publisher("testExpression2")
	@ExpressionBinding(argNames="s, i", argumentMapName="argz", exceptionName="x", returnValueName="result")
	public void methodWithExpressionBinding(String arg1, int arg2) {
	}

}
