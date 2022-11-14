/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.config;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Gary Russell
 */
public class MessageBusParserTests {

	@Test
	public void testErrorChannelReference() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithErrorChannel.xml", this.getClass());
		BeanFactoryChannelResolver resolver = new BeanFactoryChannelResolver(context);
		assertThat(resolver.resolveDestination("errorChannel")).isEqualTo(context.getBean("errorChannel"));
		context.close();
	}

	@Test
	public void testDefaultErrorChannel() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaults.xml", this.getClass());
		BeanFactoryChannelResolver resolver = new BeanFactoryChannelResolver(context);
		assertThat(resolver.resolveDestination("errorChannel")).isEqualTo(context.getBean("errorChannel"));
		context.close();
	}

	@Test
	public void testMulticasterIsSyncByDefault() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaults.xml", this.getClass());
		SimpleApplicationEventMulticaster multicaster = (SimpleApplicationEventMulticaster)
				context.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
		DirectFieldAccessor accessor = new DirectFieldAccessor(multicaster);
		Object taskExecutor = accessor.getPropertyValue("taskExecutor");
		if (SpringVersion.getVersion().startsWith("2")) {
			assertThat(taskExecutor.getClass()).isEqualTo(SyncTaskExecutor.class);
		}
		else {
			assertThat(taskExecutor).isNull();
		}
		context.close();
	}

	@Test
	public void testAsyncMulticasterExplicitlySetToFalse() throws Exception {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithoutAsyncEventMulticaster.xml", this.getClass());
		context.refresh();
		SimpleApplicationEventMulticaster multicaster = (SimpleApplicationEventMulticaster)
				context.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
		DirectFieldAccessor accessor = new DirectFieldAccessor(multicaster);
		Object taskExecutor = accessor.getPropertyValue("taskExecutor");
		if (SpringVersion.getVersion().startsWith("2")) {
			assertThat(taskExecutor.getClass()).isEqualTo(SyncTaskExecutor.class);
		}
		else {
			assertThat(taskExecutor).isNull();
		}
		context.close();
	}

	@Test
	public void testAsyncMulticaster() throws Exception {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithAsyncEventMulticaster.xml", this.getClass());
		context.refresh();
		SimpleApplicationEventMulticaster multicaster = (SimpleApplicationEventMulticaster)
				context.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
		DirectFieldAccessor accessor = new DirectFieldAccessor(multicaster);
		Object taskExecutor = accessor.getPropertyValue("taskExecutor");
		assertThat(taskExecutor.getClass()).isEqualTo(ThreadPoolTaskExecutor.class);
		context.close();
	}

	@Test
	public void testExplicitlyDefinedTaskSchedulerHasCorrectType() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithTaskScheduler.xml", this.getClass());
		TaskScheduler scheduler = (TaskScheduler) context.getBean("taskScheduler");
		assertThat(scheduler.getClass()).isEqualTo(StubTaskScheduler.class);
		context.close();
	}

	@Test
	public void testExplicitlyDefinedTaskSchedulerMatchesUtilLookup() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithTaskScheduler.xml", this.getClass());
		TaskScheduler scheduler = (TaskScheduler) context.getBean("taskScheduler");
		assertThat(IntegrationContextUtils.getTaskScheduler(context)).isEqualTo(scheduler);
		context.close();
	}

}
