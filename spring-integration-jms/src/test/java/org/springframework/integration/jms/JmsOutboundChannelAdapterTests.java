/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.integration.jms;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class JmsOutboundChannelAdapterTests extends ActiveMQMultiContextTests {

	@Autowired
	private Aborter aborter;

	@Autowired
	private JmsMessageDrivenEndpoint endpoint;

	@Test
	public void testTransactionalSend() throws JMSException {
		JmsTemplate template = new JmsTemplate(connectionFactory);
		template.send("outcatQ1",
				session -> {
					TextMessage textMessage =
							session.createTextMessage("Hello, world!");
					textMessage.setObjectProperty("bytesProperty", "testValue".getBytes());
					return textMessage;
				});
		template.setReceiveTimeout(20000);
		Message receive = template.receive("outcatQ2");
		assertThat(receive).isNotNull();
		assertThat(receive.getObjectProperty("bytesProperty")).isEqualTo("testValue".getBytes());

		this.aborter.abort = true;
		template.convertAndSend("outcatQ1", "Hello, world!");
		template.setReceiveTimeout(1000);
		assertThat(template.receive("outcatQ2")).isNull();
		endpoint.stop();
	}

	public static class Aborter {

		private volatile boolean abort;

		public void foo() {
			if (abort) {
				throw new RuntimeException("intentional");
			}
		}

	}

}
