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
 
package org.springframework.integration.router;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;

/**
 * @author Marius Bogoevici
 */
public class CompletionStrategyAdapterTests {

	private SimpleCompletionStrategy simpleCompletionStrategy;

	@Before
	public void setUp() {
		simpleCompletionStrategy = new SimpleCompletionStrategy();
	}

	@Test
	public void testTrueConvertedProperly() {
		CompletionStrategyAdapter adapter = new CompletionStrategyAdapter(new AlwaysTrueCompletionStrategy(),
				"checkCompleteness");
		Assert.assertTrue(adapter.isComplete(new ArrayList<Message<?>>()));
	}

	@Test
	public void testFalseConvertedProperly() {
		CompletionStrategyAdapter adapter = new CompletionStrategyAdapter(new AlwaysFalseCompletionStrategy(),
				"checkCompleteness");
		Assert.assertTrue(!adapter.isComplete(new ArrayList<Message<?>>()));
	}

	@Test
	public void testAdapterWithNonParameterizedMessageListBasedMethod() {
		CompletionStrategy adapter = new CompletionStrategyAdapter(simpleCompletionStrategy,
				"checkCompletenessOnNonParameterizedListOfMessages");
		List<Message<?>> messages = createListOfMessages();
		Assert.assertTrue(adapter.isComplete(messages));
	}

	@Test
	public void testAdapterWithWildcardParametrizedMessageBasedMethod() {
		CompletionStrategy adapter = new CompletionStrategyAdapter(simpleCompletionStrategy,
				"checkCompletenessOnListOfMessagesParametrizedWithWildcard");
		List<Message<?>> messages = createListOfMessages();
		Assert.assertTrue(adapter.isComplete(messages));
	}

	@Test
	public void testAdapterWithTypeParametrizedMessageBasedMethod() {
		CompletionStrategy adapter = new CompletionStrategyAdapter(simpleCompletionStrategy,
				"checkCompletenessOnListOfMessagesParametrizedWithString");
		List<Message<?>> messages = createListOfMessages();
		Assert.assertTrue(adapter.isComplete(messages));
	}

	@Test
	public void testAdapterWithPojoBasedMethod() {
		CompletionStrategy adapter = new CompletionStrategyAdapter(simpleCompletionStrategy,
				"checkCompletenessOnListOfStrings");
		List<Message<?>> messages = createListOfMessages();
		Assert.assertTrue(adapter.isComplete(messages));
	}

	@Test
	public void testAdapterWithPojoBasedMethodReturningObject() {
		CompletionStrategy adapter = new CompletionStrategyAdapter(simpleCompletionStrategy,
				"checkCompletenessOnListOfStrings");
		List<Message<?>> messages = createListOfMessages();
		Assert.assertTrue(adapter.isComplete(messages));
	}

	@Test(expected = ConfigurationException.class)
	public void testAdapterWithWrongMethodName() {
		new CompletionStrategyAdapter(simpleCompletionStrategy, "methodThatDoesNotExist");
	}

	@Test(expected = ConfigurationException.class)
	public void testInvalidParameterTypeUsingMethodName() {
		new CompletionStrategyAdapter(simpleCompletionStrategy, "invalidParameterType");
	}

	@Test(expected = ConfigurationException.class)
	public void testTooManyParametersUsingMethodName() {
		new CompletionStrategyAdapter(simpleCompletionStrategy, "tooManyParameters");
	}

	@Test(expected = ConfigurationException.class)
	public void testNotEnoughParametersUsingMethodName() {
		new CompletionStrategyAdapter(simpleCompletionStrategy, "notEnoughParameters");
	}

	@Test(expected = ConfigurationException.class)
	public void testListSubclassParameterUsingMethodName() {
		new CompletionStrategyAdapter(simpleCompletionStrategy, "ListSubclassParameter");
	}
	

