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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.core.GenericMessage;

/**
 * @author Mark Fisher
 */
public class PayloadTypeMatchingHandlerMethodResolverTests {

	private PayloadTypeMatchingHandlerMethodResolver resolver;


	@Before
	public void initResolver() {
		Method[] candidates = HandlerMethodUtils.getCandidateHandlerMethods(new TestService());
		resolver = new PayloadTypeMatchingHandlerMethodResolver(candidates);
	}


	@Test
	public void stringPayload() throws Exception {
		Class<?>[] types = new Class<?>[] { String.class };
		Method expected = TestService.class.getMethod("stringPayload", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<String>("foo"));
		assertEquals(expected, resolved);
	}

	@Test
	public void exactMatch() throws Exception {
		Class<?>[] types = new Class<?>[] { TestFooImpl1.class };
		Method expected = TestService.class.getMethod("fooImpl1Payload", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<TestFoo>(new TestFooImpl1()));
		assertEquals(expected, resolved);
	}

	@Test
	public void interfaceMatch() throws Exception {
		Class<?>[] types = new Class<?>[] { TestFoo.class };
		Method expected = TestService.class.getMethod("fooInterfacePayload", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<TestFoo>(new TestFooImpl2()));
		assertEquals(expected, resolved);
	}

	@Test
	public void superclassMatch() throws Exception {
		Class<?>[] types = new Class<?>[] { TestFooImpl1.class };
		Method expected = TestService.class.getMethod("fooImpl1Payload", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<TestFoo>(new TestFooImpl1Subclass()));
		assertEquals(expected, resolved);
	}

	@Test
	public void interfaceOfSuperclassMatch() throws Exception {
		Class<?>[] types = new Class<?>[] { TestFoo.class };
		Method expected = TestService.class.getMethod("fooInterfacePayload", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<TestFoo>(new TestFooImpl2Subclass()));
		assertEquals(expected, resolved);
	}

	@Test
	public void numberSuperclassMatch() throws Exception {
		Class<?>[] types = new Class<?>[] { Number.class };
		Method expected = TestService.class.getMethod("numberPayload", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<Long>(new Long(99)));
		assertEquals(expected, resolved);
	}

	@Test
	public void payloadAndHeaderMethod() throws Exception {
		Class<?>[] types = new Class<?>[] { Integer.class, String.class };
		Method expected = TestService.class.getMethod("integerPayloadAndHeader", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<Integer>(new Integer(123)));
		assertEquals(expected, resolved);
	}

	@Test
	public void fallbackToHeaderOnlyMethod() throws Exception {
		Class<?>[] types = new Class<?>[] { String.class };
		Method expected = TestService.class.getMethod("headerOnlyMethod", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<Date>(new Date()));
		assertEquals(expected, resolved);
	}


	public static class TestService {

		public void stringPayload(String s) {
		}

		public void fooInterfacePayload(TestFoo foo) {
		}

		public void fooImpl1Payload(TestFooImpl1 foo) {
		}

		public void numberPayload(Number n) {
		}

		public void headerOnlyMethod(@Header("testHeader") String s) {
		}

		public void integerPayloadAndHeader(Integer n, @Header("testHeader") String s2) {
		}
	}


	public interface TestFoo {
	}

	public class TestFooImpl1 implements TestFoo {
	}

	public class TestFooImpl2 implements TestFoo {
	}

	public class TestFooImpl1Subclass extends TestFooImpl1 {
	}

	public class TestFooImpl2Subclass extends TestFooImpl2 {
	}

}
