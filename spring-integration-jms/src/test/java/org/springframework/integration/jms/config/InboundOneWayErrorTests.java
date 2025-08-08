/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class InboundOneWayErrorTests extends ActiveMQMultiContextTests {

	@Autowired
	ApplicationContext context;

	@Autowired
	TestErrorHandler errorHandler;

	@BeforeEach
	void setup() {
		this.errorHandler.lastError = null;
	}

	@Test
	public void noErrorChannel() throws Exception {
		JmsTemplate jmsTemplate = new JmsTemplate(context.getBean("jmsConnectionFactory", ConnectionFactory.class));
		Destination queue = context.getBean("queueA", Destination.class);
		jmsTemplate.send(queue, session -> session.createTextMessage("test-A"));
		assertThat(this.errorHandler.latch.await(3000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(this.errorHandler.lastError).isNotNull();
		assertThat(this.errorHandler.lastError.getCause()).isNotNull();
		assertThat(this.errorHandler.lastError.getCause().getMessage()).isEqualTo("failed to process: test-A");
		PollableChannel testErrorChannel = context.getBean("testErrorChannel", PollableChannel.class);
		assertThat(testErrorChannel.receive(0)).isNull();
	}

	@Test
	public void errorChannel() {
		JmsTemplate jmsTemplate = new JmsTemplate(context.getBean("jmsConnectionFactory", ConnectionFactory.class));
		Destination queue = context.getBean("queueB", Destination.class);
		jmsTemplate.send(queue, session -> session.createTextMessage("test-B"));
		PollableChannel errorChannel = context.getBean("testErrorChannel", PollableChannel.class);
		Message<?> errorMessage = errorChannel.receive(3000);
		assertThat(errorMessage).isNotNull();
		assertThat(errorMessage.getPayload().getClass()).isEqualTo(MessageHandlingException.class);
		MessageHandlingException exception = (MessageHandlingException) errorMessage.getPayload();
		assertThat(exception.getCause()).isNotNull();
		assertThat(exception.getCause().getClass()).isEqualTo(TestException.class);
		assertThat(exception.getCause().getMessage()).isEqualTo("failed to process: test-B");
		assertThat(this.errorHandler.lastError).isNull();
	}

	public static class TestService {

		public void process(Object o) {
			throw new TestException("failed to process: " + o);
		}

	}

	@SuppressWarnings("serial")
	private static class TestException extends RuntimeException {

		TestException(String message) {
			super(message);
		}

	}

	private static class TestErrorHandler implements ErrorHandler {

		private final CountDownLatch latch = new CountDownLatch(1);

		private volatile Throwable lastError;

		@Override
		public void handleError(Throwable t) {
			this.lastError = t;
			this.latch.countDown();
		}

	}

}
