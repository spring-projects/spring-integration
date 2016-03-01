/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.integration.amqp.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.amqp.rule.BrokerRunning;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
public class AsyncGatewayTests {

	@ClassRule
	public static BrokerRunning brokerRunning = BrokerRunning.isRunningWithEmptyQueues("asyncQ1", "asyncRQ1");

//	@Rule
//	public Log4jLevelAdjuster adjuster = new Log4jLevelAdjuster(Level.TRACE, "org.springframework.integration",
//			"org.springframework.amqp");

	@AfterClass
	public static void tearDown() {
		brokerRunning.removeTestQueues();
	}

	@Test
	public void testConfirmsAndReturns() {
		CachingConnectionFactory ccf = new CachingConnectionFactory("localhost");
		ccf.setPublisherConfirms(true);
		ccf.setPublisherReturns(true);
		RabbitTemplate template = new RabbitTemplate(ccf);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(ccf);
		container.setBeanName("replyContainer");
		container.setQueueNames("asyncRQ1");
		container.afterPropertiesSet();
		container.start();
		AsyncRabbitTemplate asyncTemplate = new AsyncRabbitTemplate(template, container);
		asyncTemplate.setEnableConfirms(true);
		asyncTemplate.setMandatory(true);

		SimpleMessageListenerContainer receiver = new SimpleMessageListenerContainer(ccf);
		receiver.setBeanName("receiver");
		receiver.setQueueNames("asyncQ1");
		final CountDownLatch waitForAckBeforeReplying = new CountDownLatch(1);
		receiver.setMessageListener(new MessageListenerAdapter(new Object() {

			@SuppressWarnings("unused")
			public String handleMessage(String foo) {
				try {
					waitForAckBeforeReplying.await(10, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return foo.toUpperCase();
			}

		}));
		receiver.afterPropertiesSet();
		receiver.start();

		AsyncOutboundGateway gateway = new AsyncOutboundGateway(asyncTemplate);
		QueueChannel outputChannel = new QueueChannel();
		QueueChannel returnChannel = new QueueChannel();
		QueueChannel ackChannel = new QueueChannel();
		QueueChannel nackChannel = new QueueChannel();
		QueueChannel errorChannel = new QueueChannel();
		gateway.setOutputChannel(outputChannel);
		gateway.setReturnChannel(returnChannel);
		gateway.setConfirmAckChannel(ackChannel);
		gateway.setConfirmNackChannel(nackChannel);
		gateway.setConfirmCorrelationExpressionString("#this");
		gateway.setExchangeName("");
		gateway.setRoutingKey("asyncQ1");
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();

		Message<?> message = MessageBuilder.withPayload("foo").setErrorChannel(errorChannel).build();

		gateway.handleMessage(message);

		Message<?> ack = ackChannel.receive(10000);
		assertNotNull(ack);
		assertEquals("foo", ack.getPayload());
		waitForAckBeforeReplying.countDown();

		Message<?> received = outputChannel.receive(10000);
		assertNotNull(received);
		assertEquals("FOO", received.getPayload());

		gateway.setRoutingKey(UUID.randomUUID().toString());
		gateway.handleMessage(message);
		Message<?> returned = returnChannel.receive(10000);
		assertNotNull(returned);
		assertEquals("foo", returned.getPayload());
	}

}
