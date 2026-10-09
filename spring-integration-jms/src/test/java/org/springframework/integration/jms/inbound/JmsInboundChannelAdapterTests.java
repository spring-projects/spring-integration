/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.jms.inbound;

import org.junitpioneer.jupiter.RetryingTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.PollableChannel;
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
public class JmsInboundChannelAdapterTests extends ActiveMQMultiContextTests {

	@Autowired
	private PollableChannel out;

	@RetryingTest(10)
	public void testTransactionalReceive() {
		JmsTemplate template = new JmsTemplate(connectionFactory);
		template.convertAndSend("incatQ", "bar");
		assertThat(out.receive(20000)).isNotNull();
		/*
		 *  INT-3288 - previously acknowledge="transacted"
		 *  Caused by: jakarta.jms.JMSException: acknowledgeMode SESSION_TRANSACTED cannot be used for a non-transacted Session
		 */
	}

}
