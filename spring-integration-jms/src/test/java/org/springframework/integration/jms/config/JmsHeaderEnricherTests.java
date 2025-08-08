/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import jakarta.jms.Destination;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.jms.StubTextMessage;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class JmsHeaderEnricherTests {

	@Autowired
	private MessageChannel valueTestInput;

	@Autowired
	private MessageChannel expressionTestInput;

	@Autowired
	private PollableChannel output;

	@Autowired
	private Destination testDestination;

	@Test // INT-804
	public void verifyReplyToValue() throws Exception {
		this.valueTestInput.send(new GenericMessage<>("test"));
		Message<?> result = output.receive(10_000);
		assertThat(result.getHeaders().get(JmsHeaders.REPLY_TO)).isEqualTo(this.testDestination);
		StubTextMessage jmsMessage = new StubTextMessage();
		DefaultJmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();
		headerMapper.fromHeaders(result.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSReplyTo()).isEqualTo(this.testDestination);
	}

	@Test
	public void verifyCorrelationIdValue() throws Exception {
		this.valueTestInput.send(new GenericMessage<>("test"));
		Message<?> result = output.receive(10_000);
		assertThat(result.getHeaders().get(JmsHeaders.CORRELATION_ID)).isEqualTo("ABC");
		StubTextMessage jmsMessage = new StubTextMessage();
		DefaultJmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();
		headerMapper.fromHeaders(result.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSCorrelationID()).isEqualTo("ABC");
	}

	@Test // see INT-1122 and INT-1123
	public void verifyCorrelationIdExpression() throws Exception {
		this.expressionTestInput.send(new GenericMessage<>("test"));
		Message<?> result = output.receive(0);
		assertThat(result.getHeaders().get(JmsHeaders.CORRELATION_ID)).isEqualTo(123);
		StubTextMessage jmsMessage = new StubTextMessage();
		DefaultJmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();
		headerMapper.fromHeaders(result.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSCorrelationID()).isEqualTo("123");
	}

}
