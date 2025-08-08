/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.jms.request_reply;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class StopStartTests extends ActiveMQMultiContextTests {

	@Autowired
	@Qualifier("out")
	private Lifecycle outGateway;

	@Autowired
	private MessageChannel test;

	@Test
	public void test() {
		MessagingTemplate template = new MessagingTemplate(this.test);
		this.outGateway.start();
		assertThat(template.convertSendAndReceive("foo", String.class)).isEqualTo("FOO");
		this.outGateway.stop();
		this.outGateway.start();
		assertThat(template.convertSendAndReceive("bar", String.class)).isEqualTo("BAR");
		this.outGateway.stop();
	}

}
