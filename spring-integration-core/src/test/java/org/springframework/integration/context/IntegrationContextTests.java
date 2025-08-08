/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 3.0
 */
@SpringJUnitConfig
@DirtiesContext
public class IntegrationContextTests {

	@Autowired
	@Qualifier(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME)
	private IntegrationProperties integrationProperties;

	@Autowired
	@Qualifier("fooService")
	private AbstractEndpoint serviceActivator;

	@Autowired
	@Qualifier("fooServiceExplicit")
	private AbstractEndpoint serviceActivatorExplicit;

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	@Test
	public void testIntegrationContextComponents() {
		assertThat(this.integrationProperties.isMessagingTemplateThrowExceptionOnLateReply()).isTrue();
		assertThat(this.integrationProperties.getTaskSchedulerPoolSize()).isEqualTo(20);
		assertThat(this.serviceActivator.getIntegrationProperties()).isSameAs(this.integrationProperties);
		assertThat(TestUtils.getPropertyValue(this.taskScheduler, "poolSize")).isEqualTo(20);
		assertThat(this.serviceActivator.isAutoStartup()).isFalse();
		assertThat(this.serviceActivator.isRunning()).isFalse();
		assertThat(this.serviceActivatorExplicit.isAutoStartup()).isTrue();
		assertThat(this.serviceActivatorExplicit.isRunning()).isTrue();
	}

}
