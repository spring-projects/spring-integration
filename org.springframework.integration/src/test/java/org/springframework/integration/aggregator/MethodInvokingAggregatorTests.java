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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class MethodInvokingAggregatorTests {

	private SimpleAggregator simpleAggregator;


	@Before
	public void setUp() {
		simpleAggregator = new SimpleAggregator();
	}


	@Test
	public void adapterWithNonParameterizedMessageListBasedMethod() {
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(
				simpleAggregator, "doAggregationOnNonParameterizedListOfMessages");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessge = aggregator.aggregateMessages(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals("123456789", returnedMessge.getPayload());
	}

	@Test
	public void adapterWithWildcardParameterizedMessageBasedMethod() {
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(
				simpleAggregator, "doAggregationOnListOfMessagesParametrizedWithWildcard");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessge = aggregator.aggregateMessages(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals("123456789", returnedMessge.getPayload());
	}

	@Test
	public void adapterWithTypeParameterizedMessageBasedMethod() {
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(
				simpleAggregator, "doAggregationOnListOfMessagesParametrizedWithString");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessge = aggregator.aggregateMessages(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals("123456789", returnedMessge.getPayload());
	}

	@Test
	public void adapterWithPojoBasedMethod() {
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(
				simpleAggregator, "doAggregationOnListOfStrings");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessge = aggregator.aggregateMessages(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals("123456789", returnedMessge.getPayload());
	}

	@Test
	public void adapterWithPojoBasedMethodReturningObject() {
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(
				simpleAggregator, "doAggregationOnListOfStringsReturningLong");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessge = aggregator.aggregateMessages(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals(123456789l, returnedMessge.getPayload());
	}

	@Test
	public void adapterWithVoidReturnType() {
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(
				simpleAggregator, "doAggregationWithNoReturn");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessage = aggregator.aggregateMessages(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertNull(returnedMessage);
	}
	
	@Test
	public void adapterWithNullReturn() {
		MethodInvokingAggregator aggregator = new MethodInvokingAggregator(
				simpleAggregator, "doAggregationWithNullReturn");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessage = aggregator.aggregateMessages(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertNull(returnedMessage);
	}

	@Test(expected = IllegalArgumentException.class)
	public void adapterWithWrongMethodName() {
		new MethodInvokingAggregator(simpleAggregator, "methodThatDoesNotExist");
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidParameterTypeUsingMethodName() {
		new MethodInvokingAggregator(simpleAggregator, "invalidParameterType");
	}

	@Test(expected = IllegalArgumentException.class)
	public void tooManyParametersUsingMethodName() {
		new MethodInvokingAggregator(simpleAggregator, "tooManyParameters");
	}

	@Test(expected = IllegalArgumentException.class)
	public void notEnoughParametersUsingMethodName() {
		new MethodInvokingAggregator(simpleAggregator, "notEnoughParameters");
	}

	@Test(expected = IllegalArgumentException.class)
	public void listSubclassParameterUsingMethodName() {
		new MethodInvokingAggregator(simpleAggregator, "ListSubclassParameter");
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidParameterTypeUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new MethodInvokingAggregator(simpleAggregator, simpleAggregator.getClass().getMethod(
				"invalidParameterType", String.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void tooManyParametersUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new MethodInvokingAggregator(simpleAggregator, simpleAggregator.getClass().getMethod(
				"tooManyParameters", List.class, List.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void notEnoughParametersUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new MethodInvokingAggregator(simpleAggregator, simpleAggregator.getClass().getMethod(
				"notEnoughParameters", new Class[] {} ));
	}

	@Test(expected = IllegalArgumentException.class)
	public void listSubclassParameterUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new MethodInvokingAggregator(simpleAggregator, simpleAggregator.getClass().getMethod(
				"listSubclassParameter", new Class[] {LinkedList.class} ));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullObject() {
		new MethodInvokingAggregator(null, "doesNotMatter");
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullMethodName() {
		String methodName = null;
		new MethodInvokingAggregator(simpleAggregator, methodName);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullMethodObject() {
		Method method = null;
		new MethodInvokingAggregator(simpleAggregator, method);
	}


	private static List<Message<?>> createListOfMessages() {
		List<Message<?>> messages =  new ArrayList<Message<?>>();
		messages.add(new GenericMessage<String>("123"));
		messages.add(new GenericMessage<String>("456"));  
		messages.add(new GenericMessage<String>("789"));
		return messages;
	}


	private class SimpleAggregator {

		private volatile boolean aggregationPerformed;


		public SimpleAggregator() {
			this.aggregationPerformed = false;
		}

		public boolean isAggregationPerformed() {
			return this.aggregationPerformed;
		}

		@SuppressWarnings("unchecked")
		public Message<?> doAggregationOnNonParameterizedListOfMessages(List<Message> messages) {
			this.aggregationPerformed = true;
			StringBuffer buffer = new StringBuffer();
			for (Message<?> message : messages) {
				buffer.append(message.getPayload());
			}
			return new GenericMessage<String>(buffer.toString());
		}

		public Message<?> doAggregationOnListOfMessagesParametrizedWithWildcard(List<Message<?>> messages) {
			this.aggregationPerformed = true;
			StringBuffer buffer = new StringBuffer();
			for (Message<?> message : messages) {
				buffer.append(message.getPayload());
			}
			return new GenericMessage<String>(buffer.toString());
		}

		public Message<?> doAggregationOnListOfMessagesParametrizedWithString(List<Message<String>> messages) {
			this.aggregationPerformed = true;
			StringBuffer buffer = new StringBuffer();
			for (Message<String> message : messages) {
				buffer.append(message.getPayload());
			}
			return new GenericMessage<String>(buffer.toString());
		}

		public Message<?> doAggregationOnListOfStrings(List<String> messages) {
			this.aggregationPerformed = true;
			StringBuffer buffer = new StringBuffer();
			for (String payload : messages) {
				buffer.append(payload);
			}
			return new GenericMessage<String>(buffer.toString());
		}

		public Long doAggregationOnListOfStringsReturningLong(List<String> messages) {
			this.aggregationPerformed = true;
			StringBuffer buffer = new StringBuffer();
			for (String payload : messages) {
				buffer.append(payload);
			}
			return Long.parseLong(buffer.toString());
		}
		
		public void doAggregationWithNoReturn(List<String> message) {
			this.aggregationPerformed = true;
		}
		
		public Message<?> doAggregationWithNullReturn(List<String> message) {
			this.aggregationPerformed = true;
			return null;
		}

		public Message<?> invalidParameterType(String invalid) {
			return null;
		}

		public Message<?> tooManyParameters(List<?> c1, List<?> c2) {
			return null;
		}

		public Message<?> notEnoughParameters() {
			return null;
		}

		public Message<?> listSubclassParameter(LinkedList<?> l1){
			return null;
		}
	}

}
