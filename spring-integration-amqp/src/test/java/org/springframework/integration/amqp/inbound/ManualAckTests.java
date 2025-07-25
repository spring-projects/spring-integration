/*
 * Copyright 2014-present the original author or authors.
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

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.support.RabbitTestContainer;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class ManualAckTests implements RabbitTestContainer {

	@Autowired
	private MessageChannel foo;

	@Autowired
	private PollableChannel bar;

	@Autowired
	private SimpleMessageListenerContainer container;

	@Autowired
	private RabbitTemplate template;

	@Test
	public void testManual() {
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(this.container);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.setOutputChannel(foo);
		adapter.afterPropertiesSet();
		adapter.start();
		this.template.convertAndSend("Hello, world");
		Message<?> out = bar.receive(5000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo(1);
		out = bar.receive(5000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo(2);
		out = bar.receive(5000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo(3);
		out = bar.receive(1000);
		assertThat(out).isNull();
		adapter.stop();
	}

	@Configuration
	@EnableIntegration
	public static class ManualAckConfig {

		private int called;

		@ServiceActivator(inputChannel = "foo", outputChannel = "bar")
		public Integer handle(@Payload String payload, @Header(AmqpHeaders.CHANNEL) Channel channel,
				@Header(AmqpHeaders.DELIVERY_TAG) Long deliveryTag) throws Exception {
			if (++called > 2) {
				channel.basicAck(deliveryTag, false);
			}
			else {
				channel.basicNack(deliveryTag, false, true);
			}
			return called;
		}

		@Bean
		public QueueChannel bar() {
			return new QueueChannel();
		}

		@Bean
		public CachingConnectionFactory connectionFactory() {
			CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
			connectionFactory.setPort(RabbitTestContainer.amqpPort());
			return connectionFactory;
		}

		@Bean
		public Queue queue() {
			return new AnonymousQueue();
		}

		@Bean
		public SimpleMessageListenerContainer container() {
			SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory());
			container.setQueues(queue());
			container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
			container.setAutoStartup(false);
			return container;
		}

		@Bean
		public RabbitTemplate template() {
			RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
			rabbitTemplate.setRoutingKey(queue().getName());
			return rabbitTemplate;
		}

		@Bean
		public RabbitAdmin admin() {
			return new RabbitAdmin(connectionFactory());
		}

	}

}
