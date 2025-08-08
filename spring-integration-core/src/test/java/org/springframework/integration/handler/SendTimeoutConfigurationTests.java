/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class SendTimeoutConfigurationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void serviceActivator() {
		assertThat(getTimeout("serviceActivator")).isEqualTo(123);
	}

	@Test
	public void filter() {
		assertThat(getTimeout("filter")).isEqualTo(123);
	}

	@Test
	public void transformer() {
		assertThat(getTimeout("transformer")).isEqualTo(123);
	}

	@Test
	public void splitter() {
		assertThat(getTimeout("splitter")).isEqualTo(123);
	}

	@Test
	public void router() {
		assertThat(getTimeout("router")).isEqualTo(123);
	}

	private long getTimeout(String endpointName) {
		return TestUtils.getPropertyValue(this.context.getBean(endpointName),
				"handler.messagingTemplate.sendTimeout", Long.class);
	}

}
