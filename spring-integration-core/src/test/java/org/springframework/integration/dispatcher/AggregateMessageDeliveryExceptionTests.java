/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.dispatcher;

import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class AggregateMessageDeliveryExceptionTests {

	private Message<?> message = new GenericMessage<>("foo");

	private AggregateMessageDeliveryException exception =
			new AggregateMessageDeliveryException(this.message, "something went wrong", exceptionsList());

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
		assertThat(exception.getMessage()).contains("first problem");
		assertThat(exception.getMessage()).contains("second problem");
		assertThat(exception.getMessage()).contains("third problem");
	}

	@Test
	public void shouldShowFirstOriginalExceptionInCause() {
		assertThat(this.exception.getCause()).isEqualTo(this.firstProblem);
	}

}
