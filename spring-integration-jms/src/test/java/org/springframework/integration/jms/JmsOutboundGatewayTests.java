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

package org.springframework.integration.jms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.jms.JmsOutboundGateway.ReplyContainerProperties;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.jms.JmsException;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ObjectUtils;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2.4
 */
public class JmsOutboundGatewayTests extends ActiveMQMultiContextTests {

	private final Log logger = LogFactory.getLog(this.getClass());

	@Test
	public void testContainerBeanNameWhenNoGatewayBeanName() {
		JmsOutboundGateway gateway = new JmsOutboundGateway();
		gateway.setConnectionFactory(mock(ConnectionFactory.class));
		gateway.setRequestDestinationName("foo");
		gateway.setUseReplyContainer(true);
		gateway.setReplyContainerProperties(new ReplyContainerProperties());
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();
		assertThat(TestUtils.getPropertyValue(gateway, "replyContainer.beanName"))
				.isEqualTo("JMS_OutboundGateway@" + ObjectUtils.getIdentityHexString(gateway) +
						".replyListener");
	}

	@Test
	public void testReplyContainerRecovery() throws Exception {
		JmsOutboundGateway gateway = new JmsOutboundGateway();
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		gateway.setConnectionFactory(connectionFactory);
		gateway.setRequestDestinationName("foo");
		gateway.setUseReplyContainer(true);
		ReplyContainerProperties replyContainerProperties = new ReplyContainerProperties();
		final List<Throwable> errors = new ArrayList<>();
		ExecutorService exec = Executors.newFixedThreadPool(10);
		ErrorHandlingTaskExecutor errorHandlingTaskExecutor =
				new ErrorHandlingTaskExecutor(exec, t -> {
					errors.add(t);
					throw new RuntimeException(t);
				});
		replyContainerProperties.setTaskExecutor(errorHandlingTaskExecutor);
		replyContainerProperties.setRecoveryInterval(100L);
		gateway.setReplyContainerProperties(replyContainerProperties);
		final Connection connection = mock(Connection.class);
		final AtomicInteger connectionAttempts = new AtomicInteger();
		doAnswer(invocation -> {
			int theCount = connectionAttempts.incrementAndGet();
			if (theCount > 1 && theCount < 4) {
				throw new JmsException("bar") {

				};
			}
			return connection;
		}).when(connectionFactory).createConnection();
		Session session = mock(Session.class);
		when(connection.createSession(false, 1)).thenReturn(session);
		MessageConsumer consumer = mock(MessageConsumer.class);
		when(session.createConsumer(any(Destination.class), isNull())).thenReturn(consumer);
		when(session.createTemporaryQueue()).thenReturn(mock(TemporaryQueue.class));
		final Message message = mock(Message.class);
		final AtomicInteger count = new AtomicInteger();
		doAnswer(invocation -> {
			int theCount = count.incrementAndGet();
			if (theCount > 1 && theCount < 4) {
				throw new JmsException("foo") {

				};
			}
			if (theCount > 4) {
				Thread.sleep(100);
				return null;
			}
			return message;
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
			assertThat(count.get() > 4).isTrue();
			assertThat(errors.size()).isEqualTo(0);
		}
		finally {
			gateway.stop();
			exec.shutdownNow();
		}
	}

	@Test
	public void testConnectionBreakOnReplyMessageIdCorrelation() {
		CachingConnectionFactory connectionFactory1 = new CachingConnectionFactory(ActiveMQMultiContextTests.amqFactory);
		connectionFactory1.setCacheConsumers(false);
		final JmsOutboundGateway gateway = new JmsOutboundGateway();
		gateway.setConnectionFactory(connectionFactory1);
		String requestQ = "requests1";
		gateway.setRequestDestinationName(requestQ);
		String replyQ = "replies1";
		gateway.setReplyDestinationName(replyQ);
		QueueChannel queueChannel = new QueueChannel();
		gateway.setOutputChannel(queueChannel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setReceiveTimeout(60000);
		gateway.afterPropertiesSet();
		gateway.start();
		ExecutorService exec = Executors.newSingleThreadExecutor();
		CachingConnectionFactory connectionFactory2 = new CachingConnectionFactory(ActiveMQMultiContextTests.amqFactory);
		connectionFactory2.setCacheConsumers(false);
		JmsTemplate template = new JmsTemplate(connectionFactory2);
		template.setReceiveTimeout(10000);
		template.afterPropertiesSet();
		try {
			exec.execute(() -> gateway.handleMessage(new GenericMessage<>("foo")));
			final Message request = template.receive(requestQ);
			assertThat(request).isNotNull();
			connectionFactory1.resetConnection();
			MessageCreator reply =
					session -> {
						TextMessage reply1 = session.createTextMessage("bar");
						reply1.setJMSCorrelationID(request.getJMSMessageID());
						return reply1;
					};
			template.send(replyQ, reply);
			org.springframework.messaging.Message<?> received = queueChannel.receive(20000);
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isEqualTo("bar");
		}
		finally {
			gateway.stop();
			connectionFactory1.destroy();
			connectionFactory2.destroy();
			exec.shutdownNow();
		}
	}

	@Test
	public void testConnectionBreakOnReplyCustomCorrelation() {
		CachingConnectionFactory connectionFactory1 = new CachingConnectionFactory(ActiveMQMultiContextTests.amqFactory);
		connectionFactory1.setCacheConsumers(false);
		final JmsOutboundGateway gateway = new JmsOutboundGateway();
		gateway.setConnectionFactory(connectionFactory1);
		String requestQ = "requests2";
		gateway.setRequestDestinationName(requestQ);
		String replyQ = "replies2";
		gateway.setReplyDestinationName(replyQ);
		QueueChannel queueChannel = new QueueChannel();
		gateway.setOutputChannel(queueChannel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.setReceiveTimeout(60000);
		gateway.setCorrelationKey("JMSCorrelationID");
		gateway.afterPropertiesSet();
		gateway.start();
		ExecutorService exec = Executors.newSingleThreadExecutor();
		CachingConnectionFactory connectionFactory2 = new CachingConnectionFactory(ActiveMQMultiContextTests.amqFactory);
		connectionFactory2.setCacheConsumers(false);
		JmsTemplate template = new JmsTemplate(connectionFactory2);
		template.setReceiveTimeout(10000);
		template.afterPropertiesSet();
		try {
			exec.execute(() -> gateway.handleMessage(new GenericMessage<>("foo")));
			Message request = template.receive(requestQ);
			assertThat(request).isNotNull();
			connectionFactory1.resetConnection();
			MessageCreator reply = session -> {
				TextMessage reply1 = session.createTextMessage("bar");
				reply1.setJMSCorrelationID(request.getJMSCorrelationID());
				return reply1;
			};
			logger.debug("Sending reply to: " + replyQ);
			template.send(replyQ, reply);
			logger.debug("Sent reply to: " + replyQ);
			org.springframework.messaging.Message<?> received = queueChannel.receive(20000);
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isEqualTo("bar");
		}
		finally {
			gateway.stop();
			connectionFactory1.destroy();
			connectionFactory2.destroy();
			exec.shutdownNow();
		}
	}

}
