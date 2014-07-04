/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.amqp.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.amqp.rule.BrokerRunning;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ChannelTests {

	@ClassRule
	public static final BrokerRunning brokerIsRunning = BrokerRunning.isRunning();

	@Autowired
	private PublishSubscribeAmqpChannel channel;

	@Autowired
	private CachingConnectionFactory factory;

	@Autowired
	private ConfigurableApplicationContext context;

	@Test
	public void pubSubLostConnectionTest() throws Exception {
		final CyclicBarrier latch = new CyclicBarrier(2);
		channel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				try {
					latch.await(10, TimeUnit.SECONDS);
				}
				catch (Exception e) {
				}
			}
		});
		channel.send(new GenericMessage<String>("foo"));
		latch.await(10, TimeUnit.SECONDS);
		latch.reset();
		factory.destroy();
		channel.send(new GenericMessage<String>("bar"));
		latch.await(10, TimeUnit.SECONDS);
		context.close();
		assertEquals(0, TestUtils.getPropertyValue(factory, "connectionListener.delegates", Collection.class).size());
	}

	/*
	 * Verify queue is declared if not present and not declared if it is already present.
	 */
	@Test
	public void channelDeclarationTests() {
		RabbitAdmin admin = new RabbitAdmin(this.factory);
		admin.deleteQueue("implicit");
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(this.factory);
		AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);
		PointToPointSubscribableAmqpChannel channel = new PointToPointSubscribableAmqpChannel("implicit", container,
				amqpTemplate);
		channel.setBeanFactory(mock(BeanFactory.class));
		channel.afterPropertiesSet();
		assertNotNull(admin.getQueueProperties("implicit"));
		admin.deleteQueue("implicit");

		admin.deleteQueue("explicit");
		channel.setQueueName("explicit");
		channel.afterPropertiesSet();
		assertNotNull(admin.getQueueProperties("explicit"));

		admin.deleteQueue("explicit");
		admin.declareQueue(new Queue("explicit", false)); // verify no declaration if exists with non-standard props
		channel.afterPropertiesSet();
		assertNotNull(admin.getQueueProperties("explicit"));
		admin.deleteQueue("explicit");
	}

}
