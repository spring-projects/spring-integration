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

package org.springframework.integration.util;

import static org.junit.Assert.assertThat;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;
import org.springframework.util.Assert;

/**
 * This is a utitily class for tests with convenience methods needed in this
 * project
 * 
 * Since there cannot be inter project dependencies on test code this utility
 * class is copied around. This practice should be deprecated when there is a
 * test project.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public abstract class TestUtils {

	public static Object getPropertyValue(Object root, String propertyPath) {
		Object value = null;
		DirectFieldAccessor accessor = new DirectFieldAccessor(root);
		String[] tokens = propertyPath.split("\\.");
		for (int i = 0; i < tokens.length; i++) {
			value = accessor.getPropertyValue(tokens[i]);
			if (value != null) {
				accessor = new DirectFieldAccessor(value);
			}
			else if (i == tokens.length - 1) {
				return null;
			}
			else {
				throw new IllegalArgumentException("intermediate property '"
						+ tokens[i] + "' is null");
			}
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getPropertyValue(Object root, String propertyPath,
			Class<T> type) {
		Object value = getPropertyValue(root, propertyPath);
		Assert.isAssignable(type, value.getClass());
		return (T) value;
	}

	@SuppressWarnings("unchecked")
	public static MessageHandler handlerExpecting(
			final Matcher<Message> expectation) {
		return new MessageHandler() {
			public void handleMessage(Message<?> message) {
				assertThat(message, expectation);
			}
		};
	}

	public static Matcher<Message<?>> messageWithPayload(final Object payload) {
		return new BaseMatcher<Message<?>>() {

			public boolean matches(Object candidate) {
				return ((Message<?>) candidate).getPayload().equals(payload);
			}

			public void describeTo(Description description) {
				description.appendText("A message with payload: " + payload);
			}
		};
	}
}
