/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.outbound;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class TcpSendingNoSocketTests {

	@Autowired
	private MessageChannel shouldFail;

	@Autowired
	private MessageChannel advised;

	@Autowired
	private AbstractServerConnectionFactory mockCf;

	@BeforeEach
	void setup() {
		given(mockCf.getApplicationEventPublisher()).willReturn(event -> {
		});
	}

	@Test
	public void exceptionExpected() {
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> shouldFail.send(new GenericMessage<>("foo")))
				.withMessageStartingWith("Unable to find outbound socket");
	}

	@Test
	public void exceptionTrapped() {
		advised.send(new GenericMessage<>("foo"));
	}

}
