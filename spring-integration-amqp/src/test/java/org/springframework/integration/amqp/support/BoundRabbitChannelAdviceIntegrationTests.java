/*
 * Copyright 2018-2024 the original author or authors.
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.RabbitAvailable;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.spy;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
@RabbitAvailable(queues = BoundRabbitChannelAdviceIntegrationTests.QUEUE)
public class BoundRabbitChannelAdviceIntegrationTests {

	static final String QUEUE = "dedicated.advice";

	@Autowired
	private Config.Gate gate;

	@Autowired
	private Config config;

	@Test
	void testAdvice() throws Exception {
		BoundRabbitChannelAdvice advice = this.config.advice(this.config.template());
		Log logger = spy(TestUtils.getPropertyValue(advice, "logger", Log.class));
		new DirectFieldAccessor(advice).setPropertyValue("logger", logger);
		willReturn(true).given(logger).isDebugEnabled();
		final CountDownLatch latch = new CountDownLatch(1);
		willAnswer(i -> {
			latch.countDown();
			return i.callRealMethod();
		}).given(logger).debug(anyString());
		this.gate.send("a,b,c");
		assertThat(this.config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.config.received).containsExactly("A", "B", "C");
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		private final CountDownLatch latch = new CountDownLatch(3);

		private final List<String> received = new ArrayList<>();

		@Bean
		public CachingConnectionFactory cf() {
			CachingConnectionFactory ccf = new CachingConnectionFactory("localhost");
			ccf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.SIMPLE);
			return ccf;
		}

		@Bean
		public RabbitTemplate template() {
			return new RabbitTemplate(cf());
		}

		@Bean
		public BoundRabbitChannelAdvice advice(RabbitTemplate template) {
			return new BoundRabbitChannelAdvice(template, Duration.ofSeconds(10));
		}

		@Bean
		public IntegrationFlow flow(RabbitTemplate template, BoundRabbitChannelAdvice advice) {
			return IntegrationFlow.from(Gate.class)
					.splitWith(s -> s.delimiters(",").advice(advice))
					.<String, String>transform(String::toUpperCase)
					.handle(Amqp.outboundAdapter(template).routingKey(QUEUE))
					.get();
		}

		@Bean
		public IntegrationFlow listener(CachingConnectionFactory ccf) {
			return IntegrationFlow.from(Amqp.inboundAdapter(ccf, QUEUE))
					.handle(m -> {
						received.add((String) m.getPayload());
						this.latch.countDown();
					})
					.get();
		}

		public interface Gate {

			void send(String out);

		}

	}

}
