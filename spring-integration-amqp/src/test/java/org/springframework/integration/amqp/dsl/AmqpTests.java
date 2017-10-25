/*
 * Copyright 2014-2017 the original author or authors.
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

package org.springframework.integration.amqp.dsl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.BrokerRunning;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.channel.AbstractAmqpChannel;
import org.springframework.integration.amqp.inbound.AmqpInboundGateway;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class AmqpTests {

	@ClassRule
	public static BrokerRunning brokerRunning =
			BrokerRunning.isRunningWithEmptyQueues("amqpOutboundInput", "amqpReplyChannel", "asyncReplies",
					"defaultReplyTo", "si.dsl.test", "testTemplateChannelTransacted");

	@Autowired
	private ConnectionFactory rabbitConnectionFactory;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	@Qualifier("queue")
	private Queue amqpQueue;

	@Autowired
	private AmqpInboundGateway amqpInboundGateway;

	@Autowired(required = false)
	@Qualifier("amqpInboundGatewayContainer")
	private SimpleMessageListenerContainer amqpInboundGatewayContainer;

	@Autowired
	private Lifecycle asyncOutboundGateway;

	@AfterClass
	public static void tearDown() {
		brokerRunning.removeTestQueues();
	}

	@Test
	public void testAmqpInboundGatewayFlow() {
		assertNotNull(this.amqpInboundGatewayContainer);
		assertSame(this.amqpTemplate, TestUtils.getPropertyValue(this.amqpInboundGateway, "amqpTemplate"));

		Object result = this.amqpTemplate.convertSendAndReceive(this.amqpQueue.getName(), "world");
		assertEquals("HELLO WORLD", result);

		this.amqpInboundGateway.stop();
		//INTEXT-209
		this.amqpInboundGateway.start();

		this.amqpTemplate.convertAndSend(this.amqpQueue.getName(), "world");
		((RabbitTemplate) this.amqpTemplate).setReceiveTimeout(10000);
		result = this.amqpTemplate.receiveAndConvert("defaultReplyTo");
		assertEquals("HELLO WORLD", result);
	}

	@Autowired
	@Qualifier("amqpOutboundInput")
	private MessageChannel amqpOutboundInput;

	@Autowired
	@Qualifier("amqpReplyChannel.channel")
	private PollableChannel amqpReplyChannel;

	@Test
	public void testAmqpOutboundFlow() throws Exception {
		this.amqpOutboundInput.send(MessageBuilder.withPayload("hello through the amqp")
				.setHeader("routingKey", "si.dsl.test")
				.build());
		Message<?> receive = null;
		int i = 0;
		do {
			receive = this.amqpReplyChannel.receive();
			if (receive != null) {
				break;
			}
			Thread.sleep(100);
			i++;
		}
		while (i < 10);

		assertNotNull(receive);
		assertEquals("HELLO THROUGH THE AMQP", receive.getPayload());

		((Lifecycle) this.amqpOutboundInput).stop();
	}

	@Test
	public void testTemplateChannelTransacted() {
		IntegrationFlowBuilder flow = IntegrationFlows.from(Amqp.channel("testTemplateChannelTransacted",
				this.rabbitConnectionFactory)
				.autoStartup(false)
				.templateChannelTransacted(true));
		assertTrue(TestUtils.getPropertyValue(flow, "currentMessageChannel.amqpTemplate.transactional",
				Boolean.class));
	}

	@Autowired
	@Qualifier("amqpAsyncOutboundFlow.input")
	private MessageChannel amqpAsyncOutboundFlowInput;

	@Test
	public void testAmqpAsyncOutboundGatewayFlow() throws Exception {
		QueueChannel replyChannel = new QueueChannel();
		this.amqpAsyncOutboundFlowInput.send(MessageBuilder.withPayload("async gateway")
				.setReplyChannel(replyChannel)
				.build());

		Message<?> receive = replyChannel.receive(10000);
		assertNotNull(receive);
		assertEquals("HELLO ASYNC GATEWAY", receive.getPayload());

		this.asyncOutboundGateway.stop();
	}

	@Autowired
	private AbstractAmqpChannel unitChannel;

	@Autowired
	private AmqpHeaderMapper mapperIn;

	@Autowired
	private AmqpHeaderMapper mapperOut;

	@Test
	public void unitTestChannel() {
		assertEquals(MessageDeliveryMode.NON_PERSISTENT,
				TestUtils.getPropertyValue(this.unitChannel, "defaultDeliveryMode"));
		assertSame(this.mapperIn, TestUtils.getPropertyValue(this.unitChannel, "inboundHeaderMapper"));
		assertSame(this.mapperOut, TestUtils.getPropertyValue(this.unitChannel, "outboundHeaderMapper"));
		assertTrue(TestUtils.getPropertyValue(this.unitChannel, "extractPayload", Boolean.class));
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public ConnectionFactory rabbitConnectionFactory() {
			return new CachingConnectionFactory("localhost");
		}

		@Bean
		public RabbitTemplate amqpTemplate() {
			return new RabbitTemplate(rabbitConnectionFactory());
		}

		@Bean
		public RabbitAdmin amqpAdmin() {
			return new RabbitAdmin(rabbitConnectionFactory());
		}

		@Bean
		public Queue queue() {
			return new AnonymousQueue();
		}

		@Bean
		public Queue defaultReplyTo() {
			return new Queue("defaultReplyTo");
		}

		@Bean
		public IntegrationFlow amqpFlow(ConnectionFactory rabbitConnectionFactory, AmqpTemplate amqpTemplate) {
			return IntegrationFlows
					.from(Amqp.inboundGateway(rabbitConnectionFactory, amqpTemplate, queue())
							.id("amqpInboundGateway")
							.configureContainer(c -> c
								.id("amqpInboundGatewayContainer")
								.recoveryInterval(5000)
								.concurrentConsumers(1))
							.defaultReplyTo(defaultReplyTo().getName()))
					.transform("hello "::concat)
					.transform(String.class, String::toUpperCase)
					.get();
		}

		// syntax only
		public IntegrationFlow amqpDMLCFlow(ConnectionFactory rabbitConnectionFactory, AmqpTemplate amqpTemplate) {
			return IntegrationFlows
					.from(Amqp.inboundGateway(new DirectMessageListenerContainer())
							.id("amqpInboundGateway")
							.configureContainer(c -> c
								.recoveryInterval(5000)
								.consumersPerQueue(1))
							.defaultReplyTo(defaultReplyTo().getName()))
					.transform("hello "::concat)
					.transform(String.class, String::toUpperCase)
					.get();
		}

		@Bean
		public IntegrationFlow amqpOutboundFlow(ConnectionFactory rabbitConnectionFactory, AmqpTemplate amqpTemplate) {
			return IntegrationFlows.from(Amqp.channel("amqpOutboundInput", rabbitConnectionFactory))
					.handle(Amqp.outboundAdapter(amqpTemplate).routingKeyExpression("headers.routingKey"))
					.get();
		}

		@Bean
		public Queue fooQueue() {
			return new Queue("si.dsl.test");
		}

		@Bean
		public Queue amqpReplyChannel() {
			return new Queue("amqpReplyChannel");
		}

		@Bean
		public IntegrationFlow amqpInboundFlow(ConnectionFactory rabbitConnectionFactory) {
			return IntegrationFlows.from(Amqp.inboundAdapter(rabbitConnectionFactory, fooQueue())
					.id("amqpInboundFlowAdapter"))
					.transform(String.class, String::toUpperCase)
					.channel(Amqp.pollableChannel(rabbitConnectionFactory)
							.queueName("amqpReplyChannel")
							.channelTransacted(true))
					.get();
		}

		@Bean
		public Queue asyncReplies() {
			return new Queue("asyncReplies");
		}

		@Bean
		public AsyncRabbitTemplate asyncRabbitTemplate(ConnectionFactory rabbitConnectionFactory) {
			return new AsyncRabbitTemplate(rabbitConnectionFactory, "", "", "asyncReplies");
		}

		@Bean
		public IntegrationFlow amqpAsyncOutboundFlow(AsyncRabbitTemplate asyncRabbitTemplate) {
			return f -> f
					.handle(Amqp.asyncOutboundGateway(asyncRabbitTemplate)
							.routingKeyFunction(m -> queue().getName()),
							e -> e.id("asyncOutboundGateway"));
		}

		@Bean
		public AbstractAmqpChannel unitChannel(ConnectionFactory rabbitConnectionFactory) {
			return Amqp.pollableChannel(rabbitConnectionFactory)
					.queueName("si.dsl.test")
					.channelTransacted(true)
					.extractPayload(true)
					.inboundHeaderMapper(mapperIn())
					.outboundHeaderMapper(mapperOut())
					.defaultDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
					.get();
		}

		@Bean
		public AmqpHeaderMapper mapperIn() {
			return DefaultAmqpHeaderMapper.inboundMapper();
		}

		@Bean
		public AmqpHeaderMapper mapperOut() {
			return DefaultAmqpHeaderMapper.outboundMapper();
		}

	}

}
