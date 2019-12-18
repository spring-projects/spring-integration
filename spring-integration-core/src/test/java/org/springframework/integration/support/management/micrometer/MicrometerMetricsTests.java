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

package org.springframework.integration.support.management.micrometer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.EndpointId;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.AbstractPollableChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
public class MicrometerMetricsTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private MeterRegistry meterRegistry;

	@Autowired
	private AbstractMessageChannel channel;

	@Autowired
	private AbstractMessageChannel channel2;

	@Autowired
	private MessageSource<?> source;

	@Autowired
	private QueueChannel queue;

	@Autowired
	private PollableChannel badPoll;

	@Autowired
	private NullChannel nullChannel;

	@SuppressWarnings("unchecked")
	@Test
	public void testSend() {
		GenericMessage<String> message = new GenericMessage<>("foo");
		this.channel.send(message);
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.channel.send(this.source.receive())) // "bar"
				.withStackTraceContaining("testErrorCount");

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.channel.send(new GenericMessage<>("bar")))
				.withStackTraceContaining("testErrorCount");

		assertThat(TestUtils.getPropertyValue(this.channel, "meters", Set.class)).hasSize(2);
		this.channel2.send(message);
		this.queue.send(message);
		this.queue.send(message);
		this.queue.receive();
		this.badPoll.send(message);

		assertThatExceptionOfType(RuntimeException.class)
				.isThrownBy(() -> this.badPoll.receive())
				.withMessage("badPoll");

		nullChannel.send(message);
		MeterRegistry registry = this.meterRegistry;
		assertThat(registry.get("spring.integration.channels").gauge().value()).isEqualTo(6);
		assertThat(registry.get("spring.integration.handlers").gauge().value()).isEqualTo(3);
		assertThat(registry.get("spring.integration.sources").gauge().value()).isEqualTo(1);

		assertThat(registry.get("spring.integration.receive")
				.tag("name", "source")
				.tag("result", "success")
				.counter().count()).isEqualTo(1);

		assertThat(registry.get("spring.integration.receive")
				.tag("name", "badPoll")
				.tag("result", "failure")
				.counter().count()).isEqualTo(1);

		assertThat(registry.get("spring.integration.send")
				.tag("name", "eipBean.handler")
				.tag("result", "success")
				.timer().count()).isEqualTo(1);

		assertThat(registry.get("spring.integration.send")
				.tag("name", "eipMethod")
				.tag("result", "success")
				.timer().count()).isEqualTo(1);

		assertThat(registry.get("spring.integration.send")
				.tag("name", "channel")
				.tag("result", "success")
				.timer().count()).isEqualTo(1);

		assertThat(registry.get("spring.integration.send")
				.tag("name", "channel")
				.tag("result", "failure")
				.timer().count()).isEqualTo(2);

		assertThat(registry.get("spring.integration.send")
				.tag("name", "eipMethod")
				.tag("result", "failure")
				.timer().count()).isEqualTo(2);

		assertThat(registry.get("spring.integration.receive")
				.tag("name", "queue")
				.tag("result", "success")
				.counter().count()).isEqualTo(1);

		assertThat(registry.get("spring.integration.send")
				.tag("name", "nullChannel")
				.tag("result", "success")
				.timer().count()).isEqualTo(1);

		BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) this.context.getBeanFactory();
		beanFactory.registerBeanDefinition("newChannel",
				BeanDefinitionBuilder.genericBeanDefinition(DirectChannel.class).getRawBeanDefinition());
		DirectChannel newChannel = this.context.getBean("newChannel", DirectChannel.class);
		newChannel.setBeanName("newChannel");
		newChannel.subscribe(m -> { });
		newChannel.send(new GenericMessage<>("foo"));
		assertThat(registry.get("spring.integration.send")
				.tag("name", "newChannel")
				.tag("result", "success")
				.timer().count()).isEqualTo(1);

		// Test meter removal
		registry.get("spring.integration.send")
				.tag("name", "newChannel")
				.tag("result", "success")
				.timer();
		newChannel.destroy();

		assertThatExceptionOfType(MeterNotFoundException.class)
				.isThrownBy(() ->
						registry.get("spring.integration.send")
								.tag("name", "newChannel")
								.tag("result", "success")
								.timer())
				.withStackTraceContaining("A meter with name 'spring.integration.send' was found")
				.withStackTraceContaining("No meters have a tag 'name' with value 'newChannel'");

		this.context.close();

		assertThatExceptionOfType(MeterNotFoundException.class)
				.isThrownBy(() -> registry.get("spring.integration.send").timers())
				.withStackTraceContaining("No meter with name 'spring.integration.send' was found");

		assertThatExceptionOfType(MeterNotFoundException.class)
				.isThrownBy(() -> registry.get("spring.integration.receive").counters())
				.withStackTraceContaining("No meter with name 'spring.integration.receive' was found");

		this.channel.destroy();
		assertThat(TestUtils.getPropertyValue(this.channel, "meters", Set.class)).hasSize(0);

	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationManagement
	public static class Config {

		@Bean
		public MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

		@ServiceActivator(inputChannel = "channel")
		@EndpointId("eipMethod")
		public void service(String in) {
			if ("bar".equals(in)) {
				throw new RuntimeException("testErrorCount");
			}
		}

		@Bean("eipBean.handler")
		@EndpointId("eipBean")
		@ServiceActivator(inputChannel = "channel2")
		public MessageHandler replyingHandler() {
			return new AbstractReplyProducingMessageHandler() {

				@Override
				protected Object handleRequestMessage(Message<?> requestMessage) {
					return null;
				}

			};
		}

		@Bean
		public MessageSource<?> source() {
			return new AbstractMessageSource<String>() {

				@Override
				public String getComponentType() {
					return "testSource";
				}

				@Override
				protected Object doReceive() {
					return "bar";
				}
			};
		}

		@Bean
		public QueueChannel queue() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel badPoll() {
			return new AbstractPollableChannel() {

				@Override
				protected boolean doSend(Message<?> message, long timeout) {
					return true;
				}

				@Override
				protected Message<?> doReceive(long timeout) {
					throw new RuntimeException("badPoll");
				}
			};
		}

	}

}
