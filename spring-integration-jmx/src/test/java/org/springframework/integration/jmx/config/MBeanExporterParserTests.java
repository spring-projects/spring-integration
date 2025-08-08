/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import java.util.Properties;

import javax.management.MBeanServer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class MBeanExporterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testMBeanExporterExists() {
		IntegrationMBeanExporter exporter = this.context.getBean(IntegrationMBeanExporter.class);
		MBeanServer server = this.context.getBean("mbs", MBeanServer.class);
		Properties properties = TestUtils.getPropertyValue(exporter, "objectNameStaticProperties", Properties.class);
		assertThat(properties).isNotNull();
		assertThat(properties.size()).isEqualTo(2);
		assertThat(properties.containsKey("foo")).isTrue();
		assertThat(properties.containsKey("bar")).isTrue();
		assertThat(exporter.getServer()).isEqualTo(server);
		assertThat(TestUtils.getPropertyValue(exporter, "namingStrategy")).isSameAs(context.getBean("keyNamer"));
		exporter.destroy();
	}

}
