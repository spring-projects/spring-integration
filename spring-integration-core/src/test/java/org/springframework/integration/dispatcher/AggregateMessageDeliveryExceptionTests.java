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

package org.springframework.integration.dispatcher;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;

/**
 * @author Iwein Fuld
 * @author Gunnar Hillert
 */
public class AggregateMessageDeliveryExceptionTests {

	private Message<?> message = new GenericMessage<String>("foo");

	private AggregateMessageDeliveryException exception = new AggregateMessageDeliveryException(message,
			"something went wrong", exceptionsList());

	private MessageDeliveryException firstProblem;


	private List<? extends Exception> exceptionsList() {
		firstProblem = new MessageDeliveryException(message, "first problem");
		return Arrays.asList(firstProblem, new MessageDeliveryException(message, "second problem"),
				new MessageDeliveryException(message, "third problem"));

	}

	@Test
	@Ignore
	// turn this on if you want to read the message and stacktrace in the
	// console
	public void shouldThrow() {
		throw exception;
	}

	@Test
	public void shouldShowOriginalExceptionsInMessage() {
		assertThat(exception.getMessage(), containsString("first problem"));
		assertThat(exception.getMessage(), containsString("second problem"));
		assertThat(exception.getMessage(), containsString("third problem"));
	}

	@Test
	public void shouldShowFirstOriginalExceptionInCause() {
		assertThat((MessageDeliveryException) exception.getCause(), is(firstProblem));
	}

}
