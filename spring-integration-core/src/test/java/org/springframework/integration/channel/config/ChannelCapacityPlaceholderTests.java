/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel.config;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ChannelCapacityPlaceholderTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void verifyCapacityValueChanges() {
		QueueChannel channel = context.getBean("channel", QueueChannel.class);
		assertThat(channel).isNotNull();
		assertThat(channel.getRemainingCapacity()).isEqualTo(99);
		channel.send(MessageBuilder.withPayload("test1").build());
		channel.send(MessageBuilder.withPayload("test2").build());
		assertThat(channel.getRemainingCapacity()).isEqualTo(97);
		assertThat(channel.receive(0)).isNotNull();
		assertThat(channel.getRemainingCapacity()).isEqualTo(98);
	}

	@Test
	public void testCapacityOnPriorityChannel() {
		PriorityChannel channel = context.getBean("priorityChannel", PriorityChannel.class);
		assertThat(channel).isNotNull();
		assertThat(channel.getRemainingCapacity()).isEqualTo(99);
		channel.send(MessageBuilder.withPayload("test1").build());
		channel.send(MessageBuilder.withPayload("test2").build());
		assertThat(channel.getRemainingCapacity()).isEqualTo(97);
		assertThat(channel.receive(0)).isNotNull();
		assertThat(channel.getRemainingCapacity()).isEqualTo(98);
	}

}
