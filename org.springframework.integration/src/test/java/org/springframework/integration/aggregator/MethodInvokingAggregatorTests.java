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
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.aggregator.Aggregator;
import org.springframework.integration.aggregator.MethodInvokingAggregator;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;

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
	public void testAdapterWithNonParameterizedMessageListBasedMethod() {
		Aggregator aggregator = new MethodInvokingAggregator(simpleAggregator, "doAggregationOnNonParameterizedListOfMessages");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessge = aggregator.aggregate(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals("123456789", returnedMessge.getPayload());
	}

	@Test
	public void testAdapterWithWildcardParametrizedMessageBasedMethod() {
		Aggregator aggregator = new MethodInvokingAggregator(simpleAggregator, "doAggregationOnListOfMessagesParametrizedWithWildcard");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessge = aggregator.aggregate(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals("123456789", returnedMessge.getPayload());
	}

	@Test
	public void testAdapterWithTypeParametrizedMessageBasedMethod() {
		Aggregator aggregator = new MethodInvokingAggregator(simpleAggregator, "doAggregationOnListOfMessagesParametrizedWithString");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessge = aggregator.aggregate(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals("123456789", returnedMessge.getPayload());
	}
	
	@Test
	public void testAdapterWithPojoBasedMethod() {
		Aggregator aggregator = new MethodInvokingAggregator(simpleAggregator, "doAggregationOnListOfStrings");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessge = aggregator.aggregate(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals("123456789", returnedMessge.getPayload());
	}

	@Test
	public void testAdapterWithPojoBasedMethodReturningObject() {
		Aggregator aggregator = new MethodInvokingAggregator(simpleAggregator, "doAggregationOnListOfStringsReturningLong");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessge = aggregator.aggregate(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertEquals(123456789l, returnedMessge.getPayload());
	}
	
	@Test
	public void testAdapterWithVoidReturnType() {
		Aggregator aggregator = new MethodInvokingAggregator(simpleAggregator, "doAggregationWithNoReturn");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessage = aggregator.aggregate(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertNull(returnedMessage);
	}
	
	@Test
	public void testAdapterWithNullReturn() {
		Aggregator aggregator = new MethodInvokingAggregator(simpleAggregator, "doAggregationWithNullReturn");
		List<Message<?>> messages = createListOfMessages();	
		Message<?> returnedMessage = aggregator.aggregate(messages);
		Assert.assertTrue(simpleAggregator.isAggregationPerformed());
		Assert.assertNull(returnedMessage);
	}

	@Test(expected=ConfigurationException.class)
	public void testAdapterWithWrongMethodName() {
		new MethodInvokingAggregator(simpleAggregator, "methodThatDoesNotExist");
	}

	@Test(expected=ConfigurationException.class)
	public void testInvalidParameterTypeUsingMethodName() {
		new MethodInvokingAggregator(simpleAggregator, "invalidParameterType");
	}

	@Test(expected=ConfigurationException.class)
	public void testTooManyParametersUsingMethodName() {
		new MethodInvokingAggregator(simpleAggregator, "tooManyParameters");
	}

	@Test(expected=ConfigurationException.class)
	public void testNotEnoughParametersUsingMethodName() {
		new MethodInvokingAggregator(simpleAggregator, "notEnoughParameters");
	}

	@Test(expected=ConfigurationException.class)
	public void testListSubclassParameterUsingMethodName() {
		new MethodInvokingAggregator(simpleAggregator, "ListSubclassParameter");
	}

	@Test(expected=ConfigurationException.class)
	public void testInvalidParameterTypeUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new MethodInvokingAggregator(simpleAggregator, simpleAggregator.getClass().getMethod(
				"invalidParameterType", String.class));
	}

	@Test(expected=ConfigurationException.class)
	public void testTooManyParametersUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new MethodInvokingAggregator(simpleAggregator, simpleAggregator.getClass().getMethod(
				"tooManyParameters", List.class, List.class));
	}

	@Test(expected=ConfigurationException.class)
	public void testNotEnoughParametersUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new MethodInvokingAggregator(simpleAggregator, simpleAggregator.getClass().getMethod(
				"notEnoughParameters", new Class[] {} ));
	}

	@Test(expected= ConfigurationException.class)
	public void testListSubclassParameterUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new MethodInvokingAggregator(simpleAggregator, simpleAggregator.getClass().getMethod(
				"listSubclassParameter", new Class[] {LinkedList.class} ));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullObject() {
		new MethodInvokingAggregator(null, "doesNotMatter");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullMethodName() {
		String methodName = null;
		new MethodInvokingAggregator(simpleAggregator, methodName);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullMethodObject() {
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
