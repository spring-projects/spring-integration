/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannelOperations;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class PriorityChannelTests {

	@Autowired
	private MBeanServer server;

	@Autowired
	private PollableChannel testChannel;

	@Test
	public void testHandlerMBeanRegistration() throws Exception {
		Set<ObjectName> names = server.queryNames(new ObjectName("test.PriorityChannel:type=MessageHandler,*"), null);
		// Error handler plus the service activator
		assertThat(names.size()).isEqualTo(2);
	}

	@Test
	public void testChannelMBeanRegistration() throws Exception {
		Set<ObjectName> names = server.queryNames(new ObjectName("test.PriorityChannel:type=MessageChannel,name=testChannel,*"), null);
		assertThat(names.size()).isEqualTo(1);
		assertThat(server.getAttribute(names.iterator().next(), "QueueSize")).isEqualTo(0);
		assertThat(((QueueChannelOperations) testChannel).getQueueSize()).isEqualTo(0);
	}

	public static class Source {

		public String get() {
			return "foo";
		}

	}

}
