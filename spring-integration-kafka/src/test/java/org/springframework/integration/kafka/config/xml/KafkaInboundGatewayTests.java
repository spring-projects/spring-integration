/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.kafka.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.kafka.inbound.KafkaInboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class KafkaInboundGatewayTests {

	@Autowired
	private KafkaInboundGateway<?, ?, ?> gateway1;

	@Autowired
	private ApplicationContext context;

	@Test
	public void testProps() {
		assertThat(this.gateway1.isAutoStartup()).isFalse();
		assertThat(this.gateway1.isRunning()).isFalse();
		assertThat(this.gateway1.getPhase()).isEqualTo(100);
		assertThat(TestUtils.getPropertyValue(this.gateway1, "requestChannelName")).isEqualTo("nullChannel");
		assertThat(TestUtils.getPropertyValue(this.gateway1, "replyChannelName")).isEqualTo("errorChannel");
		KafkaMessageListenerContainer<?, ?> container =
				TestUtils.getPropertyValue(this.gateway1, "messageListenerContainer",
						KafkaMessageListenerContainer.class);
		assertThat(container).isNotNull();
		assertThat(TestUtils.getPropertyValue(this.gateway1, "listener.fallbackType"))
				.isEqualTo(String.class);
		assertThat(TestUtils.getPropertyValue(this.gateway1, "errorMessageStrategy"))
			.isSameAs(this.context.getBean("ems"));
		assertThat(TestUtils.getPropertyValue(this.gateway1, "retryTemplate"))
			.isSameAs(this.context.getBean("retryTemplate"));
		assertThat(TestUtils.getPropertyValue(this.gateway1, "recoveryCallback"))
			.isSameAs(this.context.getBean("recoveryCallback"));
		assertThat(TestUtils.getPropertyValue(this.gateway1, "messagingTemplate.sendTimeout")).isEqualTo(5000L);
		assertThat(TestUtils.getPropertyValue(this.gateway1, "replyTimeout")).isEqualTo(43L);
	}

}
