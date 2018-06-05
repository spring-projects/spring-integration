/*
 * Copyright 2014-2018 the original author or authors.
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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.RabbitAvailable;
import org.springframework.amqp.rabbit.junit.RabbitAvailableCondition;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.channel.AbstractAmqpChannel;
import org.springframework.integration.amqp.inbound.AmqpInboundGateway;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.AmqpMessageHeaderErrorMessageStrategy;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.StringObjectMapBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
@SpringJUnitConfig
@RabbitAvailable(queues = { "amqpOutboundInput", "amqpReplyChannel", "asyncReplies",
							"defaultReplyTo", "si.dsl.test", "si.dsl.exception.test.dlq",
							"si.dsl.conv.exception.test.dlq", "testTemplateChannelTransacted" })
@DirtiesContext
public class AmqpTests {

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

	@AfterAll
	public static void tearDown(ConfigurableApplicationContext context) {
		context.stop(); // prevent queues from being redeclared after deletion
		RabbitAvailableCondition.getBrokerRunning().removeTestQueues("si.dsl.exception.test",
				"si.dsl.conv.exception.test");
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
	private AtomicReference<ListenerExecutionFailedException> lefe;

	@Autowired
	private AtomicReference<?> raw;


	@Test
	public void testInboundMessagingExceptionFlow() {
		this.amqpTemplate.convertAndSend("si.dsl.exception.test", "foo");
		assertNotNull(this.amqpTemplate.receive("si.dsl.exception.test.dlq", 30_000));
		assertNull(this.lefe.get());
		assertNotNull(this.raw.get());
		this.raw.set(null);
	}

	@Test
	public void testInboundConversionExceptionFlow() {
		this.amqpTemplate.convertAndSend("si.dsl.conv.exception.test", "foo");
		assertNotNull(this.amqpTemplate.receive("si.dsl.conv.exception.test.dlq", 30_000));
		assertNotNull(this.lefe.get());
		assertThat(this.lefe.get().getCause(), instanceOf(MessageConversionException.class));
		assertNotNull(this.raw.get());
		this.raw.set(null);
		this.lefe.set(null);
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
		public AtomicReference<ListenerExecutionFailedException> lefe() {
			return new AtomicReference<>();
		}

		@Bean
		public AtomicReference<org.springframework.amqp.core.Message> raw() {
			return new AtomicReference<>();
		}

		@Bean
		public Queue exQueue() {
			return new Queue("si.dsl.exception.test", true, false, false,
					new StringObjectMapBuilder()
						.put("x-dead-letter-exchange", "")
						.put("x-dead-letter-routing-key", exDLQ().getName())
						.get());
		}

		@Bean
		public Queue exDLQ() {
			return new Queue("si.dsl.exception.test.dlq");
		}

		@Bean
		public IntegrationFlow inboundWithExceptionFlow(ConnectionFactory cf) {
			return IntegrationFlows.from(Amqp.inboundAdapter(cf, exQueue())
						.configureContainer(c -> c.defaultRequeueRejected(false))
						.errorChannel("errors.input"))
					.handle(m -> {
						throw new RuntimeException("fail");
					})
					.get();
		}

		@Bean
		public IntegrationFlow errors() {
			return f -> f.handle(m -> {
					raw().set(m.getHeaders().get(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE,
							org.springframework.amqp.core.Message.class));
					if (m.getPayload() instanceof ListenerExecutionFailedException) {
						lefe().set((ListenerExecutionFailedException) m.getPayload());
					}
					throw (RuntimeException) m.getPayload();
				});
		}

		@Bean
		public Queue exConvQueue() {
			return new Queue("si.dsl.conv.exception.test", true, false, false,
					new StringObjectMapBuilder()
						.put("x-dead-letter-exchange", "")
						.put("x-dead-letter-routing-key", exConvDLQ().getName())
						.get());
		}

		@Bean
		public Queue exConvDLQ() {
			return new Queue("si.dsl.conv.exception.test.dlq");
		}

		@Bean
		public IntegrationFlow inboundWithConvExceptionFlow(ConnectionFactory cf) {
			return IntegrationFlows.from(Amqp.inboundAdapter(cf, exConvQueue())
						.configureContainer(c -> c.defaultRequeueRejected(false))
						.messageConverter(new SimpleMessageConverter() {

							@Override
							public Object fromMessage(org.springframework.amqp.core.Message message)
									throws MessageConversionException {
								throw new MessageConversionException("fail");
							}

						})
						.errorChannel("errors.input"))
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
