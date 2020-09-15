/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.integration.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.EndpointId;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ReplyProducingMessageHandlerWrapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0.4
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class BeanNameTests {

	@SuppressWarnings("unused")
	@Autowired
	private EventDrivenConsumer eipMethod;

	@SuppressWarnings("unused")
	@Autowired
	@Qualifier("eipMethod.handler")
	private MessageHandler eipMethodHandler;

	@SuppressWarnings("unused")
	@Autowired
	private EventDrivenConsumer eipBean;

	@SuppressWarnings("unused")
	@Autowired
	@Qualifier("eipBean.handler")
	private MessageHandler eipBeanHandler;

	@SuppressWarnings("unused")
	@Autowired
	private EventDrivenConsumer eipBean2;

	@SuppressWarnings("unused")
	@Autowired
	@Qualifier("eipBean2.handler")
	private MessageHandler eipBean2Handler;

	@Autowired
	@Qualifier("eipBean2.handler.wrapper")
	private ReplyProducingMessageHandlerWrapper eipBean2HandlerWrapper;

	@SuppressWarnings("unused")
	@Autowired
	private SourcePollingChannelAdapter eipMethodSource;

	@SuppressWarnings("unused")
	@Autowired
	@Qualifier("eipMethodSource.source")
	private MessageSource<?> eipMethodSourceSource;

	@SuppressWarnings("unused")
	@Autowired
	private SourcePollingChannelAdapter eipSource;

	@SuppressWarnings("unused")
	@Autowired
	@Qualifier("eipSource.source")
	private MessageSource<?> eipSourceSource;

	@Test
	public void contextLoads() {
		assertThat(this.eipBean2HandlerWrapper.getComponentName()).isEqualTo("eipBean2");
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationManagement
	public static class Config {

		@Bean
		public static MeterRegistry meterRegistry() {
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

		@Bean("eipBean2.handler")
		@EndpointId("eipBean2")
		@ServiceActivator(inputChannel = "channel3")
		public MessageHandler handler() {
			return m -> { };
		}

		@EndpointId("eipMethodSource")
		@InboundChannelAdapter(channel = "channel3", poller = @Poller(fixedDelay = "5000"))
		public String pojoSource() {
			return null;
		}

		@Bean("eipSource.source")
		@EndpointId("eipSource")
		@InboundChannelAdapter(channel = "channel3", poller = @Poller(fixedDelay = "5000"))
		public MessageSource<?> source() {
			return () -> null;
		}

	}

}
