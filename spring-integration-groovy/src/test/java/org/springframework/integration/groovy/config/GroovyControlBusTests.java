/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.groovy.config;

import java.util.HashMap;
import java.util.Map;

import groovy.lang.GroovyObject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 * @author Gunnar Hillert
 *
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
		Message<?> message =
				MessageBuilder.withPayload(
								"def result = service.convert('aardvark'); def foo = headers.foo; result+foo")
						.setHeader("foo", "bar")
						.build();
		this.input.send(message);
		assertThat(output.receive(0).getPayload()).isEqualTo("catbar");
		assertThat(output.receive(0)).isNull();
		assertThat(this.groovyCustomizer.executed).isTrue();
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test //INT-2567
	public void testOperationWithCustomScope() {
		Message<?> message =
				MessageBuilder.withPayload("def result = threadScopedService.convert('testString')")
						.build();
		this.input.send(message);
		assertThat(output.receive(0).getPayload()).isEqualTo("cat");
	}

	@Test //INT-2567
	public void testFailOperationWithCustomScope() {
		try {
			Message<?> message =
					MessageBuilder.withPayload("def result = requestScopedService.convert('testString')")
							.build();
			this.input.send(message);
			fail("Expected BeanCreationException");
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			assertThat(cause instanceof BeanCreationException)
					.as("Expected BeanCreationException, got " + cause.getClass() + ":" + cause
							.getMessage()).isTrue();
			assertThat(cause.getMessage().contains("requestScopedService")).isTrue();
		}
	}

	@Test //INT-2567
	public void testOperationWithRequestCustomScope() {
		RequestContextHolder.setRequestAttributes(new MockRequestAttributes());
		Message<?> message =
				MessageBuilder.withPayload("def result = requestScopedService.convert('testString')")
						.build();
		this.input.send(message);
		assertThat(output.receive(0).getPayload()).isEqualTo("cat");

		RequestContextHolder.resetRequestAttributes();
	}

	@Test //INT-2567
	public void testFailOperationOnNonManagedComponent() {
		try {
			Message<?> message =
					MessageBuilder.withPayload("def result = nonManagedService.convert('testString')")
							.build();
			this.input.send(message);
			fail("Expected BeanCreationNotAllowedException");
		}
		catch (MessageHandlingException e) {
			Throwable cause = e.getCause();
			assertThat(cause instanceof BeanCreationNotAllowedException)
					.as("Expected BeanCreationNotAllowedException, got " + cause.getClass() + ":" + cause.getMessage())
					.isTrue();
			assertThat(cause.getMessage().contains("nonManagedService")).isTrue();
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
			assertThat(cause instanceof BeanIsAbstractException)
					.as("Expected BeanIsAbstractException, got " + cause.getClass() + ":" + cause.getMessage())
					.isTrue();
			assertThat(cause.getMessage().contains("abstractService")).isTrue();
		}
	}

	@Test //INT-2631
	public void testOperationOnPrototypeBean() {
		Message<?> message = MessageBuilder.withPayload("def result = prototypeService.convert('testString')").build();
		this.input.send(message);
		assertThat(output.receive(0).getPayload()).isEqualTo("cat");
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

		@Override
		public void customize(GroovyObject goo) {
			this.executed = true;
		}

	}

	private static class MockRequestAttributes implements RequestAttributes {

		private final Map<String, Object> fakeRequest = new HashMap<>();

		@Override
		public Object getAttribute(String name, int scope) {
			return fakeRequest.get(name);
		}

		@Override
		public void setAttribute(String name, Object value, int scope) {
			fakeRequest.put(name, value);
		}

		@Override
		public void removeAttribute(String name, int scope) {
		}

		@Override
		public String[] getAttributeNames(int scope) {
			return null;
		}

		@Override
		public void registerDestructionCallback(String name, Runnable callback, int scope) {

		}

		@Override
		public Object resolveReference(String key) {
			return null;
		}

		@Override
		public String getSessionId() {
			return null;
		}

		@Override
		public Object getSessionMutex() {
			return null;
		}

	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

}
