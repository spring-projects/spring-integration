/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class MessageBusParserTests {

	@Test
	public void testErrorChannelReference() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithErrorChannel.xml", this.getClass());
		BeanFactoryChannelResolver resolver = new BeanFactoryChannelResolver(context);
		assertEquals(context.getBean("errorChannel"), resolver.resolveDestination("errorChannel"));
	}

	@Test
	public void testDefaultErrorChannel() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaults.xml", this.getClass());
		BeanFactoryChannelResolver resolver = new BeanFactoryChannelResolver(context);
		assertEquals(context.getBean("errorChannel"), resolver.resolveDestination("errorChannel"));
	}

	@Test
	public void testMulticasterIsSyncByDefault() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaults.xml", this.getClass());
		SimpleApplicationEventMulticaster multicaster = (SimpleApplicationEventMulticaster)
				context.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
		DirectFieldAccessor accessor = new DirectFieldAccessor(multicaster);
		Object taskExecutor = accessor.getPropertyValue("taskExecutor");
		if (SpringVersion.getVersion().startsWith("2")) {
			assertEquals(SyncTaskExecutor.class, taskExecutor.getClass());
		}
		else {
			assertNull(taskExecutor);
		}
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
			assertEquals(SyncTaskExecutor.class, taskExecutor.getClass());
		}
		else {
			assertNull(taskExecutor);
		}
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
		assertEquals(ThreadPoolTaskExecutor.class, taskExecutor.getClass());
	}

	@Test
	public void testExplicitlyDefinedTaskSchedulerHasCorrectType() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithTaskScheduler.xml", this.getClass());
		TaskScheduler scheduler = (TaskScheduler) context.getBean("taskScheduler");
		assertEquals(StubTaskScheduler.class, scheduler.getClass());
	}

	@Test
	public void testExplicitlyDefinedTaskSchedulerMatchesUtilLookup() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithTaskScheduler.xml", this.getClass());
		TaskScheduler scheduler = (TaskScheduler) context.getBean("taskScheduler");
		assertEquals(scheduler, IntegrationContextUtils.getTaskScheduler(context));
	}

}
