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
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
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
public class MethodInvokerTests {

	@Autowired
	private MBeanServer server;

	@Autowired
	private MessageChannel echos;

	@Autowired
	private SubscribableChannel underscores;

	@Test
	public void testHandlerMBeanRegistration() throws Exception {
		Set<ObjectName> names = server.queryNames(new ObjectName("test.MethodInvoker:type=MessageHandler,*"), null);
		// System . err.println(names);
		// the router and the error handler...
		assertThat(names.size()).isEqualTo(2);
		underscores.subscribe(message -> assertThat(message.getPayload()).isEqualTo("foo"));
		echos.send(MessageBuilder.withPayload("foo").setHeader("entity-type", "underscore").build());
	}

}
