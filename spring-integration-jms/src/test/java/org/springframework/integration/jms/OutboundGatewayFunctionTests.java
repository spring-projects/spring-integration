/*
 * Copyright 2002-2018 the original author or authors.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.test.rule.Log4j2LevelAdjuster;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.JmsException;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class OutboundGatewayFunctionTests extends ActiveMQMultiContextTests {

	private static Destination requestQueue1 = new ActiveMQQueue("request1");

	private static Destination replyQueue1 = new ActiveMQQueue("reply1");

	private static Destination requestQueue2 = new ActiveMQQueue("request2");

	private static Destination replyQueue2 = new ActiveMQQueue("reply2");

	private static Destination requestQueue3 = new ActiveMQQueue("request3");

	private static Destination requestQueue4 = new ActiveMQQueue("request4");

	private static Destination requestQueue5 = new ActiveMQQueue("request5");

	private static Destination requestQueue6 = new ActiveMQQueue("request6");

	private static Destination requestQueue7 = new ActiveMQQueue("request7");

	private static Destination replyQueue7 = new ActiveMQQueue("reply7");

	@Rule
	public Log4j2LevelAdjuster adjuster = Log4j2LevelAdjuster.trace();

	@Test
	public void testContainerWithDest() throws Exception {
		BeanFactory beanFactory = mock(BeanFactory.class);
		when(beanFactory.containsBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)).thenReturn(true);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();
		when(beanFactory.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class))
				.thenReturn(scheduler);
		final JmsOutboundGateway gateway = new JmsOutboundGateway();
		gateway.setBeanFactory(beanFactory);
		ConnectionFactory connectionFactory = getConnectionFactory();
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
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			latch1.countDown();
			try {
				reply.set(gateway.handleRequestMessage(new GenericMessage<String>("foo")));
			}
			finally {
				latch2.countDown();
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		JmsTemplate template = new JmsTemplate();
		template.setConnectionFactory(connectionFactory);
		template.setReceiveTimeout(10000);
		javax.jms.Message request = template.receive(requestQueue1);
		assertNotNull(request);
		final javax.jms.Message jmsReply = request;
		template.send(request.getJMSReplyTo(), (MessageCreator) session -> jmsReply);
		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		assertNotNull(reply.get());

		gateway.stop();
		scheduler.destroy();
		exec.shutdown();
	}

	@Test
	public void testContainerWithDestNoCorrelation() throws Exception {
		BeanFactory beanFactory = mock(BeanFactory.class);
		when(beanFactory.containsBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)).thenReturn(true);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();
		when(beanFactory.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class))
				.thenReturn(scheduler);
		final JmsOutboundGateway gateway = new JmsOutboundGateway();
		gateway.setBeanFactory(beanFactory);
		gateway.setConnectionFactory(getConnectionFactory());
		gateway.setRequestDestination(requestQueue2);
		gateway.setReplyDestination(replyQueue2);
		gateway.setUseReplyContainer(true);
		gateway.afterPropertiesSet();
		gateway.start();
		final AtomicReference<Object> reply = new AtomicReference<Object>();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			latch1.countDown();
			try {
				reply.set(gateway.handleRequestMessage(new GenericMessage<String>("foo")));
			}
			finally {
				latch2.countDown();
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		JmsTemplate template = new JmsTemplate();
		template.setConnectionFactory(getConnectionFactory());
		template.setReceiveTimeout(10000);
		javax.jms.Message request = template.receive(requestQueue2);
		assertNotNull(request);
		final javax.jms.Message jmsReply = request;
		template.send(request.getJMSReplyTo(), (MessageCreator) session -> {
			jmsReply.setJMSCorrelationID(jmsReply.getJMSMessageID());
			return jmsReply;
		});
		assertTrue(latch2.await(20, TimeUnit.SECONDS));
		assertNotNull(reply.get());

		gateway.stop();
		scheduler.destroy();
		exec.shutdownNow();
	}

	@Test
	public void testContainerWithDestName() throws Exception {
		BeanFactory beanFactory = mock(BeanFactory.class);
		when(beanFactory.containsBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)).thenReturn(true);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();
		when(beanFactory.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class))
				.thenReturn(scheduler);
		final JmsOutboundGateway gateway = new JmsOutboundGateway();
		gateway.setBeanFactory(beanFactory);
		gateway.setConnectionFactory(getConnectionFactory());
		gateway.setRequestDestination(requestQueue3);
		gateway.setReplyDestinationName("reply3");
		gateway.setCorrelationKey("JMSCorrelationID");
		gateway.setUseReplyContainer(true);
		gateway.afterPropertiesSet();
		gateway.start();
		final AtomicReference<Object> reply = new AtomicReference<Object>();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			latch1.countDown();
			try {
				reply.set(gateway.handleRequestMessage(new GenericMessage<String>("foo")));
			}
			finally {
				latch2.countDown();
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		JmsTemplate template = new JmsTemplate();
		template.setConnectionFactory(getConnectionFactory());
		template.setReceiveTimeout(10000);
		javax.jms.Message request = template.receive(requestQueue3);
		assertNotNull(request);
		final javax.jms.Message jmsReply = request;
		template.send(request.getJMSReplyTo(), session -> jmsReply);
		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		assertNotNull(reply.get());

		gateway.stop();
		scheduler.destroy();
		exec.shutdownNow();
	}

	@Test
	public void testContainerWithDestNameNoCorrelation() throws Exception {
		BeanFactory beanFactory = mock(BeanFactory.class);
		when(beanFactory.containsBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)).thenReturn(true);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();
		when(beanFactory.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class))
				.thenReturn(scheduler);
		final JmsOutboundGateway gateway = new JmsOutboundGateway();
		gateway.setBeanFactory(beanFactory);
		gateway.setConnectionFactory(getConnectionFactory());
		gateway.setRequestDestination(requestQueue4);
		gateway.setReplyDestinationName("reply4");
		gateway.setUseReplyContainer(true);
		gateway.afterPropertiesSet();
		gateway.start();
		final AtomicReference<Object> reply = new AtomicReference<>();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			latch1.countDown();
			try {
				reply.set(gateway.handleRequestMessage(new GenericMessage<>("foo")));
			}
			finally {
				latch2.countDown();
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		JmsTemplate template = new JmsTemplate();
		template.setConnectionFactory(getConnectionFactory());
		template.setReceiveTimeout(10000);
		javax.jms.Message request = template.receive(requestQueue4);
		assertNotNull(request);
		final javax.jms.Message jmsReply = request;
		template.send(request.getJMSReplyTo(), session -> {
			jmsReply.setJMSCorrelationID(jmsReply.getJMSMessageID());
			return jmsReply;
		});
		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		assertNotNull(reply.get());

		gateway.stop();
		scheduler.destroy();
		exec.shutdownNow();
	}

	@Test
	public void testContainerWithTemporary() throws Exception {
		BeanFactory beanFactory = mock(BeanFactory.class);
		when(beanFactory.containsBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)).thenReturn(true);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();
		when(beanFactory.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class))
				.thenReturn(scheduler);
		final JmsOutboundGateway gateway = new JmsOutboundGateway();
		gateway.setBeanFactory(beanFactory);
		gateway.setConnectionFactory(getConnectionFactory());
		gateway.setRequestDestination(requestQueue5);
		gateway.setCorrelationKey("JMSCorrelationID");
		gateway.setUseReplyContainer(true);
		gateway.setComponentName("testContainerWithTemporary.gateway");
		gateway.afterPropertiesSet();
		gateway.start();
		final AtomicReference<Object> reply = new AtomicReference<>();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			latch1.countDown();
			try {
				reply.set(gateway.handleRequestMessage(new GenericMessage<>("foo")));
			}
			finally {
				latch2.countDown();
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		JmsTemplate template = new JmsTemplate();
		template.setConnectionFactory(getConnectionFactory());
		template.setReceiveTimeout(10000);
		javax.jms.Message request = template.receive(requestQueue5);
		assertNotNull(request);
		final javax.jms.Message jmsReply = request;
		template.send(request.getJMSReplyTo(), session -> jmsReply);
		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		assertNotNull(reply.get());

		gateway.stop();
		scheduler.destroy();
		exec.shutdownNow();
	}

	@Test
	public void testContainerWithTemporaryNoCorrelation() throws Exception {
		BeanFactory beanFactory = mock(BeanFactory.class);
		when(beanFactory.containsBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)).thenReturn(true);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();
		when(beanFactory.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class))
				.thenReturn(scheduler);
		final JmsOutboundGateway gateway = new JmsOutboundGateway();
		gateway.setBeanFactory(beanFactory);
		gateway.setConnectionFactory(getConnectionFactory());
		gateway.setRequestDestination(requestQueue6);
		gateway.setUseReplyContainer(true);
		gateway.afterPropertiesSet();
		gateway.start();
		final AtomicReference<Object> reply = new AtomicReference<>();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			latch1.countDown();
			try {
				reply.set(gateway.handleRequestMessage(new GenericMessage<>("foo")));
			}
			finally {
				latch2.countDown();
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		JmsTemplate template = new JmsTemplate();
		template.setConnectionFactory(getConnectionFactory());
		template.setReceiveTimeout(10000);
		javax.jms.Message request = template.receive(requestQueue6);
		assertNotNull(request);
		final javax.jms.Message jmsReply = request;
		template.send(request.getJMSReplyTo(), session -> {
			jmsReply.setJMSCorrelationID(jmsReply.getJMSMessageID());
			return jmsReply;
		});
		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		assertNotNull(reply.get());

		gateway.stop();
		scheduler.destroy();
		exec.shutdownNow();
	}

	@Test
	public void testLazyContainerWithDest() throws Exception {
		BeanFactory beanFactory = mock(BeanFactory.class);
		when(beanFactory.containsBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)).thenReturn(true);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();
		when(beanFactory.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class))
				.thenReturn(scheduler);
		final JmsOutboundGateway gateway = new JmsOutboundGateway();
		gateway.setBeanFactory(beanFactory);
		gateway.setConnectionFactory(getConnectionFactory());
		gateway.setRequestDestination(requestQueue7);
		gateway.setReplyDestination(replyQueue7);
		gateway.setCorrelationKey("JMSCorrelationID");
		gateway.setUseReplyContainer(true);
		gateway.setIdleReplyContainerTimeout(1, TimeUnit.SECONDS);
		gateway.setRequiresReply(true);
		gateway.setReceiveTimeout(20000);
		gateway.afterPropertiesSet();
		gateway.start();
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			JmsTemplate template = new JmsTemplate();
			template.setConnectionFactory(getConnectionFactory());
			template.setReceiveTimeout(20000);
			receiveAndSend(template);
			receiveAndSend(template);
		});

		assertNotNull(gateway.handleRequestMessage(new GenericMessage<>("foo")));
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(gateway, "replyContainer",
				DefaultMessageListenerContainer.class);
		int n = 0;
		while (n++ < 100 && container.isRunning()) {
			Thread.sleep(100);
		}
		assertFalse(container.isRunning());
		assertNotNull(gateway.handleRequestMessage(new GenericMessage<>("foo")));
		assertTrue(container.isRunning());

		gateway.stop();
		assertFalse(container.isRunning());
		scheduler.destroy();
		exec.shutdownNow();
	}

	private void receiveAndSend(JmsTemplate template) {
		javax.jms.Message request = template.receive(requestQueue7);
		final javax.jms.Message jmsReply = request;
		try {
			template.send(request.getJMSReplyTo(), session -> jmsReply);
		}
		catch (JmsException | JMSException e) {
		}
	}

	private ConnectionFactory getConnectionFactory() {
		ActiveMQConnectionFactory activeMQConnectionFactory =
				new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
		CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(activeMQConnectionFactory);
		cachingConnectionFactory.setCacheConsumers(false);
		return cachingConnectionFactory;
	}

}
