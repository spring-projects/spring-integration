/*
 * Copyright 2018-2022 the original author or authors.
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

package org.springframework.integration.amqp.inbound;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.RabbitAvailable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.acks.AcknowledgmentCallback.Status;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 *
 * @since 5.0.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
@RabbitAvailable({
		AmqpMessageSourceIntegrationTests.DSL_QUEUE,
		AmqpMessageSourceIntegrationTests.INTERCEPT_QUEUE,
		AmqpMessageSourceIntegrationTests.DLQ,
		AmqpMessageSourceIntegrationTests.NOAUTOACK_QUEUE})
public class AmqpMessageSourceIntegrationTests {

	static final String DSL_QUEUE = "AmqpMessageSourceIntegrationTests";

	static final String QUEUE_WITH_DLQ = "AmqpMessageSourceIntegrationTests.withDLQ";

	static final String DLQ = QUEUE_WITH_DLQ + ".dlq";

	static final String INTERCEPT_QUEUE = "AmqpMessageSourceIntegrationTests.channel";

	static final String NOAUTOACK_QUEUE = "AmqpMessageSourceIntegrationTests.noAutoAck";

	@Autowired
	private Config config;

	@Autowired
	private ConfigurableApplicationContext context;

	@BeforeEach
	public void before() {
		RabbitAdmin admin = new RabbitAdmin(this.config.connectionFactory());
		Queue queue = QueueBuilder.nonDurable(QUEUE_WITH_DLQ)
				.autoDelete()
				.withArgument("x-dead-letter-exchange", "")
				.withArgument("x-dead-letter-routing-key", DLQ)
				.build();
		admin.declareQueue(queue);
		this.context.start();
	}

	@AfterEach
	public void after() {
		this.context.stop();
	}

	@Test
	public void testImplicitNackThenAck() throws Exception {
		RabbitTemplate template = new RabbitTemplate(this.config.connectionFactory());
		template.convertAndSend(QUEUE_WITH_DLQ, "foo");
		template.convertAndSend(QUEUE_WITH_DLQ, "nackIt");
		template.convertAndSend(DSL_QUEUE, "bar");
		template.convertAndSend(INTERCEPT_QUEUE, "baz");
		template.convertAndSend(NOAUTOACK_QUEUE, "qux");
		assertThat(this.config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.config.received).isEqualTo("foo");
		Message dead = template.receive(DLQ, 10_000);
		assertThat(dead).isNotNull();
		assertThat(this.config.fromDsl).isEqualTo("bar");
		assertThat(this.config.fromInterceptedSource).isEqualTo("BAZ");
		assertThat(template.receive(NOAUTOACK_QUEUE)).isNull();
		assertThat(this.config.requeueLatch.getCount()).isEqualTo(1L);
		this.config.callback.acknowledge(Status.REQUEUE);
		assertThat(this.config.requeueLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		private final CountDownLatch latch = new CountDownLatch(5);

		private final CountDownLatch requeueLatch = new CountDownLatch(2);

		private volatile String received;

		private volatile Object fromDsl;

		private volatile Object fromInterceptedSource;

		private volatile AcknowledgmentCallback callback;

		@InboundChannelAdapter(channel = "in", poller = @Poller(fixedDelay = "100"), autoStartup = "false")
		@Bean
		public MessageSource<?> source() {
			return new AmqpMessageSource(connectionFactory(), QUEUE_WITH_DLQ);
		}

		@ServiceActivator(inputChannel = "in")
		public void in(String in) {
			try {
				if ("nackIt".equals(in)) {
					throw new RuntimeException("testing auto nack");
				}
				else {
					this.received = in;
				}
			}
			finally {
				this.latch.countDown();
			}
		}

		@InboundChannelAdapter(channel = "noAutoAck", poller = @Poller(fixedDelay = "100"), autoStartup = "false")
		@Bean
		public MessageSource<?> noAutoAckSource() {
			return new AmqpMessageSource(connectionFactory(), NOAUTOACK_QUEUE) {

				@Override
				protected AbstractIntegrationMessageBuilder<Object> doReceive() {
					AbstractIntegrationMessageBuilder<Object> builder = super.doReceive();
					if (builder != null) {
						Config.this.requeueLatch.countDown();
					}
					return builder;
				}

			};
		}

		@ServiceActivator(inputChannel = "noAutoAck")
		public void ack(@Header(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK)
		AcknowledgmentCallback callback) {
			callback.noAutoAck();
			this.callback = callback;
			latch.countDown();
		}

		@Bean
		public IntegrationFlow flow() {
			return IntegrationFlow.from(Amqp.inboundPolledAdapter(connectionFactory(), DSL_QUEUE),
							e -> e.poller(Pollers.fixedDelay(100)).autoStartup(false))
					.handle(p -> {
						this.fromDsl = p.getPayload();
						this.latch.countDown();
					})
					.get();
		}

		@Bean
		public IntegrationFlow messageSourceChannelFlow() {
			return IntegrationFlow.from(interceptedSource(),
							e -> e.poller(Pollers.fixedDelay(100)).autoStartup(false))
					.handle(p -> {
						this.fromInterceptedSource = p.getPayload();
						this.latch.countDown();
					})
					.get();
		}

		@Bean
		public MessageSource<?> interceptedSource() {
			return new AmqpMessageSource(connectionFactory(), INTERCEPT_QUEUE);
		}

		@Bean
		public static BeanPostProcessor upcase() {
			return new BeanPostProcessor() {

				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
					if (beanName.equals("interceptedSource")) {
						ProxyFactory pf = new ProxyFactory(bean);
						pf.addAdvice((MethodInterceptor) invocation -> {

							if (invocation.getMethod().getName().equals("receive")) {
								org.springframework.messaging.Message<?> message =
										(org.springframework.messaging.Message<?>) invocation.proceed();
								if (message == null) {
									return null;
								}
								return MessageBuilder.withPayload(((String) message.getPayload()).toUpperCase())
										.copyHeaders(message.getHeaders())
										.build();
							}
							else {
								return invocation.proceed();
							}
						});
						return pf.getProxy();
					}
					return bean;
				}

			};
		}

		@Bean
		public ConnectionFactory connectionFactory() {
			return new CachingConnectionFactory("localhost");
		}

	}

}
