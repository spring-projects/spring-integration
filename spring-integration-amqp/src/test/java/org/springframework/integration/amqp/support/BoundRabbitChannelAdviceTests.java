/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.integration.amqp.support;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.1
 *
 */
@SpringJUnitConfig
public class BoundRabbitChannelAdviceTests {

	@Autowired
	private Config.Gate gate;

	@Autowired
	private Config config;

	@Test
	void testAdvice() throws Exception {
		this.gate.send("a,b,c");
		verify(this.config.connection, times(1)).createChannel();
		verify(this.config.channel).confirmSelect();
		verify(this.config.channel).basicPublish(eq(""), eq("rk"), anyBoolean(), any(), eq("A".getBytes()));
		verify(this.config.channel).basicPublish(eq(""), eq("rk"), anyBoolean(), any(), eq("B".getBytes()));
		verify(this.config.channel).basicPublish(eq(""), eq("rk"), anyBoolean(), any(), eq("C".getBytes()));
		verify(this.config.channel).waitForConfirmsOrDie(10_000L);
	}

	@Test
	void validate() {
		RabbitOperations template = mock(RabbitOperations.class);
		given(template.getConnectionFactory()).willReturn(
				mock(org.springframework.amqp.rabbit.connection.ConnectionFactory.class));
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BoundRabbitChannelAdvice(template, Duration.ofSeconds(1)));
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		private Connection connection;

		private Channel channel;

		@Bean
		public CachingConnectionFactory cf() throws Exception {
			ConnectionFactory cf = mock(ConnectionFactory.class);
			cf.setHost("localhost");
			cf = spy(cf);
			willAnswer(i -> {
				this.connection = mock(Connection.class);
				willAnswer(ii -> {
					this.channel = mock(Channel.class);
					given(this.channel.isOpen()).willReturn(true);
					return this.channel;
				}).given(this.connection).createChannel();
				return this.connection;
			}).given(cf).newConnection((ExecutorService) isNull(), anyString());
			cf.setAutomaticRecoveryEnabled(false);
			CachingConnectionFactory ccf = new CachingConnectionFactory(cf);
			ccf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.SIMPLE);
			return ccf;
		}

		@Bean
		public RabbitTemplate template() throws Exception {
			return new RabbitTemplate(cf());
		}

		@Bean
		public IntegrationFlow flow(RabbitTemplate template) {
			return IntegrationFlows.from(Gate.class)
					.split(s -> s.delimiters(",")
							.advice(new BoundRabbitChannelAdvice(template, Duration.ofSeconds(10))))
					.<String, String>transform(String::toUpperCase)
					.handle(Amqp.outboundAdapter(template).routingKey("rk"))
					.get();
		}

		public interface Gate {

			void send(String out);

		}

	}

}
