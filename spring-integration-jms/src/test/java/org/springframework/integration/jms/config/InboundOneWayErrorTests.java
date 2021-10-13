/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jms.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

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
