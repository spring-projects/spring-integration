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

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.messaging.Message;

/**
 * @author Marius Bogoevici
 * @author Dave Syer
 */
public class MethodInvokingReleaseStrategyTests {

	@Test
	public void testTrueConvertedProperly() {
		MethodInvokingReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new AlwaysTrueReleaseStrategy(),
				"checkCompleteness");
		Assert.assertTrue(adapter.canRelease(createListOfMessages(0)));
	}

	@Test
	public void testFalseConvertedProperly() {
		MethodInvokingReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new AlwaysFalseReleaseStrategy(),
				"checkCompleteness");
		Assert.assertTrue(!adapter.canRelease(createListOfMessages(0)));
	}

	@Test
	public void testAdapterWithNonParameterizedMessageListBasedMethod() {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			public boolean checkCompletenessOnNonParameterizedListOfMessages(List<Message<?>> messages) {
				Assert.assertTrue(messages.size() > 0);
				return messages.size() > new IntegrationMessageHeaderAccessor(messages.iterator().next()).getSequenceSize();
			}
		}
		ReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				"checkCompletenessOnNonParameterizedListOfMessages");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	@Test
	public void testAdapterWithWildcardParametrizedMessageBasedMethod() {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			public boolean checkCompletenessOnListOfMessagesParametrizedWithWildcard(List<Message<?>> messages) {
				Assert.assertTrue(messages.size() > 0);
				return messages.size() > new IntegrationMessageHeaderAccessor(messages.iterator().next()).getSequenceSize();
			}
		}
		ReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				"checkCompletenessOnListOfMessagesParametrizedWithWildcard");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	@Test
	public void testAdapterWithTypeParametrizedMessageBasedMethod() {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			public boolean checkCompletenessOnListOfMessagesParametrizedWithString(List<Message<String>> messages) {
				Assert.assertTrue(messages.size() > 0);
				return messages.size() > new IntegrationMessageHeaderAccessor(messages.iterator().next()).getSequenceSize();
			}
		}
		ReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				"checkCompletenessOnListOfMessagesParametrizedWithString");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	@Test
	public void testAdapterWithPojoBasedMethod() {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			// Example for the case when completeness is checked on the structure of
			// the data
			public boolean checkCompletenessOnListOfStrings(List<String> messages) {
				StringBuffer buffer = new StringBuffer();
				for (String content : messages) {
					buffer.append(content);
				}
				return buffer.length() >= 9;
			}
		}
		ReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				"checkCompletenessOnListOfStrings");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	@Test
	public void testAdapterWithPojoBasedMethodReturningObject() {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			// Example for the case when completeness is checked on the structure of
			// the data
			public boolean checkCompletenessOnListOfStrings(List<String> messages) {
				StringBuffer buffer = new StringBuffer();
				for (String content : messages) {
					buffer.append(content);
				}
				return buffer.length() >= 9;
			}
		}
		ReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				"checkCompletenessOnListOfStrings");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAdapterWithWrongMethodName() {
		class TestReleaseStrategy {
		}
		new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), "methodThatDoesNotExist");
	}

	@Test(expected = IllegalStateException.class)
	public void testInvalidParameterTypeUsingMethodName() {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			public boolean invalidParameterType(Date invalid) {
				return true;
			}
		}
		ReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), "invalidParameterType");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooManyParametersUsingMethodName() {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			public boolean tooManyParameters(List<?> c1, List<?> c2) {
				return false;
			}
		}
		new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), "tooManyParameters");
	}

	@Test
	public void testNotEnoughParametersUsingMethodName() {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			public boolean notEnoughParameters() {
				return false;
			}
		}
		// TODO: this is stupid, but maybe it should be illegal?
		new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), "notEnoughParameters");
	}

	@Test
	public void testNotEnoughParametersUsingMethodObject() throws SecurityException, NoSuchMethodException {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			public boolean notEnoughParameters() {
				return false;
			}
		}
		// TODO: this is stupid, but maybe it should be illegal?
		new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), TestReleaseStrategy.class.getMethod(
				"notEnoughParameters"));
	}

	@Test
	public void testListSubclassParameterUsingMethodName() {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			public boolean listSubclassParameter(LinkedList<?> l1) {
				return true;
			}
		}
		ReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), "listSubclassParameter");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	// TODO: should this be MessageHandlingException?
	@Test(expected = ConversionFailedException.class)
	public void testWrongReturnType() throws SecurityException, NoSuchMethodError {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			public String wrongReturnType(List<Message<?>> messages) {
				return "foo";
			}
		}
		ReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), "wrongReturnType");
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooManyParametersUsingMethodObject() throws SecurityException, NoSuchMethodException {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			public boolean tooManyParameters(List<?> c1, List<?> c2) {
				return false;
			}
		}
		new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), TestReleaseStrategy.class.getMethod(
				"tooManyParameters", List.class, List.class));
	}

	@Test
	public void testListSubclassParameterUsingMethodObject() throws SecurityException, NoSuchMethodException {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			public boolean listSubclassParameter(LinkedList<?> l1) {
				return true;
			}
		}
		ReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				TestReleaseStrategy.class.getMethod("listSubclassParameter", new Class<?>[] { LinkedList.class }));
		MessageGroup messages = createListOfMessages(3);
		Assert.assertTrue(adapter.canRelease(messages));
	}

	// TODO: review exception type here
	@Test(expected = IllegalStateException.class)
	public void testWrongReturnTypeUsingMethodObject() throws SecurityException, NoSuchMethodException {
		class TestReleaseStrategy {
			@SuppressWarnings("unused")
			public int wrongReturnType(List<Message<?>> message) {
				return 0;
			}
		}
		new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), TestReleaseStrategy.class.getMethod(
				"wrongReturnType", new Class<?>[] { List.class }));
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

}
