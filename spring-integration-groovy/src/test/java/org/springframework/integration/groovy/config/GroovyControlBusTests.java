/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.groovy.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import groovy.lang.GroovyObject;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


/**
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 * @author Gunnar Hillert
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GroovyControlBusTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Autowired
	private MyGroovyCustomizer groovyCustomizer;

	private static volatile int adviceCalled;

	@Before
	public void beforeTest() {
		adviceCalled = 0;
	}

	@Test
	public void testOperationOfControlBus() { // long is > 3
		this.groovyCustomizer.executed = false;
		Message<?> message = MessageBuilder.withPayload("def result = service.convert('aardvark'); def foo = headers.foo; result+foo").setHeader("foo", "bar").build();
		this.input.send(message);
		assertEquals("catbar", output.receive(0).getPayload());
		assertNull(output.receive(0));
		assertTrue(this.groovyCustomizer.executed);
		assertEquals(1, adviceCalled);
	}

	@Test //INT-2567
	public void testOperationWithCustomScope() {
		Message<?> message = MessageBuilder.withPayload("def result = threadScopedService.convert('testString')").build();
		this.input.send(message);
		assertEquals("cat", output.receive(0).getPayload());
	}

	@Test //INT-2567
	public void testFailOperationWithCustomScope() {
		try {
			Message<?> message = MessageBuilder.withPayload("def result = requestScopedService.convert('testString')").build();
			this.input.send(message);
			fail("Expected BeanCreationException");
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			assertTrue("Expected BeanCreationException, got " + cause.getClass() + ":" + cause.getMessage(), cause instanceof BeanCreationException);
			assertTrue(cause.getMessage().contains("requestScopedService"));
		}
	}

	@Test //INT-2567
	public void testOperationWithRequestCustomScope() {
		RequestContextHolder.setRequestAttributes(new MockRequestAttributes());
		Message<?> message = MessageBuilder.withPayload("def result = requestScopedService.convert('testString')").build();
		this.input.send(message);
		assertEquals("cat", output.receive(0).getPayload());

		RequestContextHolder.resetRequestAttributes();
	}

	@Test //INT-2567
	public void testFailOperationOnNonManagedComponent() {
		try {
			Message<?> message = MessageBuilder.withPayload("def result = nonManagedService.convert('testString')").build();
			this.input.send(message);
			fail("Expected BeanCreationNotAllowedException");
		}
		catch (MessageHandlingException e) {
			Throwable cause = e.getCause();
			assertTrue("Expected BeanCreationNotAllowedException, got " + cause.getClass() + ":" + cause.getMessage(), cause instanceof BeanCreationNotAllowedException);
			assertTrue(cause.getMessage().contains("nonManagedService"));
		}
	}

	@Test //INT-2631
	public void testFailOperationOnAbstractBean() {
		try {
			Message<?> message = MessageBuilder.withPayload("abstractService.convert('testString')").build();
			this.input.send(message);
			fail("Expected BeanIsAbstractException");
		}
		catch (MessageHandlingException e) {
			Throwable cause = e.getCause();
			assertTrue("Expected BeanIsAbstractException, got " + cause.getClass() + ":" + cause.getMessage(), cause instanceof BeanIsAbstractException);
			assertTrue(cause.getMessage().contains("abstractService"));
		}
	}

	@Test //INT-2631
	public void testOperationOnPrototypeBean() {
		Message<?> message = MessageBuilder.withPayload("def result = prototypeService.convert('testString')").build();
		this.input.send(message);
		assertEquals("cat", output.receive(0).getPayload());
	}

	@ManagedResource
	public static class Service {

		@ManagedOperation
		public String convert(String input) {
			return "cat";
		}
	}


	public static class NonManagedService {

		public String convert(String input) {
			return "cat";
		}
	}

	public static class MyGroovyCustomizer implements GroovyObjectCustomizer {
		private volatile boolean executed;

		public void customize(GroovyObject goo) {
			this.executed = true;
		}

	}

	private static class MockRequestAttributes implements RequestAttributes {

		private final Map<String, Object> fakeRequest = new HashMap<String, Object>();

		public Object getAttribute(String name, int scope) {
			return fakeRequest.get(name);
		}

		public void setAttribute(String name, Object value, int scope) {
			fakeRequest.put(name, value);
		}

		public void removeAttribute(String name, int scope) {
		}

		public String[] getAttributeNames(int scope) {
			return null;
		}

		public void registerDestructionCallback(String name, Runnable callback, int scope) {

		}

		public Object resolveReference(String key) {
			return null;
		}

		public String getSessionId() {
			return null;
		}

		public Object getSessionMutex() {
			return null;
		}

	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}

	}
}
