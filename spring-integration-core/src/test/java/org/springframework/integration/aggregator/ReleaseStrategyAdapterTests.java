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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.core.GenericMessage;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;

/**
 * @author Marius Bogoevici
 */
public class ReleaseStrategyAdapterTests {

	private SimpleReleaseStrategy simpleReleaseStrategy;

	@Before
	public void setUp() {
		simpleReleaseStrategy = new SimpleReleaseStrategy();
	}

	@Test
	public void testTrueConvertedProperly() {
		ReleaseStrategyAdapter adapter = new ReleaseStrategyAdapter(new AlwaysTrueReleaseStrategy(),
				"checkCompleteness");
		Assert.assertTrue(adapter.canRelease(createListOfMessages(0)));
	}

	@Test
	public void testFalseConvertedProperly() {
		ReleaseStrategyAdapter adapter = new ReleaseStrategyAdapter(new AlwaysFalseReleaseStrategy(),
				"checkCompleteness");
		Assert.assertTrue(!adapter.canRelease(createListOfMessages(0)));
	}

	@Test
	public void testAdapterWithNonParameterizedMessageListBasedMethod() {
		ReleaseStrategy adapter = new ReleaseStrategyAdapter(simpleReleaseStrategy,
				"checkCompletenessOnNonParameterizedListOfMessages");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	@Test
	public void testAdapterWithWildcardParametrizedMessageBasedMethod() {
		ReleaseStrategy adapter = new ReleaseStrategyAdapter(simpleReleaseStrategy,
				"checkCompletenessOnListOfMessagesParametrizedWithWildcard");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	@Test
	public void testAdapterWithTypeParametrizedMessageBasedMethod() {
		ReleaseStrategy adapter = new ReleaseStrategyAdapter(simpleReleaseStrategy,
				"checkCompletenessOnListOfMessagesParametrizedWithString");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	@Test
	public void testAdapterWithPojoBasedMethod() {
		ReleaseStrategy adapter = new ReleaseStrategyAdapter(simpleReleaseStrategy, "checkCompletenessOnListOfStrings");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	@Test
	public void testAdapterWithPojoBasedMethodReturningObject() {
		ReleaseStrategy adapter = new ReleaseStrategyAdapter(simpleReleaseStrategy, "checkCompletenessOnListOfStrings");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAdapterWithWrongMethodName() {
		new ReleaseStrategyAdapter(simpleReleaseStrategy, "methodThatDoesNotExist");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidParameterTypeUsingMethodName() {
		new ReleaseStrategyAdapter(simpleReleaseStrategy, "invalidParameterType");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooManyParametersUsingMethodName() {
		new ReleaseStrategyAdapter(simpleReleaseStrategy, "tooManyParameters");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNotEnoughParametersUsingMethodName() {
		new ReleaseStrategyAdapter(simpleReleaseStrategy, "notEnoughParameters");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testListSubclassParameterUsingMethodName() {
		new ReleaseStrategyAdapter(simpleReleaseStrategy, "ListSubclassParameter");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWrongReturnType() throws SecurityException, NoSuchMethodError {
		new ReleaseStrategyAdapter(simpleReleaseStrategy, "wrongReturnType");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooManyParametersUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new ReleaseStrategyAdapter(simpleReleaseStrategy, simpleReleaseStrategy.getClass().getMethod(
				"tooManyParameters", List.class, List.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNotEnoughParametersUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new ReleaseStrategyAdapter(simpleReleaseStrategy, simpleReleaseStrategy.getClass().getMethod(
				"notEnoughParameters", new Class[] {}));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testListSubclassParameterUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new ReleaseStrategyAdapter(simpleReleaseStrategy, simpleReleaseStrategy.getClass().getMethod(
				"ListSubclassParameter", new Class[] { LinkedList.class }));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWrongReturnTypeUsingMethodObject() throws SecurityException, NoSuchMethodException {
		new ReleaseStrategyAdapter(simpleReleaseStrategy, simpleReleaseStrategy.getClass().getMethod("wrongReturnType",
				new Class[] { List.class }));
	}

	private static MessageGroup createListOfMessages(int size) {
		List<Message<?>> messages = new ArrayList<Message<?>>();
		if (size > 0) {
			messages.add(new GenericMessage<String>("123"));
		}
		if (size > 1) {
			messages.add(new GenericMessage<String>("456"));
		}
		if (size > 2) {
			messages.add(new GenericMessage<String>("789"));
		}
		return new SimpleMessageGroup(messages, "ABC");
	}

	@SuppressWarnings("unused")
	private static class AlwaysTrueReleaseStrategy {
		public boolean checkCompleteness(List<Message<?>> messages) {
			return true;
		}
	}

	@SuppressWarnings("unused")
	private static class AlwaysFalseReleaseStrategy {
		public boolean checkCompleteness(List<Message<?>> messages) {
			return false;
		}
	}

	@SuppressWarnings("unused")
	private static class SimpleReleaseStrategy {

		public boolean checkCompletenessOnNonParameterizedListOfMessages(List<Message<?>> messages) {
			Assert.assertTrue(messages.size() > 0);
			return messages.size() > messages.iterator().next().getHeaders().getSequenceSize();
		}

		public boolean checkCompletenessOnListOfMessagesParametrizedWithWildcard(List<Message<?>> messages) {
			Assert.assertTrue(messages.size() > 0);
			return messages.size() > messages.iterator().next().getHeaders().getSequenceSize();
		}

		public boolean checkCompletenessOnListOfMessagesParametrizedWithString(List<Message<String>> messages) {
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
