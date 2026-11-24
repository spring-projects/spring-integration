/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
