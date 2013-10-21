/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class OutboundGatewayConnectionTests {

	private Destination requestQueue1 = new ActiveMQQueue("request1");

	private Destination replyQueue1 = new ActiveMQQueue("reply1");

	@Test @Ignore // need a more reliable stop/start for AMQ
	public void testContainerWithDestBrokenConnection() throws Exception {
		BeanFactory beanFactory = mock(BeanFactory.class);
		when(beanFactory.containsBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)).thenReturn(true);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();
		when(beanFactory.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class))
			.thenReturn(scheduler);
		final JmsOutboundGateway gateway = new JmsOutboundGateway();
		gateway.setBeanFactory(beanFactory);
		BrokerService broker = new BrokerService();
		broker.addConnector("tcp://localhost:61616?broker.persistent=false");
		broker.start();
		ActiveMQConnectionFactory amqConnectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
		ConnectionFactory connectionFactory = new CachingConnectionFactory(amqConnectionFactory);
		gateway.setConnectionFactory(connectionFactory);
		gateway.setRequestDestination(requestQueue1);
		gateway.setReplyDestination(replyQueue1);
		gateway.setCorrelationKey("JMSCorrelationID");
		gateway.setUseReplyContainer(true);
		gateway.afterPropertiesSet();
		gateway.start();
		final AtomicReference<Object> reply = new AtomicReference<Object>();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				latch1.countDown();
				try {
					reply.set(gateway.handleRequestMessage(new GenericMessage<String>("foo")));
				}
				finally {
					latch2.countDown();
				}
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		JmsTemplate template = new JmsTemplate();
		template.setConnectionFactory(amqConnectionFactory);
		template.setReceiveTimeout(5000);
		javax.jms.Message request = template.receive(requestQueue1);
		assertNotNull(request);
		final javax.jms.Message jmsReply = request;
		template.send(request.getJMSReplyTo(), new MessageCreator() {

			public Message createMessage(Session session) throws JMSException {
				return jmsReply;
			}
		});
		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		assertNotNull(reply.get());

		broker.stop();
		broker = new BrokerService();
		broker.addConnector("tcp://localhost:61616?broker.persistent=false");
		broker.start();

		final CountDownLatch latch3 = new CountDownLatch(1);
		final CountDownLatch latch4 = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				latch3.countDown();
				try {
					reply.set(gateway.handleRequestMessage(new GenericMessage<String>("foo")));
				}
				finally {
					latch4.countDown();
				}
			}
		});
		assertTrue(latch3.await(10, TimeUnit.SECONDS));
		template = new JmsTemplate();
		template.setConnectionFactory(amqConnectionFactory);
		template.setReceiveTimeout(5000);
		request = template.receive(requestQueue1);
		assertNotNull(request);
		final javax.jms.Message jmsReply2 = request;
		template.send(request.getJMSReplyTo(), new MessageCreator() {

			public Message createMessage(Session session) throws JMSException {
				return jmsReply2;
			}
		});
		assertTrue(latch4.await(10, TimeUnit.SECONDS));
		assertNotNull(reply.get());

		broker.stop();
		gateway.stop();
	}

}
