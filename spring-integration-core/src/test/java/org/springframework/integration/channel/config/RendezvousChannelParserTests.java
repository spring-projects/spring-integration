/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel.config;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RendezvousChannelParserTests {

	@Test
	public void testRendezvous() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"rendezvousChannelParserTests.xml", RendezvousChannelParserTests.class);
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		assertThat(channel.getClass()).isEqualTo(RendezvousChannel.class);
		context.close();
	}

}
