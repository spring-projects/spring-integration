/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jms.request_reply;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.condition.LongRunningTest;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.Message;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
@LongRunningTest
public class RequestReplyScenariosWithCorrelationKeyProvidedTests extends ActiveMQMultiContextTests {

	@Test
	public void messageCorrelationBasedCustomCorrelationKey() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("explicit-correlation-key.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean("explicitCorrelationKeyGateway", RequestReplyExchanger.class);

		gateway.exchange(MessageBuilder.withPayload("foo").build());
		context.close();
	}

	@Test
	public void messageCorrelationBasedCustomCorrelationKeyAsJMSCorrelationID() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("explicit-correlation-key.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean("explicitCorrelationKeyGatewayB", RequestReplyExchanger.class);

		gateway.exchange(MessageBuilder.withPayload("foo").build());
		context.close();
	}

	@Test
	public void messageCorrelationBasedOnProvidedJMSCorrelationID() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("explicit-correlation-key.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean("existingCorrelationKeyGatewayB", RequestReplyExchanger.class);

		String correlationId = UUID.randomUUID().toString().replaceAll("'", "''");
		Message<?> result = gateway.exchange(MessageBuilder.withPayload("foo")
				.setHeader(JmsHeaders.CORRELATION_ID, correlationId)
				.build());
		assertThat(result.getHeaders().get("receivedCorrelationId")).isEqualTo(correlationId);
		context.close();
	}

	@Test
	public void messageCorrelationBasedCustomCorrelationKeyDelayedReplies() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("explicit-correlation-key.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean("explicitCorrelationKeyGatewayC", RequestReplyExchanger.class);


		for (int i = 0; i < 3; i++) {
			try {
				gateway.exchange(MessageBuilder.withPayload("hello").build());
			}
			catch (Exception e) {
				// ignore
			}
		}

		JmsOutboundGateway outGateway =
				TestUtils.getPropertyValue(context.getBean("outGateway"), "handler", JmsOutboundGateway.class);
		outGateway.setReceiveTimeout(5000);
		assertThat(gateway.exchange(MessageBuilder.withPayload("foo").build()).getPayload()).isEqualTo("foo");
		context.close();
	}


	public static class DelayedService {

		public String echo(String s) throws Exception {
			Thread.sleep(200);
			return s;
		}

	}

}
