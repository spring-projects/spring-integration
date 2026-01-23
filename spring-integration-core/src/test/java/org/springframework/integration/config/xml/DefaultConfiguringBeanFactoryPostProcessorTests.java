/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class DefaultConfiguringBeanFactoryPostProcessorTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void errorChannelRegistered() {
		Object errorChannel = context.getBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
		assertThat(errorChannel).isNotNull();
		assertThat(errorChannel.getClass()).isEqualTo(PublishSubscribeChannel.class);
	}

	@Test
	public void nullChannelRegistered() {
		Object nullChannel = context.getBean(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME);
		assertThat(nullChannel).isNotNull();
		assertThat(nullChannel.getClass()).isEqualTo(NullChannel.class);
	}

	@Test
	public void taskSchedulerRegistered() {
		Object taskScheduler = context.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME);
		assertThat(taskScheduler.getClass()).isEqualTo(ThreadPoolTaskScheduler.class);
		ErrorHandler errorHandler = TestUtils.getPropertyValue(taskScheduler, "errorHandler");
		assertThat(errorHandler.getClass()).isEqualTo(MessagePublishingErrorHandler.class);
		MessageChannel defaultErrorChannel =
				TestUtils.getPropertyValue(errorHandler, "messagingTemplate.defaultDestination");
		assertThat(defaultErrorChannel).isNull();
		errorHandler.handleError(new Throwable());
		defaultErrorChannel = TestUtils.getPropertyValue(errorHandler, "messagingTemplate.defaultDestination");
		assertThat(defaultErrorChannel).isNotNull();
		assertThat(defaultErrorChannel).isEqualTo(context.getBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME));
	}

	@Test
	public void taskSchedulerNotRegisteredMoreThanOnce() {
		ClassPathXmlApplicationContext superParentApplicationContext = new ClassPathXmlApplicationContext(
				"superParentApplicationContext.xml", this.getClass());
		ClassPathXmlApplicationContext parentApplicationContext = new ClassPathXmlApplicationContext(
				new String[] {"org/springframework/integration/config/xml/parentApplicationContext.xml"},
				superParentApplicationContext);
		ClassPathXmlApplicationContext childApplicationContext = new ClassPathXmlApplicationContext(
				new String[] {"org/springframework/integration/config/xml/childApplicationContext.xml"},
				parentApplicationContext);
		TaskScheduler parentScheduler = childApplicationContext.getParent().getBean("taskScheduler",
				TaskScheduler.class);
		TaskScheduler childScheduler = childApplicationContext.getBean("taskScheduler", TaskScheduler.class);

		assertThat(childScheduler).as("Child task scheduler was null").isNotNull();
		assertThat(parentScheduler).as("Parent task scheduler was null").isNotNull();
		assertThat(childScheduler).as("Different schedulers in parent and child").isEqualTo(parentScheduler);
		childApplicationContext.close();
		parentApplicationContext.close();
		superParentApplicationContext.close();
	}

}
