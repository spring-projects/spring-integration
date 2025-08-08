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
public class PollingAdapterMBeanTests {

	@Autowired
	private MBeanServer server;

	@Test
	public void testMessageSourceMBeanExists() throws Exception {
		// System . err.println(server.queryNames(new ObjectName("*:type=MessageSource,*"), null));
		Set<ObjectName> names = server.queryNames(new ObjectName("test.PollingAdapterMBean:type=MessageSource,*"), null);
		assertThat(names.size()).isEqualTo(1);
	}

	public static class Source {

		public String get() {
			return "foo";
		}

	}

}
