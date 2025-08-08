/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.http.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @since 1.0.3
 */
@SpringJUnitConfig
@DirtiesContext
public class DefaultConfigurationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void verifyErrorChannel() {
		Object errorChannel = context.getBean("errorChannel");
		assertThat(errorChannel).isNotNull();
		assertThat(errorChannel.getClass()).isEqualTo(PublishSubscribeChannel.class);
	}

	@Test
	public void verifyNullChannel() {
		Object nullChannel = context.getBean("nullChannel");
		assertThat(nullChannel).isNotNull();
		assertThat(nullChannel.getClass()).isEqualTo(NullChannel.class);
	}

	@Test
	public void verifyTaskScheduler() {
		Object taskScheduler = context.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME);
		assertThat(taskScheduler.getClass()).isEqualTo(ThreadPoolTaskScheduler.class);
		ErrorHandler errorHandler = TestUtils.getPropertyValue(taskScheduler, "errorHandler", ErrorHandler.class);
		assertThat(errorHandler.getClass()).isEqualTo(MessagePublishingErrorHandler.class);
		MessageChannel defaultErrorChannel = TestUtils.getPropertyValue(errorHandler,
				"messagingTemplate.defaultDestination", MessageChannel.class);
		assertThat(defaultErrorChannel).isNull();
		errorHandler.handleError(new Throwable());
		defaultErrorChannel = TestUtils.getPropertyValue(errorHandler, "messagingTemplate.defaultDestination",
				MessageChannel.class);
		assertThat(defaultErrorChannel).isNotNull();
		assertThat(defaultErrorChannel).isEqualTo(context.getBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME));
	}

}
