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

package org.springframework.integration.aggregator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.GenericMessage;

import static org.easymock.EasyMock.*;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
@SuppressWarnings("unchecked")
public class MethodInvokingAggregatorTests {

	private TestAggregator mockAggregator = createMock(TestAggregator.class);

	private List<Message<?>> messages = new ArrayList<Message<?>>();

	@Test
	public void adapterWithNonParameterizedMessageListBasedMethod() {
		expect(mockAggregator.doAggregationOnNonParameterizedListOfMessages(isA(List.class))).andStubReturn(
				new GenericMessage<String>(""));
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(mockAggregator,
				"doAggregationOnNonParameterizedListOfMessages");
		replay(mockAggregator);
		aggregator.aggregateMessages(messages);
		verify(mockAggregator);
	}

	@Test
	public void adapterWithWildcardParameterizedMessageBasedMethod() {
		expect(mockAggregator.doAggregationOnListOfMessagesParametrizedWithWildcard(isA(List.class))).andStubReturn(
				new GenericMessage<String>(""));
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(mockAggregator,
				"doAggregationOnListOfMessagesParametrizedWithWildcard");
		replay(mockAggregator);
		aggregator.aggregateMessages(messages);
		verify(mockAggregator);
	}

	@Test
	public void adapterWithTypeParameterizedMessageBasedMethod() {
		expect(mockAggregator.doAggregationOnListOfMessagesParametrizedWithString(isA(List.class))).andStubReturn(
				new GenericMessage<String>(""));
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(mockAggregator,
				"doAggregationOnListOfMessagesParametrizedWithString");
		replay(mockAggregator);
		aggregator.aggregateMessages(messages);
		verify(mockAggregator);
	}

	@Test
	public void adapterWithPojoBasedMethod() {
		expect(mockAggregator.doAggregationOnListOfStrings(isA(List.class))).andStubReturn(
				new GenericMessage<String>(""));
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(mockAggregator,
				"doAggregationOnListOfStrings");
		replay(mockAggregator);
		aggregator.aggregateMessages(messages);
		verify(mockAggregator);
	}

	@Test
	public void adapterWithPojoBasedMethodReturningObject() {
		expect(mockAggregator.doAggregationOnListOfStringsReturningLong(isA(List.class))).andStubReturn(6l);
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(mockAggregator,
				"doAggregationOnListOfStringsReturningLong");
		replay(mockAggregator);
		aggregator.aggregateMessages(messages);
		verify(mockAggregator);
	}

	@Test
	public void adapterWithVoidReturnType() {
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(mockAggregator, "doAggregationWithNoReturn");
		replay(mockAggregator);
		aggregator.aggregateMessages(messages);
		verify(mockAggregator);
	}

	@Test
	public void adapterWithNullReturn() {
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(mockAggregator,
				"doAggregationOnListOfStrings");
		replay(mockAggregator);
		aggregator.aggregateMessages(messages);
		verify(mockAggregator);
	}

	@Test(expected = IllegalArgumentException.class)
	public void adapterWithWrongMethodName() {
		new MethodInvokingAggregator(mockAggregator, "methodThatDoesNotExist");
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidParameterTypeUsingMethodName() {
		new MethodInvokingAggregator(mockAggregator, "invalidParameterType");
	}

	@Test(expected = IllegalArgumentException.class)
	public void tooManyParametersUsingMethodName() {
		new MethodInvokingAggregator(mockAggregator, "tooManyParameters");
	}

	@Test(expected = IllegalArgumentException.class)
	public void notEnoughParametersUsingMethodName() {
		new MethodInvokingAggregator(mockAggregator, "notEnoughParameters");
	}

	@Test(expected = IllegalArgumentException.class)
	public void listSubclassParameterUsingMethodName() {
		new MethodInvokingAggregator(mockAggregator, "ListSubclassParameter");
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidParameterTypeUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new MethodInvokingAggregator(mockAggregator, mockAggregator.getClass().getMethod("invalidParameterType",
				String.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void tooManyParametersUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new MethodInvokingAggregator(mockAggregator, mockAggregator.getClass().getMethod("tooManyParameters",
				List.class, List.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void notEnoughParametersUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new MethodInvokingAggregator(mockAggregator, mockAggregator.getClass().getMethod("notEnoughParameters",
				new Class[] {}));
	}

	@Test(expected = IllegalArgumentException.class)
	public void listSubclassParameterUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new MethodInvokingAggregator(mockAggregator, mockAggregator.getClass().getMethod("listSubclassParameter",
				new Class[] { LinkedList.class }));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullObject() {
		new MethodInvokingAggregator(null, "doesNotMatter");
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullMethodName() {
		String methodName = null;
		new MethodInvokingAggregator(mockAggregator, methodName);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullMethodObject() {
		Method method = null;
		new MethodInvokingAggregator(mockAggregator, method);
	}

	private interface TestAggregator {
		public Message<?> doAggregationOnNonParameterizedListOfMessages(List<Message> messages);

		public Message<?> doAggregationOnListOfMessagesParametrizedWithWildcard(List<Message<?>> messages);

		public Message<?> doAggregationOnListOfMessagesParametrizedWithString(List<Message<String>> messages);

		public Message<?> doAggregationOnListOfStrings(List<String> messages);

		public Long doAggregationOnListOfStringsReturningLong(List<String> messages);

		public void doAggregationWithNoReturn(List<String> message);

		public Message<?> invalidParameterType(String invalid);

		public Message<?> tooManyParameters(List<?> c1, List<?> c2);

		public Message<?> notEnoughParameters();

		public Message<?> listSubclassParameter(LinkedList<?> l1);
	}

}
