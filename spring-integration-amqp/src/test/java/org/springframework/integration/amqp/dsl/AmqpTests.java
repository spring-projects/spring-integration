/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.amqp.dsl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.rabbitmq.stream.ConsumerBuilder;
import com.rabbitmq.stream.Environment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.RabbitAvailable;
import org.springframework.amqp.rabbit.junit.RabbitAvailableCondition;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.channel.AbstractAmqpChannel;
import org.springframework.integration.amqp.channel.PollableAmqpChannel;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter.BatchMode;
import org.springframework.integration.amqp.inbound.AmqpInboundGateway;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.AmqpMessageHeaderErrorMessageStrategy;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnablePublisher;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.StringObjectMapBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.rabbit.stream.listener.ConsumerCustomizer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
@SpringJUnitConfig
@RabbitAvailable(queues = {"amqpOutboundInput", "amqpReplyChannel", "asyncReplies",
		"defaultReplyTo", "si.dsl.test", "si.dsl.exception.test.dlq",
		"si.dsl.conv.exception.test.dlq", "testTemplateChannelTransacted", "publisherQueue"})
@DirtiesContext
public class AmqpTests {

	@Autowired
	private ConnectionFactory rabbitConnectionFactory;

	@Autowired
	private IntegrationFlowContext integrationFlowContext;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	@Qualifier("queue")
	private Queue amqpQueue;

	@Autowired
	@Qualifier("queue2")
	private Queue amqpQueue2;

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
		assertThat(this.amqpInboundGatewayContainer).isNotNull();
		assertThat(TestUtils.getPropertyValue(this.amqpInboundGateway, "amqpTemplate")).isSameAs(this.amqpTemplate);

		Object result = this.amqpTemplate.convertSendAndReceive(this.amqpQueue.getName(), "world");
		assertThat(result).isEqualTo("HELLO WORLD");

		this.amqpInboundGateway.stop();
		//INTEXT-209
		this.amqpInboundGateway.start();

