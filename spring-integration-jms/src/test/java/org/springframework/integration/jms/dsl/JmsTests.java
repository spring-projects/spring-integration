/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.jms.dsl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.QueueChannelSpec;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.JmsDestinationPollingSource;
import org.springframework.integration.jms.SubscribableJmsChannel;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Nasko Vasilev
 * @author Artem Vozhdayenko
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class JmsTests extends ActiveMQMultiContextTests {

	@Autowired
	private ListableBeanFactory beanFactory;

	@Autowired
	private ControlBusGateway controlBus;

	@Autowired
	@Qualifier("flow1QueueChannel")
	private PollableChannel outputChannel;

	@Autowired
	@Qualifier("jmsOutboundFlow.input")
	private MessageChannel jmsOutboundInboundChannel;

	@Autowired
	@Qualifier("jmsOutboundInboundReplyChannel")
	private PollableChannel jmsOutboundInboundReplyChannel;

	@Autowired
	private JmsDestinationPollingSource jmsDestinationPollingSource;

	@Autowired
	@Qualifier("jmsOutboundGatewayFlow.input")
	private MessageChannel jmsOutboundGatewayChannel;

	@Autowired
	private TestChannelInterceptor testChannelInterceptor;

	@Autowired
	private PollableChannel jmsPubSubBridgeChannel;

	@Autowired
	private PollableChannel jmsPubSubBridgeChannel2;

	@Autowired
	@Qualifier("jmsOutboundGateway.handler")
	private MessageHandler jmsOutboundGatewayHandler;

	@Autowired
	private AtomicBoolean jmsMessageDrivenChannelCalled;

	@Autowired
	private AtomicBoolean jmsInboundGatewayChannelCalled;

	@Autowired(required = false)
	@Qualifier("jmsOutboundFlowTemplate")
	private JmsTemplate jmsOutboundFlowTemplate;

	@Autowired(required = false)
	@Qualifier("jmsMessageDrivenRedeliveryFlowContainer")
	private MessageListenerContainer jmsMessageDrivenRedeliveryFlowContainer;

	@Autowired
	private CountDownLatch redeliveryLatch;

	@Autowired
	JmsTemplate jmsTemplate;

	@Test
	public void testPollingFlow() {
		this.controlBus.send("@'integerMessageSource.inboundChannelAdapter'.start()");
		assertThat(this.beanFactory.getBean("integerChannel")).isInstanceOf(FixedSubscriberChannel.class);
		for (int i = 0; i < 5; i++) {
			Message<?> message = this.outputChannel.receive(20000);
			assertThat(message).isNotNull();
			assertThat(message.getPayload()).isEqualTo("" + i);
		}
		this.controlBus.send("@'integerMessageSource.inboundChannelAdapter'.stop()");

		assertThat(((InterceptableChannel) this.outputChannel).getInterceptors())
				.contains(this.testChannelInterceptor);
		assertThat(this.testChannelInterceptor.invoked.get()).isGreaterThanOrEqualTo(5);

	}

	@Test
	public void testJmsOutboundInboundFlow() {
		JmsTemplate jmsTemplate =
				TestUtils.getPropertyValue(this.jmsDestinationPollingSource, "jmsTemplate", JmsTemplate.class);

		assertThat(jmsTemplate.getReceiveTimeout()).isEqualTo(1000);

		this.jmsOutboundInboundChannel.send(MessageBuilder.withPayload("hello THROUGH the JMS")
				.setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, "jmsInbound")
				.build());

		Message<?> receive = this.jmsOutboundInboundReplyChannel.receive(10000);

		assertThat(receive)
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("HELLO THROUGH THE JMS");

		this.jmsOutboundInboundChannel.send(MessageBuilder.withPayload("hello THROUGH the JMS")
				.setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, "jmsMessageDriven")
				.build());

		receive = this.jmsOutboundInboundReplyChannel.receive(10000);

		assertThat(receive)
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("hello through the jms");

		assertThat(this.jmsMessageDrivenChannelCalled.get()).isTrue();

		this.jmsOutboundInboundChannel.send(MessageBuilder.withPayload("    foo    ")
				.setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, "containerSpecDestination")
				.build());

		receive = this.jmsOutboundInboundReplyChannel.receive(10000);

		assertThat(receive)
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("foo");

		assertThat(this.jmsOutboundFlowTemplate).isNotNull();
	}

	@Test
	public void testJmsPipelineFlow() {
		assertThat(TestUtils.getPropertyValue(this.jmsOutboundGatewayHandler, "idleReplyContainerTimeout", Long.class))
				.isEqualTo(10000L);
		PollableChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("hello through the jms pipeline")
				.setReplyChannel(replyChannel)
				.setHeader("destination", "jmsPipelineTest")
				.build();
		this.jmsOutboundGatewayChannel.send(message);

		Message<?> receive = replyChannel.receive(5000);

		assertThat(receive)
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("HELLO THROUGH THE JMS PIPELINE");

		assertThat(this.jmsInboundGatewayChannelCalled.get()).isTrue();

		message = MessageBuilder.withPayload("junk")
				.setReplyChannel(replyChannel)
				.setHeader("destination", "jmsPipelineTest")
				.build();

		this.jmsOutboundGatewayChannel.send(message);

		receive = replyChannel.receive(5000);

		assertThat(receive)
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("error: junk is not convertible");
	}

	@Test
	public void testPubSubFlow() {
		JmsTemplate template = new JmsTemplate(connectionFactory);
		template.setPubSubDomain(true);
		template.setDefaultDestinationName("pubsub");
		template.convertAndSend("foo");
		Message<?> received = this.jmsPubSubBridgeChannel.receive(5000);
		assertThat(received)
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("foo");
		received = this.jmsPubSubBridgeChannel2.receive(5000);
		assertThat(received)
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("foo");
	}

	@Test
	public void testJmsRedeliveryFlow() throws InterruptedException {
		this.jmsOutboundInboundChannel.send(MessageBuilder.withPayload("foo")
				.setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, "jmsMessageDrivenRedelivery")
				.build());

		assertThat(this.redeliveryLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(this.jmsMessageDrivenRedeliveryFlowContainer).isNotNull();
		this.jmsMessageDrivenRedeliveryFlowContainer.stop();
	}

	@Test
	public void customReplyToHeader() throws JMSException {
		this.jmsTemplate.send("jmsPipelineTest", session -> {
			TextMessage message = session.createTextMessage("test data");
			message.setStringProperty("myReplyTo", "replyToQueue");
			return message;
		});

		jakarta.jms.Message replyMessage = this.jmsTemplate.receive("replyToQueue");
		assertThat(replyMessage).isNotNull();
		assertThat(replyMessage.getBody(String.class)).isEqualTo("TEST DATA");
	}

	@MessagingGateway(defaultRequestChannel = "controlBus.input")
	private interface ControlBusGateway {

		void send(String command);

	}

	@Configuration
	@EnableIntegration
	@IntegrationComponentScan
	public static class ContextConfiguration {

		@Bean
		public JmsTemplate jmsTemplate() {
			JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
			jmsTemplate.setReceiveTimeout(10_000);
			return jmsTemplate;
		}

		@Bean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerSpec poller() {
			return Pollers.fixedDelay(1000);
		}

		@Bean
		public IntegrationFlow controlBus() {
			return IntegrationFlowDefinition::controlBus;
		}

		@Bean
		@InboundChannelAdapter(value = "flow1.input", autoStartup = "false", poller = @Poller(fixedRate = "100"))
		public MessageSource<?> integerMessageSource() {
			MethodInvokingMessageSource source = new MethodInvokingMessageSource();
			source.setObject(new AtomicInteger());
			source.setMethodName("getAndIncrement");
			return source;
		}

		@Bean
		public IntegrationFlow flow1() {
			return f -> f
					.fixedSubscriberChannel("integerChannel")
					.transform("payload.toString()")
					.channel(Jms.pollableChannel("flow1QueueChannel", amqFactory)
							.destination("flow1QueueChannel"));
		}

		@Bean
		public IntegrationFlow jmsOutboundFlow() {
			return f -> f
					.handle(Jms.outboundAdapter(connectionFactory)
							.destinationExpression("headers." + SimpMessageHeaderAccessor.DESTINATION_HEADER)
							.configureJmsTemplate(t -> t.id("jmsOutboundFlowTemplate")));
		}

		@Bean
		public QueueChannelSpec jmsOutboundInboundReplyChannel() {
			return MessageChannels.queue();
		}

		@Bean
		public IntegrationFlow jmsInboundFlow(QueueChannel jmsOutboundInboundReplyChannel) {
			return IntegrationFlow
					.from(Jms.inboundAdapter(amqFactory).destination("jmsInbound"))
					.<String, String>transform(String::toUpperCase)
					.channel(jmsOutboundInboundReplyChannel)
					.get();
		}

		@Bean
		public JmsPublishSubscribeMessageChannelSpec jmsPublishSubscribeChannel() {
			return Jms.publishSubscribeChannel(amqFactory)
					.destination("pubsub");
		}

		@Bean
		public IntegrationFlow pubSubFlow(SubscribableJmsChannel jmsPublishSubscribeChannel) {
			return f -> f
					.publishSubscribeChannel(jmsPublishSubscribeChannel,
							pubsub -> pubsub
									.subscribe(subFlow -> subFlow
											.channel(c -> c.queue("jmsPubSubBridgeChannel")))
									.subscribe(subFlow -> subFlow
											.channel(c -> c.queue("jmsPubSubBridgeChannel2"))));
		}

		@Bean
		public IntegrationFlow jmsMessageDrivenFlow() {
			return IntegrationFlow
					.from(Jms.messageDrivenChannelAdapter(amqFactory,
									DefaultMessageListenerContainer.class)
							.outputChannel(jmsMessageDrivenInputChannel())
							.destination("jmsMessageDriven")
							.configureListenerContainer(c -> c.clientId("foo")))
					.<String, String>transform(String::toLowerCase)
					.channel(jmsOutboundInboundReplyChannel())
					.get();
		}

		@Bean
		public AtomicBoolean jmsMessageDrivenChannelCalled() {
			return new AtomicBoolean();
		}

		@Bean
		public MessageChannel jmsMessageDrivenInputChannel() {
			DirectChannel directChannel = new DirectChannel();
			directChannel.addInterceptor(new ChannelInterceptor() {

				@Override
				public Message<?> preSend(Message<?> message, MessageChannel channel) {
					jmsMessageDrivenChannelCalled().set(true);
					return message;
				}

			});
			return directChannel;
		}

		@Bean
		public IntegrationFlow jmsMessageDrivenFlowWithContainer() {
			return IntegrationFlow
					.from(Jms.messageDrivenChannelAdapter(
							Jms.container(amqFactory, "containerSpecDestination")
									.pubSubDomain(false)
									.taskExecutor(Executors.newCachedThreadPool())))
					.transform(String::trim)
					.channel(jmsOutboundInboundReplyChannel())
					.get();
		}

		@Bean
		public IntegrationFlow jmsOutboundGatewayFlow() {
			return f -> f.handle(Jms.outboundGateway(connectionFactory)
							.replyContainer(c -> c.idleReplyContainerTimeout(10))
							.replyPubSubDomain(true)
							.requestDestination("jmsPipelineTest"),
					e -> e.id("jmsOutboundGateway"));
		}

		@Bean
		public IntegrationFlow jmsInboundGatewayFlow() {
			return IntegrationFlow.from(
							Jms.inboundGateway(amqFactory)
									.requestChannel(jmsInboundGatewayInputChannel())
									.replyTimeout(1)
									.errorOnTimeout(true)
									.errorChannel(new FixedSubscriberChannel(new AbstractReplyProducingMessageHandler() {

										@Override
										protected Object handleRequestMessage(Message<?> requestMessage) {
											return "error: " +
													((MessageTimeoutException) requestMessage.getPayload())
															.getFailedMessage().getPayload() + " is not convertible";
										}

									}))
									.requestDestination("jmsPipelineTest")
									.replyToFunction(message -> message.getStringProperty("myReplyTo"))
									.configureListenerContainer(c ->
											c.transactionManager(mock(PlatformTransactionManager.class))))
					.filter(payload -> !"junk".equals(payload))
					.<String, String>transform(String::toUpperCase)
					.get();
		}

		@Bean
		public AtomicBoolean jmsInboundGatewayChannelCalled() {
			return new AtomicBoolean();
		}

		@Bean
		public MessageChannel jmsInboundGatewayInputChannel() {
			DirectChannel directChannel = new DirectChannel();
			directChannel.addInterceptor(new ChannelInterceptor() {

				@Override
				public Message<?> preSend(Message<?> message, MessageChannel channel) {
					jmsInboundGatewayChannelCalled().set(true);
					return message;
				}

			});
			return directChannel;
		}

		@Bean
		public IntegrationFlow jmsMessageDrivenRedeliveryFlow() {
			return IntegrationFlow
					.from(Jms.messageDrivenChannelAdapter(amqFactory)
							.errorChannel("errorChannelForRedelivery")
							.destination("jmsMessageDrivenRedelivery")
							.configureListenerContainer(c -> c
									.transactionManager(mock(PlatformTransactionManager.class))
									.subscriptionDurable(false)
									.subscriptionShared(false)
									.id("jmsMessageDrivenRedeliveryFlowContainer")))
					.<String, String>transform(p -> {
						throw new RuntimeException("intentional");
					})
					.get();
		}

		@Bean
		public CountDownLatch redeliveryLatch() {
			return new CountDownLatch(3);
		}

		@Bean
		public IntegrationFlow errorHandlingFlow() {
			return IntegrationFlow.from("errorChannelForRedelivery")
					.handle(m -> {
						MessagingException exception = (MessagingException) m.getPayload();
						redeliveryLatch().countDown();
						throw exception;
					})
					.get();
		}

		@Bean
		@GlobalChannelInterceptor(patterns = "flow1QueueChannel")
		ChannelInterceptor testChannelInterceptor() {
			return new TestChannelInterceptor();
		}

	}

	public static class TestChannelInterceptor implements ChannelInterceptor {

		private final AtomicInteger invoked = new AtomicInteger();

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			this.invoked.incrementAndGet();
			return message;
		}

	}

}
