/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.scripting.config.jsr223;

import java.util.Date;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class Jsr223InboundChannelAdapterTests {

	@Autowired
	@Qualifier("inbound-channel-adapter-channel")
	private PollableChannel inboundChannelAdapterChannel;

	@Test
	public void testInt2867InboundChannelAdapter() throws Exception {
		Message<?> message = this.inboundChannelAdapterChannel.receive(20000);
		assertThat(message).isNotNull();
		Object payload = message.getPayload();
		Thread.sleep(2);
		assertThat(payload).isInstanceOf(Date.class);
		assertThat(((Date) payload).before(new Date())).isTrue();
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");

		message = this.inboundChannelAdapterChannel.receive(20000);
		assertThat(message).isNotNull();
	}

}