		this.amqpTemplate.convertAndSend(this.amqpQueue.getName(), "world");
		((RabbitTemplate) this.amqpTemplate).setReceiveTimeout(10000);
		result = this.amqpTemplate.receiveAndConvert("defaultReplyTo");
		assertThat(result).isEqualTo("HELLO WORLD");
	}

	@Autowired
	@Qualifier("amqpOutboundInput")
	private MessageChannel amqpOutboundInput;

	@Autowired
	@Qualifier("amqpReplyChannel.channel")
	private PollableChannel amqpReplyChannel;

	@Test
	public void testAmqpOutboundFlow() {
		this.amqpOutboundInput.send(MessageBuilder.withPayload("one")
				.setHeader("routingKey", "si.dsl.test")
				.build());
		this.amqpOutboundInput.send(MessageBuilder.withPayload("two")
				.setHeader("routingKey", "si.dsl.test")
				.build());
		Message<?> receive = this.amqpReplyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("[ONE, TWO]");

		((Lifecycle) this.amqpOutboundInput).stop();
	}

	@Test
	public void testTemplateChannelTransacted() {
		IntegrationFlowBuilder flow = IntegrationFlow.from(Amqp.channel("testTemplateChannelTransacted",
						this.rabbitConnectionFactory)
				.autoStartup(false)
				.templateChannelTransacted(true));
		assertThat(TestUtils.getPropertyValue(flow, "currentMessageChannel.amqpTemplate.transactional",
				Boolean.class)).isTrue();
	}

	@Autowired
	@Qualifier("amqpAsyncOutboundFlow.input")
	private MessageChannel amqpAsyncOutboundFlowInput;

	@Test
	public void testAmqpAsyncOutboundGatewayFlow() {
		QueueChannel replyChannel = new QueueChannel();
		this.amqpAsyncOutboundFlowInput.send(MessageBuilder.withPayload("async gateway")
				.setReplyChannel(replyChannel)
				.build());

		Message<?> receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("HELLO ASYNC GATEWAY");

		this.asyncOutboundGateway.stop();
	}

	@Autowired
	private AtomicReference<ListenerExecutionFailedException> lefe;

	@Autowired
	private AtomicReference<?> raw;

	@Test
	public void testInboundMessagingExceptionFlow() {
		this.amqpTemplate.convertAndSend("si.dsl.exception.test", "foo");
		assertThat(this.amqpTemplate.receive("si.dsl.exception.test.dlq", 30_000)).isNotNull();
		assertThat(this.lefe.get()).isNull();
		assertThat(this.raw.get()).isNotNull();
		this.raw.set(null);
	}

	@Test
	public void testInboundConversionExceptionFlow() {
		this.amqpTemplate.convertAndSend("si.dsl.conv.exception.test", "foo");
		assertThat(this.amqpTemplate.receive("si.dsl.conv.exception.test.dlq", 30_000)).isNotNull();
		assertThat(this.lefe.get()).isNotNull();
		assertThat(this.lefe.get().getCause()).isInstanceOf(MessageConversionException.class);
		assertThat(this.raw.get()).isNotNull();
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
	void unitTestChannel() {
		assertThat(TestUtils.getPropertyValue(this.unitChannel, "defaultDeliveryMode"))
				.isEqualTo(MessageDeliveryMode.NON_PERSISTENT);
		assertThat(TestUtils.getPropertyValue(this.unitChannel, "inboundHeaderMapper")).isSameAs(this.mapperIn);
		assertThat(TestUtils.getPropertyValue(this.unitChannel, "outboundHeaderMapper")).isSameAs(this.mapperOut);
		assertThat(TestUtils.getPropertyValue(this.unitChannel, "extractPayload", Boolean.class)).isTrue();
	}

	@Test
	void testContentTypeOverrideWithReplyHeadersMappedLast() {
		IntegrationFlow testFlow =
				IntegrationFlow
						.from(Amqp.inboundGateway(this.rabbitConnectionFactory, this.amqpQueue2)
								.replyHeadersMappedLast(true))
						.transform(Transformers.fromJson())
						.enrich((enricher) -> enricher.property("REPLY_KEY", "REPLY_VALUE"))
						.transform(Transformers.toJson())
						.get();

		IntegrationFlowContext.IntegrationFlowRegistration registration =
				this.integrationFlowContext.registration(testFlow).register();

		RabbitTemplate rabbitTemplate = new RabbitTemplate(this.rabbitConnectionFactory);
		rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

		Object result = rabbitTemplate.convertSendAndReceive(this.amqpQueue2.getName(),
				new HashMap<>(Collections.singletonMap("TEST_KEY", "TEST_VALUE")));

		assertThat(result).isInstanceOf(Map.class);

		@SuppressWarnings("unchecked")
		Map<String, String> resultMap = (Map<String, String>) result;

		assertThat(resultMap)
				.containsEntry("TEST_KEY", "TEST_VALUE")
				.containsEntry("REPLY_KEY", "REPLY_VALUE");

		registration.destroy();
	}

	@Test
	void streamContainer() {
		Environment env = mock(Environment.class);
		given(env.consumerBuilder()).willReturn(mock(ConsumerBuilder.class));
		RabbitStreamInboundChannelAdapterSpec inboundAdapter = RabbitStream.inboundAdapter(env);
		ConsumerCustomizer customizer = mock(ConsumerCustomizer.class);
		inboundAdapter.configureContainer(container -> container.consumerCustomizer(customizer));
		inboundAdapter.start();
		verify(customizer).accept(any(), any());
	}

	@Autowired
	QueueChannel fromRabbitViaPublisher;

	@Test
	void messageReceivedFromRabbitListenerViaPublisher() {
		this.amqpTemplate.convertAndSend("publisherQueue", "test data");

		Message<?> receive = this.fromRabbitViaPublisher.receive(10_000);
		assertThat(receive).isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("TEST DATA");
	}

	@Configuration
	@EnableIntegration
	@EnableRabbit
	@EnablePublisher
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
		public Queue queue2() {
			return new AnonymousQueue();
		}

		@Bean
		public Queue defaultReplyTo() {
			return new Queue("defaultReplyTo");
		}

		@Bean
		public IntegrationFlow amqpFlow(ConnectionFactory rabbitConnectionFactory, AmqpTemplate amqpTemplate) {
			return IntegrationFlow
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
			return IntegrationFlow
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
			return IntegrationFlow.from(Amqp.channel("amqpOutboundInput", rabbitConnectionFactory))
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
			return IntegrationFlow.from(Amqp.inboundAdapter(rabbitConnectionFactory, fooQueue())
							.configureContainer(container -> container.consumerBatchEnabled(true)
									.batchSize(2))
							.batchMode(BatchMode.EXTRACT_PAYLOADS)
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
			return IntegrationFlow.from(Amqp.inboundAdapter(cf, exQueue())
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
			return IntegrationFlow.from(Amqp.inboundAdapter(cf, exConvQueue())
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
		public AmqpPollableMessageChannelSpec<?, PollableAmqpChannel> unitChannel(
				ConnectionFactory rabbitConnectionFactory) {

			return Amqp.pollableChannel(rabbitConnectionFactory)
					.queueName("si.dsl.test")
					.channelTransacted(true)
					.extractPayload(true)
					.inboundHeaderMapper(mapperIn())
					.outboundHeaderMapper(mapperOut())
					.defaultDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
		}

		@Bean
		public AmqpHeaderMapper mapperIn() {
			return DefaultAmqpHeaderMapper.inboundMapper();
		}

		@Bean
		public AmqpHeaderMapper mapperOut() {
			return DefaultAmqpHeaderMapper.outboundMapper();
		}

		@Bean
		public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
				ConnectionFactory rabbitConnectionFactory) {

			SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
			factory.setConnectionFactory(rabbitConnectionFactory);
			return factory;
		}

		@Bean
		QueueChannel fromRabbitViaPublisher() {
			return new QueueChannel();
		}

		@RabbitListener(queuesToDeclare = @org.springframework.amqp.rabbit.annotation.Queue("publisherQueue"))
		@Publisher("fromRabbitViaPublisher")
		@Payload("#args.payload.toUpperCase()")
		public void consumeForPublisher(String payload) {

		}

	}

}
