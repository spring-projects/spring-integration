/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.integration.handler;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableMessageHistory;
import org.springframework.integration.history.MessageHistory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3.10
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class CGLibProxyHandlerTests {

	@Autowired
	private BridgeHandler bridge;

	@Autowired
	private QueueChannel out;

	@Test
	public void testProxyBridge() {
		assertThat(AopUtils.isCglibProxy(this.bridge)).isTrue();
		this.bridge.handleMessage(new GenericMessage<>("foo"));
		Message<?> received = this.out.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(MessageHistory.HEADER_NAME)).isNotNull();
		MessageHistory history = received.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class);
		assertThat(history.size()).isEqualTo(2);
	}

	@Configuration
	@EnableMessageHistory
	public static class Config {

		@Bean
		public BridgeHandler bridgeTarget() { // separate bean so setters and aPS are called
			BridgeHandler bridgeHandler = new BridgeHandler();
			bridgeHandler.setOutputChannel(out());
			bridgeHandler.setShouldTrack(true);
			return bridgeHandler;
		}

		@Bean
		public ProxyFactoryBean bridge() {
			return createProxyFactory(bridgeTarget());
		}

		@Bean
		public QueueChannel out() {
			return new QueueChannel();
		}

		private ProxyFactoryBean createProxyFactory(MessageHandler handler) {
			ProxyFactoryBean fb = new ProxyFactoryBean();
			fb.setProxyTargetClass(true);
			fb.setTarget(handler);
			return fb;
		}

	}

}
