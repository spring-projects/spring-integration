/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class ControlBusParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testControlMessageToChannelMetrics() {
		MessageChannel control = this.context.getBean("controlChannel", MessageChannel.class);
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		Object value = messagingTemplate.convertSendAndReceive(control, "@cb.isRunning()", Object.class);
		assertThat(value).isEqualTo(Boolean.TRUE);
	}

}
