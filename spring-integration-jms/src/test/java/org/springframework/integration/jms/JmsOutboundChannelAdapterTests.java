/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.jms;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

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
