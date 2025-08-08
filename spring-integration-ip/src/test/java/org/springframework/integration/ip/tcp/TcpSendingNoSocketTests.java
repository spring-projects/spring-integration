/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class TcpSendingNoSocketTests {

	@Autowired
	private MessageChannel shouldFail;

	@Autowired
	private MessageChannel advised;

	@Test
	public void exceptionExpected() {
		try {
			shouldFail.send(new GenericMessage<String>("foo"));
			fail("Exception expected");
		}
		catch (MessageHandlingException e) {
			assertThat(e.getMessage()).startsWith("Unable to find outbound socket");
		}
	}

	@Test
	public void exceptionTrapped() {
		advised.send(new GenericMessage<String>("foo"));
	}

}
