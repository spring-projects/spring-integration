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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;

/**
 * @author Mark Fisher
 */
public class HandlerMethodUtilsTests {

	@Test
	public void testNonProxyIgnoresMethodsFromObjectClass() {
		TestBean testBean = new TestBean();
		Method[] methods = HandlerMethodUtils.getCandidateHandlerMethods(testBean);
		assertEquals(2, methods.length);
	}

	@Test
	public void testDynamicProxyIgnoresMethodsFromObjectClass() {
		ProxyFactory pf = new ProxyFactory(TestInterface.class, new TestInterceptor());
		pf.setTarget(new TestBean());
		Object proxy = pf.getProxy();
		Method[] methods = HandlerMethodUtils.getCandidateHandlerMethods(proxy);
		assertEquals(2, methods.length);
	}

	@Test
	public void testCglibProxyIgnoresMethodsFromObjectClass() {
		ProxyFactory pf = new ProxyFactory(TestInterface.class, new TestInterceptor());
		pf.setTarget(new TestBean());
		pf.setProxyTargetClass(true);
		Object proxy = pf.getProxy();
		Method[] methods = HandlerMethodUtils.getCandidateHandlerMethods(proxy);
		assertEquals(2, methods.length);
	}


	public static class TestBean implements TestInterface {

		public void foo() {
		}

		public void foo(String s) {
		}

		public String toString() {
			return "test";
		}

		public boolean equals(Object o) {
			return (this == o);
		}

		public int hashCode() {
			return 23;
		}

		public Object clone() {
			return new TestBean();
		}

		public void finalize() {
		}
	}


	public static interface TestInterface {
		void foo();
	}


	public static class TestInterceptor implements MethodInterceptor {
		public Object invoke(MethodInvocation invocation) throws Throwable {
			return invocation.proceed();
		}
	}

}
