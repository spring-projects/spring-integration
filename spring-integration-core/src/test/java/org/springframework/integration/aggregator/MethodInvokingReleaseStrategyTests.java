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

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author Marius Bogoevici
 * @author Dave Syer
 * @author Artem Bilan
 */
public class MethodInvokingReleaseStrategyTests {

	@Test
	public void testTrueConvertedProperly() {
		MethodInvokingReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new AlwaysTrueReleaseStrategy(),
				"checkCompleteness");
		adapter.setBeanFactory(mock(BeanFactory.class));
		assertThat(adapter.canRelease(createListOfMessages(0))).isTrue();
	}

	@Test
	public void testFalseConvertedProperly() {
		MethodInvokingReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new AlwaysFalseReleaseStrategy(),
				"checkCompleteness");
		adapter.setBeanFactory(mock(BeanFactory.class));
		assertThat(adapter.canRelease(createListOfMessages(0))).isFalse();
	}

	@Test
	public void testAdapterWithNonParameterizedMessageListBasedMethod() {
		class TestReleaseStrategy {

			@SuppressWarnings("unused")
			public boolean checkCompletenessOnNonParameterizedListOfMessages(List<Message<?>> messages) {
				assertThat(!messages.isEmpty()).isTrue();
				return messages.size() >
						new IntegrationMessageHeaderAccessor(messages.iterator().next()).getSequenceSize();
			}

		}

		MethodInvokingReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				"checkCompletenessOnNonParameterizedListOfMessages");
		adapter.setBeanFactory(mock(BeanFactory.class));
		MessageGroup messages = createListOfMessages(3);
		assertThat(adapter.canRelease(messages)).isTrue();
	}

	@Test
	public void testAdapterWithWildcardParametrizedMessageBasedMethod() {
		class TestReleaseStrategy {

			@SuppressWarnings("unused")
			public boolean checkCompletenessOnListOfMessagesParametrizedWithWildcard(List<Message<?>> messages) {
				assertThat(!messages.isEmpty()).isTrue();
				return messages.size() >
						new IntegrationMessageHeaderAccessor(messages.iterator().next()).getSequenceSize();
			}

		}

		MethodInvokingReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				"checkCompletenessOnListOfMessagesParametrizedWithWildcard");
		adapter.setBeanFactory(mock(BeanFactory.class));
		MessageGroup messages = createListOfMessages(3);
		assertThat(adapter.canRelease(messages)).isTrue();
	}

	@Test
	public void testAdapterWithTypeParametrizedMessageBasedMethod() {
		class TestReleaseStrategy {

			@SuppressWarnings("unused")
			public boolean checkCompletenessOnListOfMessagesParametrizedWithString(List<Message<String>> messages) {
				assertThat(!messages.isEmpty()).isTrue();
				return messages.size() > new IntegrationMessageHeaderAccessor(messages.iterator().next())
						.getSequenceSize();
			}

		}

		MethodInvokingReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				"checkCompletenessOnListOfMessagesParametrizedWithString");
		adapter.setBeanFactory(mock(BeanFactory.class));
		MessageGroup messages = createListOfMessages(3);
		assertThat(adapter.canRelease(messages)).isTrue();
	}

	@Test
	public void testAdapterWithPojoBasedMethod() {
		class TestReleaseStrategy {

			@SuppressWarnings("unused")
			// Example for the case when completeness is checked on the structure of
			// the data
			public boolean checkCompletenessOnListOfStrings(List<String> messages) {
				StringBuilder buffer = new StringBuilder();
				for (String content : messages) {
					buffer.append(content);
				}
				return buffer.length() >= 9;
			}

		}

		MethodInvokingReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				"checkCompletenessOnListOfStrings");
		adapter.setBeanFactory(mock(BeanFactory.class));
		MessageGroup messages = createListOfMessages(3);
		assertThat(adapter.canRelease(messages)).isTrue();
	}

	@Test
	public void testAdapterWithPojoBasedMethodReturningObject() {
		class TestReleaseStrategy {

			@SuppressWarnings("unused")
			// Example for the case when completeness is checked on the structure of
			// the data
			public boolean checkCompletenessOnListOfStrings(List<String> messages) {
				StringBuilder buffer = new StringBuilder();
				for (String content : messages) {
					buffer.append(content);
				}
				return buffer.length() >= 9;
			}

		}

		MethodInvokingReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				"checkCompletenessOnListOfStrings");
		adapter.setBeanFactory(mock(BeanFactory.class));
		MessageGroup messages = createListOfMessages(3);
		assertThat(adapter.canRelease(messages)).isTrue();
	}

	@Test
	public void testAdapterWithWrongMethodName() {
		class TestReleaseStrategy {

		}

		assertThatIllegalStateException()
				.isThrownBy(() ->
						new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), "methodThatDoesNotExist"));
	}

	@Test
	public void testInvalidParameterTypeUsingMethodName() {
		class TestReleaseStrategy {

			@SuppressWarnings("unused")
			public boolean invalidParameterType(Date invalid) {
				return true;
			}

		}

		MethodInvokingReleaseStrategy adapter =
				new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), "invalidParameterType");
		adapter.setBeanFactory(mock(BeanFactory.class));
		MessageGroup messages = createListOfMessages(3);
		assertThatIllegalStateException().isThrownBy(() -> adapter.canRelease(messages));
	}

	@Test
	public void testTooManyParametersUsingMethodName() {
		class TestReleaseStrategy {

			@SuppressWarnings("unused")
			public boolean tooManyParameters(List<?> c1, List<?> c2) {
				return false;
			}

		}

		assertThatIllegalStateException()
				.isThrownBy(() -> new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), "tooManyParameters"));
	}

	@Test
	public void testNotEnoughParametersUsingMethodName() {
		class TestReleaseStrategy {

			@SuppressWarnings("unused")
			public boolean notEnoughParameters() {
				return false;
			}

		}

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

		MethodInvokingReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				"listSubclassParameter");
		adapter.setBeanFactory(mock(BeanFactory.class));
		MessageGroup messages = createListOfMessages(3);
		assertThat(adapter.canRelease(messages)).isTrue();
	}

	@Test
	public void testWrongReturnType() throws SecurityException, NoSuchMethodError {
		class TestReleaseStrategy {

			@SuppressWarnings("unused")
			public String wrongReturnType(List<Message<?>> messages) {
				return "foo";
			}

		}

		MethodInvokingReleaseStrategy adapter =
				new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), "wrongReturnType");
		adapter.setBeanFactory(mock(BeanFactory.class));
		MessageGroup messages = createListOfMessages(3);
		assertThatExceptionOfType(ConversionFailedException.class)
				.isThrownBy(() -> adapter.canRelease(messages));
	}

	@Test
	public void testTooManyParametersUsingMethodObject() throws SecurityException {
		class TestReleaseStrategy {

			@SuppressWarnings("unused")
			public boolean tooManyParameters(List<?> c1, List<?> c2) {
				return false;
			}

		}

		assertThatIllegalArgumentException().isThrownBy(() ->
				new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
						TestReleaseStrategy.class.getMethod("tooManyParameters", List.class, List.class)));
	}

	@Test
	public void testListSubclassParameterUsingMethodObject() throws SecurityException, NoSuchMethodException {
		class TestReleaseStrategy {

			@SuppressWarnings("unused")
			public boolean listSubclassParameter(LinkedList<?> l1) {
				return true;
			}

		}

		MethodInvokingReleaseStrategy adapter = new MethodInvokingReleaseStrategy(new TestReleaseStrategy(),
				TestReleaseStrategy.class.getMethod("listSubclassParameter", LinkedList.class));
		adapter.setBeanFactory(mock(BeanFactory.class));
		MessageGroup messages = createListOfMessages(3);
		assertThat(adapter.canRelease(messages)).isTrue();
	}

	@Test
	public void testWrongReturnTypeUsingMethodObject() throws SecurityException, NoSuchMethodException {
		class TestReleaseStrategy {

			@SuppressWarnings("unused")
			public int wrongReturnType(List<Message<?>> message) {
				return 0;
			}

		}

		MethodInvokingReleaseStrategy wrongReturnType =
				new MethodInvokingReleaseStrategy(new TestReleaseStrategy(), TestReleaseStrategy.class.getMethod(
						"wrongReturnType", List.class));
		wrongReturnType.setBeanFactory(mock(BeanFactory.class));

		assertThatIllegalStateException()
				.isThrownBy(() -> wrongReturnType.canRelease(mock(MessageGroup.class)));
	}

	private static MessageGroup createListOfMessages(int size) {
		List<Message<?>> messages = new ArrayList<>();
		if (size > 0) {
			messages.add(new GenericMessage<>("123"));
		}
		if (size > 1) {
			messages.add(new GenericMessage<>("456"));
		}
		if (size > 2) {
			messages.add(new GenericMessage<>("789"));
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
