/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.jms.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.util.ErrorHandler;

/**
 * @author Mark Fisher
 */
public class InboundOneWayErrorTests {

	@Test
	public void noErrorChannel() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("InboundOneWayErrorTests-context.xml", getClass());
		JmsTemplate jmsTemplate = new JmsTemplate(context.getBean("connectionFactory", ConnectionFactory.class));
		Destination queue = context.getBean("queueA", Destination.class);
		jmsTemplate.send(queue, new MessageCreator() {
			public javax.jms.Message createMessage(Session session) throws JMSException {
				return session.createTextMessage("test-A");
			}
		});
		TestErrorHandler errorHandler = context.getBean("testErrorHandler", TestErrorHandler.class);
		errorHandler.latch.await(3000, TimeUnit.MILLISECONDS);
		assertNotNull(errorHandler.lastError);
		assertNotNull(errorHandler.lastError.getCause());
		assertEquals("failed to process: test-A", errorHandler.lastError.getCause().getMessage());
		PollableChannel testErrorChannel = context.getBean("testErrorChannel", PollableChannel.class);
		assertNull(testErrorChannel.receive(0));
		context.close();
	}

	@Test
	public void errorChannel() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("InboundOneWayErrorTests-context.xml", getClass());
		JmsTemplate jmsTemplate = new JmsTemplate(context.getBean("connectionFactory", ConnectionFactory.class));
		Destination queue = context.getBean("queueB", Destination.class);
		jmsTemplate.send(queue, new MessageCreator() {
			public javax.jms.Message createMessage(Session session) throws JMSException {
				return session.createTextMessage("test-B");
			}
		});
		PollableChannel errorChannel = context.getBean("testErrorChannel", PollableChannel.class);
		Message<?> errorMessage = errorChannel.receive(3000);
		assertNotNull(errorMessage);
		assertEquals(MessageHandlingException.class, errorMessage.getPayload().getClass());
		MessageHandlingException exception = (MessageHandlingException) errorMessage.getPayload();
		assertNotNull(exception.getCause());
		assertEquals(TestException.class, exception.getCause().getClass());
		assertEquals("failed to process: test-B", exception.getCause().getMessage());
		TestErrorHandler errorHandler = context.getBean("testErrorHandler", TestErrorHandler.class);
		assertNull(errorHandler.lastError);
		context.close();
	}


	public static class TestService {
		public void process(Object o) {
			throw new TestException("failed to process: " + o);
		}
	}


	@SuppressWarnings("serial")
	private static class TestException extends RuntimeException {
		public TestException(String message) {
			super(message);
		}
	}

	private static class TestErrorHandler implements ErrorHandler {

		private final CountDownLatch latch = new CountDownLatch(1);

		private volatile Throwable lastError;

		public void handleError(Throwable t) {
			this.lastError = t;
			this.latch.countDown();
		}
	}

}
