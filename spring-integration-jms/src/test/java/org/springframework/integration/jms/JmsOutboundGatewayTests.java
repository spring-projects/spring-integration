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
package org.springframework.integration.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.jms.JmsOutboundGateway.ReplyContainerProperties;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.jms.JmsException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ObjectUtils;

/**
 * @author Gary Russell
 * @since 2.2.4
 *
 */
public class JmsOutboundGatewayTests {

	final Log logger = LogFactory.getLog(this.getClass());

	@Test
	public void testContainerBeanNameWhenNoGatewayBeanName() {
		JmsOutboundGateway gateway = new JmsOutboundGateway();
		gateway.setConnectionFactory(mock(ConnectionFactory.class));
		gateway.setRequestDestinationName("foo");
		gateway.setUseReplyContainer(true);
		gateway.setReplyContainerProperties(new ReplyContainerProperties());
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();
		assertEquals("JMS_OutboundGateway@" + ObjectUtils.getIdentityHexString(gateway) +
				".replyListener",
			TestUtils.getPropertyValue(gateway, "replyContainer.beanName"));
	}

	@Test
	public void testReplyContainerRecovery() throws Exception {
		JmsOutboundGateway gateway = new JmsOutboundGateway();
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		gateway.setConnectionFactory(connectionFactory);
		gateway.setRequestDestinationName("foo");
		gateway.setUseReplyContainer(true);
		ReplyContainerProperties replyContainerProperties = new ReplyContainerProperties();
		final List<Throwable> errors = new ArrayList<Throwable>();
		ErrorHandlingTaskExecutor errorHandlingTaskExecutor = new ErrorHandlingTaskExecutor(Executors.newFixedThreadPool(10), new ErrorHandler() {

			@Override
			public void handleError(Throwable t) {
				logger.info("Error:", t);
				errors.add(t);
				throw new RuntimeException(t);
			}
		});
		replyContainerProperties.setTaskExecutor(errorHandlingTaskExecutor);
		replyContainerProperties.setRecoveryInterval(100L);
		gateway.setReplyContainerProperties(replyContainerProperties);
		final Connection connection = mock(Connection.class);
		final AtomicInteger connectionAttempts = new AtomicInteger();
		doAnswer(new Answer<Connection>() {

			@SuppressWarnings("serial")
			@Override
			public Connection answer(InvocationOnMock invocation) throws Throwable {
				int theCount = connectionAttempts.incrementAndGet();
				if (theCount > 1 && theCount < 4) {
					throw new JmsException("bar") {};
				}
				return connection;
			}
		}).when(connectionFactory).createConnection();
		Session session = mock(Session.class);
		when(connection.createSession(false, 1)).thenReturn(session);
		MessageConsumer consumer = mock(MessageConsumer.class);
		when(session.createConsumer(any(Destination.class), anyString())).thenReturn(consumer);
		when(session.createTemporaryQueue()).thenReturn(mock(TemporaryQueue.class));
		final Message message = mock(Message.class);
		final AtomicInteger count = new AtomicInteger();
		doAnswer(new Answer<Message>() {

			@SuppressWarnings("serial")
			@Override
			public Message answer(InvocationOnMock invocation) throws Throwable {
				int theCount = count.incrementAndGet();
				if (theCount > 1 && theCount < 4) {
					throw new JmsException("foo") {};
				}
				if (theCount > 4) {
					Thread.sleep(100);
					return null;
				}
				return message;
			}
		}).when(consumer).receive(anyLong());
		when(message.getJMSCorrelationID()).thenReturn("foo");
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		beanFactory.registerSingleton("taskScheduler", taskScheduler);
		gateway.setBeanFactory(beanFactory);
		gateway.afterPropertiesSet();
		gateway.start();
		try {
			int n = 0;
			while (n++ < 100 && count.get() < 5) {
				Thread.sleep(100);
			}
			assertTrue(count.get() > 4);
			assertEquals(0, errors.size());
		}
		finally {
			gateway.stop();
		}
	}
}