	@Test(expected = ConfigurationException.class)
	public void testWrongReturnType() throws SecurityException, NoSuchMethodError {
		new CompletionStrategyAdapter(simpleCompletionStrategy, "wrongReturnType");
	}

	@Test(expected = ConfigurationException.class)
	public void testInvalidParameterTypeUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new AggregatorAdapter(simpleCompletionStrategy, simpleCompletionStrategy.getClass().getMethod(
				"invalidParameterType", String.class));
	}

	@Test(expected = ConfigurationException.class)
	public void testTooManyParametersUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new CompletionStrategyAdapter(simpleCompletionStrategy, simpleCompletionStrategy.getClass().getMethod(
				"tooManyParameters", List.class, List.class));
	}

	@Test(expected = ConfigurationException.class)
	public void testNotEnoughParametersUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new CompletionStrategyAdapter(simpleCompletionStrategy, simpleCompletionStrategy.getClass().getMethod(
				"notEnoughParameters", new Class[] {}));
	}

	@Test(expected = ConfigurationException.class)
	public void testListSubclassParameterUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new CompletionStrategyAdapter(simpleCompletionStrategy, simpleCompletionStrategy.getClass().getMethod(
				"ListSubclassParameter", new Class[] { LinkedList.class }));
	}
	
	@Test(expected = ConfigurationException.class)
	public void testWrongReturnTypeUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new CompletionStrategyAdapter(simpleCompletionStrategy, simpleCompletionStrategy.getClass().getMethod(
				"wrongReturnType", new Class[] { List.class }));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullObject() {
		new AggregatorAdapter(null, "doesNotMatter");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullMethodName() {
		String methodName = null;
		new AggregatorAdapter(simpleCompletionStrategy, methodName);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullMethodObject() {
		Method method = null;
		new AggregatorAdapter(simpleCompletionStrategy, method);
	}

	private static List<Message<?>> createListOfMessages() {
		List<Message<?>> messages = new ArrayList<Message<?>>();
		messages.add(new GenericMessage<String>("123"));
		messages.add(new GenericMessage<String>("456"));
		messages.add(new GenericMessage<String>("789"));
		return messages;
	}

	private static class AlwaysTrueCompletionStrategy {
		public boolean checkCompleteness(List<Message<?>> messages) {
			return true;
		}
	}

	private static class AlwaysFalseCompletionStrategy {
		public boolean checkCompleteness(List<Message<?>> messages) {
			return false;
		}
	}

	private static class SimpleCompletionStrategy {

		public boolean checkCompletenessOnNonParameterizedListOfMessages(List<Message<?>> messages) {
			Assert.assertTrue(messages.size() > 0);
			return messages.size() > messages.iterator().next().getHeaders().getSequenceSize();
		}

		public boolean checkCompletenessOnListOfMessagesParametrizedWithWildcard(List<Message<?>> messages) {
			Assert.assertTrue(messages.size() > 0);
			return messages.size() > messages.iterator().next().getHeaders().getSequenceSize();
		}

		public boolean checkCompletenessOnListOfMessagesParametrizedWithString(
				List<Message<String>> messages) {
			Assert.assertTrue(messages.size() > 0);
			return messages.size() > messages.iterator().next().getHeaders().getSequenceSize();
		}

		// Example for the case when completeness is checked on the structure of
		// the data
		public boolean checkCompletenessOnListOfStrings(List<String> messages) {
			StringBuffer buffer = new StringBuffer();
			for (String content : messages) {
				buffer.append(content);
			}
			return buffer.length() >= 9;
		}

		public String wrongReturnType(List<Message<?>> message) {
			return "";
		}

		public boolean invalidParameterType(String invalid) {
			return false;
		}

		public boolean tooManyParameters(List<?> c1, List<?> c2) {
			return false;
		}

		public boolean notEnoughParameters() {
			return false;
		}

		public boolean ListSubclassParameter(LinkedList<?> l1) {
			return false;
		}
	}

}
