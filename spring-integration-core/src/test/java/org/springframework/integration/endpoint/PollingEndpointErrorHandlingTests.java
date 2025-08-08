/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jonas Partner
 * @author Gary Russell
 */
public class PollingEndpointErrorHandlingTests {

	@SuppressWarnings("rawtypes")
	@Test
	public void checkExceptionPlacedOnErrorChannel() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"pollingEndpointErrorHandlingTests.xml", this.getClass());
		PollableChannel errorChannel = (PollableChannel) context.getBean("errorChannel");
		Message errorMessage = errorChannel.receive(5000);
		assertThat(errorMessage).as("No error message received").isNotNull();
		assertThat(errorMessage.getClass()).as("Message received was not an ErrorMessage")
				.isEqualTo(ErrorMessage.class);
		context.close();
	}

}
