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

package org.springframework.integration.util;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;

import org.springframework.beans.TypeMismatchException;
import org.springframework.integration.util.DefaultMethodInvoker;

/**
 * @author Mark Fisher
 */
public class DefaultMethodInvokerTests {

	@Test
	public void testStringArgumentWithVoidReturnAndNoConversionNecessary() throws Exception {
		TestBean testBean = new TestBean();
		String methodName = "stringArgumentWithVoidReturn";
		Method method = testBean.getClass().getMethod(methodName, String.class);
		DefaultMethodInvoker invoker = new DefaultMethodInvoker(testBean, method);
		invoker.invokeMethod("test");
		assertEquals("test", testBean.lastStringArgument);
	}

	@Test
	public void testStringArgumentWithVoidReturnAndSuccessfulConversion() throws Exception {
		TestBean testBean = new TestBean();
		String methodName = "stringArgumentWithVoidReturn";
		Method method = testBean.getClass().getMethod(methodName, String.class);
		DefaultMethodInvoker invoker = new DefaultMethodInvoker(testBean, method);
		invoker.invokeMethod(new Integer(123));
		assertEquals("123", testBean.lastStringArgument);
	}

	@Test
	public void testIntegerArgumentWithVoidReturnAndSuccessfulConversion() throws Exception {
		TestBean testBean = new TestBean();
		String methodName = "integerArgumentWithVoidReturn";
		Method method = testBean.getClass().getMethod(methodName, Integer.class);
		DefaultMethodInvoker invoker = new DefaultMethodInvoker(testBean, method);
		invoker.invokeMethod("123");
		assertEquals(new Integer(123), testBean.lastIntegerArgument);
	}

	@Test
	public void testIntegerArgumentWithVoidReturnAndFailedConversion() throws Exception {
		TestBean testBean = new TestBean();
		String methodName = "integerArgumentWithVoidReturn";
		Method method = testBean.getClass().getMethod(methodName, Integer.class);
		DefaultMethodInvoker invoker = new DefaultMethodInvoker(testBean, method);
		try {
			invoker.invokeMethod("ABC");
			throw new IllegalStateException("method invocation should have failed with TypeMismatchException");
		}
		catch (Exception e) {
			assertEquals(TypeMismatchException.class, e.getCause().getClass());
		}
	}

	@Test
	public void testTwoArgumentsAndNoConversionRequired() throws Exception {
		TestBean testBean = new TestBean();
		String methodName = "stringAndIntegerArgumentMethod";
		Method method = testBean.getClass().getMethod(methodName, String.class, Integer.class);
		DefaultMethodInvoker invoker = new DefaultMethodInvoker(testBean, method);
		Object result = invoker.invokeMethod("ABC", new Integer(456));
		assertEquals(result, "ABC:456");
	}

	@Test
	public void testTwoArgumentsAndSuccessfulConversion() throws Exception {
		TestBean testBean = new TestBean();
		String methodName = "stringAndIntegerArgumentMethod";
		Method method = testBean.getClass().getMethod(methodName, String.class, Integer.class);
		DefaultMethodInvoker invoker = new DefaultMethodInvoker(testBean, method);
		Object result = invoker.invokeMethod("ABC", "789");
		assertEquals(result, "ABC:789");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testTwoArgumentMethodWithOnlyOneArgumentProvided() throws Exception {
		TestBean testBean = new TestBean();
		String methodName = "stringAndIntegerArgumentMethod";
		Method method = testBean.getClass().getMethod(methodName, String.class, Integer.class);
		DefaultMethodInvoker invoker = new DefaultMethodInvoker(testBean, method);
		invoker.invokeMethod("ABC");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testTwoArgumentMethodWithOnlyThreeArgumentsProvided() throws Exception {
		TestBean testBean = new TestBean();
		String methodName = "stringAndIntegerArgumentMethod";
		Method method = testBean.getClass().getMethod(methodName, String.class, Integer.class);
		DefaultMethodInvoker invoker = new DefaultMethodInvoker(testBean, method);
		invoker.invokeMethod("ABC", new Integer(123), new Integer(456));
	}


	private static class TestBean {

		String lastStringArgument;

		Integer lastIntegerArgument;


		public void stringArgumentWithVoidReturn(String s) {
			this.lastStringArgument = s; 
		}

		public void integerArgumentWithVoidReturn(Integer i) {
			this.lastIntegerArgument = i;
		}

		public String stringAndIntegerArgumentMethod(String s, Integer i) {
			return s + ":" + i;
		}

	}

}
