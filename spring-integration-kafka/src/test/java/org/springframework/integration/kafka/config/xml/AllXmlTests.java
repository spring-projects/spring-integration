/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.kafka.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class AllXmlTests {

	@Autowired
	private KafkaTemplate<String, String> template;

	@Autowired
	private PollableChannel lastChannel;

	@Test
	public void testEndToEnd() {
		this.template.send("one", "foo");
		Message<?> received = this.lastChannel.receive(30_000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("fooonetwothreefour");
	}

}
