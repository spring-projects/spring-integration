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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class PayloadTypeMatchingHandlerMethodResolverWithMessageParameterTests {

	private PayloadTypeMatchingHandlerMethodResolver resolver;


	@Before
	public void initResolver() {
		Method[] candidates = HandlerMethodUtils.getCandidateHandlerMethods(new TestService());
		resolver = new PayloadTypeMatchingHandlerMethodResolver(candidates);
	}


	@Test
	public void stringPayload() throws Exception {
		Class<?>[] types = new Class<?>[] { Message.class };
		Method expected = TestService.class.getMethod("stringPayload", types);
		Method resolved = resolver.resolveHandlerMethod(new StringMessage("foo"));
		assertEquals(expected, resolved);
	}

	@Test
	public void exactMatch() throws Exception {
		Class<?>[] types = new Class<?>[] { Message.class };
		Method expected = TestService.class.getMethod("fooImpl1Payload", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<TestFoo>(new TestFooImpl1()));
		assertEquals(expected, resolved);
	}

	@Test
	public void interfaceMatch() throws Exception {
		Class<?>[] types = new Class<?>[] { Message.class };
		Method expected = TestService.class.getMethod("fooInterfacePayload", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<TestFoo>(new TestFooImpl2()));
		assertEquals(expected, resolved);
	}

	@Test
	public void superclassMatch() throws Exception {
		Class<?>[] types = new Class<?>[] { Message.class };
		Method expected = TestService.class.getMethod("fooImpl1Payload", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<TestFoo>(new TestFooImpl1Subclass()));
		assertEquals(expected, resolved);
	}

	@Test
	public void interfaceOfSuperclassMatch() throws Exception {
		Class<?>[] types = new Class<?>[] { Message.class };
		Method expected = TestService.class.getMethod("fooInterfacePayload", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<TestFoo>(new TestFooImpl2Subclass()));
		assertEquals(expected, resolved);
	}

	@Test
	public void numberSuperclassMatch() throws Exception {
		Class<?>[] types = new Class<?>[] { Message.class };
		Method expected = TestService.class.getMethod("numberPayload", types);
		Method resolved = resolver.resolveHandlerMethod(new GenericMessage<Long>(new Long(99)));
		assertEquals(expected, resolved);
	}

	@Test
	public void payloadAndHeaderMethod() throws Exception {
		Class<?>[] types = new Class<?>[] { Message.class, String.class };
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

	@Test
	public void testStringMessageTypedParameter() throws Exception {
		Object service = new TestServiceWithMessageTypes();
		Method[] candidates = HandlerMethodUtils.getCandidateHandlerMethods(service);
		PayloadTypeMatchingHandlerMethodResolver methodResovler
				= new PayloadTypeMatchingHandlerMethodResolver(candidates);
		Class<?>[] types = new Class<?>[] { StringMessage.class };
		Method expected = TestServiceWithMessageTypes.class.getMethod("stringMessage", types);
		Message<?> message = new StringMessage("foo");
		Method resolved = methodResovler.resolveHandlerMethod(message);
		assertEquals(expected, resolved);
		assertEquals("foo", resolved.invoke(service, message));
	}

	@Test
	public void testMessageParameterizedWithString() throws Exception {
		Object service = new TestServiceWithMessageTypes();
		Method[] candidates = HandlerMethodUtils.getCandidateHandlerMethods(service);
		PayloadTypeMatchingHandlerMethodResolver methodResovler
				= new PayloadTypeMatchingHandlerMethodResolver(candidates);
		Class<?>[] types = new Class<?>[] { Message.class };
		Method expected = TestServiceWithMessageTypes.class.getMethod("stringParameterizedMessage", types);
		Message<?> message = new GenericMessage<String>("foo");
		Method resolved = methodResovler.resolveHandlerMethod(message);
		assertEquals(expected, resolved);
		assertEquals("foo", resolved.invoke(service, message));
	}

	@Test
	public void testUnboundedWildcard() throws Exception {
		Object service = new TestServiceWithMessageTypes();
		Method[] candidates = HandlerMethodUtils.getCandidateHandlerMethods(service);
		PayloadTypeMatchingHandlerMethodResolver methodResovler
				= new PayloadTypeMatchingHandlerMethodResolver(candidates);
		Class<?>[] types = new Class<?>[] { Message.class };
		Method expected = TestServiceWithMessageTypes.class.getMethod("unboundedWildcardMessage", types);
		Date date = new Date();
		Message<?> message = new GenericMessage<Date>(date);
		Method resolved = methodResovler.resolveHandlerMethod(message);
		assertEquals(expected, resolved);
		assertEquals(date, resolved.invoke(service, message));
	}

	@Test
	public void testBoundedWildcard() throws Exception {
		Object service = new TestServiceWithMessageTypes();
		Method[] candidates = HandlerMethodUtils.getCandidateHandlerMethods(service);
		PayloadTypeMatchingHandlerMethodResolver methodResovler
				= new PayloadTypeMatchingHandlerMethodResolver(candidates);
		Class<?>[] types = new Class<?>[] { Message.class };
		Method expected = TestServiceWithMessageTypes.class.getMethod("boundedWildcardMessage", types);
		Message<?> message = MessageBuilder.withPayload(new Integer(123)).build();
		Method resolved = methodResovler.resolveHandlerMethod(message);
		assertEquals(expected, resolved);
		assertEquals(new Integer(123), resolved.invoke(service, message));
	}
	
	@Test
	public void testGenericSuperclass() throws Exception {
		Object service = new ConcreteTestService();
		Method[] candidates = HandlerMethodUtils.getCandidateHandlerMethods(service);
		PayloadTypeMatchingHandlerMethodResolver methodResolver = 
			new PayloadTypeMatchingHandlerMethodResolver(candidates);
		Method expected = ConcreteTestService.class.getMethod("genericMethod", Message.class);
		Message<?> message = MessageBuilder.withPayload("SomeString").build();
		Method resolved = methodResolver.resolveHandlerMethod(message);
		assertEquals(expected, resolved);
		assertEquals(message.getPayload(), resolved.invoke(service, message));
	}
	
	
	public static class GenericTestService<T extends Message<K>, K> {
		public K genericMethod(T message) {
			return message.getPayload();
		}
	}
	
	public static class ConcreteTestService extends GenericTestService<Message<String>, String> {

	}


	public static class TestService {

		public void stringPayload(Message<String> message) {
		}

		public void fooInterfacePayload(Message<TestFoo> message) {
		}

		public void fooImpl1Payload(Message<TestFooImpl1> message) {
		}

		public void numberPayload(Message<Number> message) {
		}

		public void headerOnlyMethod(@Header("testHeader") String s) {
		}

		public void integerPayloadAndHeader(Message<Integer> message, @Header("testHeader") String s) {
		}
	}


	public static class TestServiceWithMessageTypes {

		public String stringMessage(StringMessage message) {
			return message.getPayload();
		}

		public String stringParameterizedMessage(Message<String> message) {
			return message.getPayload();
		}

		public Object unboundedWildcardMessage(Message<?> message) {
			return message.getPayload();
		}

		public Number boundedWildcardMessage(Message<? extends Number> message) {
			return message.getPayload();
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
