/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.rmi.config;

import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
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
