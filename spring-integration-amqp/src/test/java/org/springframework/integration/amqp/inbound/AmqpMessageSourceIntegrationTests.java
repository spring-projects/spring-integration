/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.integration.amqp.inbound;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.BrokerRunning;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gary Russell
 * @since 5.0.1
 *
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class AmqpMessageSourceIntegrationTests {

	private static final String DSL_QUEUE = "AmqpMessageSourceIntegrationTests";

	private static final String QUEUE_WITH_DLQ = "AmqpMessageSourceIntegrationTests.withDLQ";

	private static final String DLQ = QUEUE_WITH_DLQ + ".dlq";

	private static final String INTERCEPT_QUEUE = "AmqpMessageSourceIntegrationTests.channel";

	@ClassRule
	public static BrokerRunning brokerRunning = BrokerRunning.isRunningWithEmptyQueues(DSL_QUEUE, INTERCEPT_QUEUE, DLQ);

	@Autowired
	private Config config;

	@Before
	public void before() {
		RabbitAdmin admin = new RabbitAdmin(this.config.connectionFactory());
		Queue queue = QueueBuilder.nonDurable(QUEUE_WITH_DLQ)
				.autoDelete()
				.withArgument("x-dead-letter-exchange", "")
				.withArgument("x-dead-letter-routing-key", DLQ)
				.build();
		admin.declareQueue(queue);
	}

	@AfterClass
	public static void afterClass() {
		brokerRunning.removeTestQueues(QUEUE_WITH_DLQ);
	}

	@Test
	public void testImplicitNackThenAck() throws Exception {
		RabbitTemplate template = new RabbitTemplate(this.config.connectionFactory());
		template.convertAndSend(QUEUE_WITH_DLQ, "foo");
		template.convertAndSend(QUEUE_WITH_DLQ, "nackIt");
		template.convertAndSend(DSL_QUEUE, "bar");
		template.convertAndSend(INTERCEPT_QUEUE, "baz");
		assertTrue(this.config.latch.await(10, TimeUnit.SECONDS));
		assertThat(this.config.received, equalTo("foo"));
		Message dead = template.receive(DLQ, 10_000);
		assertNotNull(dead);
		assertThat(this.config.fromDsl, equalTo("bar"));
		assertThat(this.config.fromInterceptedSource, equalTo("BAZ"));
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		private final CountDownLatch latch = new CountDownLatch(4);

		private String received;

		private Object fromDsl;

		private Object fromInterceptedSource;

		@InboundChannelAdapter(channel = "in", poller = @Poller(fixedDelay = "100"))
		@Bean
		public MessageSource<?> source() {
			return new AmqpMessageSource(connectionFactory(), QUEUE_WITH_DLQ);
		}

		@ServiceActivator(inputChannel = "in")
		public void in(String in) {
			this.latch.countDown();
			if ("nackIt".equals(in)) {
				throw new RuntimeException("testing auto nack");
			}
			else {
				this.received = in;
			}
		}

		@Bean
		public IntegrationFlow flow() {
			return IntegrationFlows.from(Amqp.inboundPolledAdapter(connectionFactory(), DSL_QUEUE),
							e -> e.poller(Pollers.fixedDelay(100)))
					.handle(p -> {
						this.fromDsl = p.getPayload();
						this.latch.countDown();
					})
					.get();
		}

		@Bean
		public IntegrationFlow messageSourceChannelFlow() {
			return IntegrationFlows.from(interceptedSource(),
							e -> e.poller(Pollers.fixedDelay(100)))
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
						MethodInterceptor interceptor = new MethodInterceptor() {

							@Override
							public Object invoke(MethodInvocation invocation) throws Throwable {
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
							}

						};
						pf.addAdvice(interceptor);
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
