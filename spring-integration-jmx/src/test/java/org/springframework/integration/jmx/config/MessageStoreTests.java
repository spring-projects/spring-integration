/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class MessageStoreTests {

	@Autowired
	private MBeanServer server;

	@Test
	public void testHandlerMBeanRegistration() throws Exception {
		Set<ObjectName> names = server.queryNames(new ObjectName("test.MessageStore:type=SimpleMessageStore,*"), null);
		assertThat(names.size()).isEqualTo(1);
		ObjectName name = names.iterator().next();
		assertThat(server.getAttribute(name, "MessageCount")).isEqualTo(0L);
		assertThat(server.getAttribute(name, "MessageGroupCount")).isEqualTo(0);
		assertThat(server.getAttribute(name, "MessageCountForAllMessageGroups")).isEqualTo(0);
	}

}
