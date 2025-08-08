/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.request_reply;

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

import static org.assertj.core.api.Assertions.assertThat;

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
