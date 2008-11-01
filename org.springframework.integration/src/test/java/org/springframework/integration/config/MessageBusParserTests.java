/*
 * Copyright 2002-2008 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.bus.MessageBusEventTests.TestMessageBusEventListener;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.config.xml.MessageBusParser;
import org.springframework.integration.scheduling.TaskScheduler;
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
		assertEquals(context.getBean("errorChannel"), resolver.resolveChannelName("errorChannel"));
	}

	@Test
	public void testDefaultErrorChannel() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaults.xml", this.getClass());
		BeanFactoryChannelResolver resolver = new BeanFactoryChannelResolver(context);
		assertEquals(context.getBean("errorChannel"), resolver.resolveChannelName("errorChannel"));
	}

	@Test
	public void testMultipleMessageBusElements() {
		boolean exceptionThrown = false;
		try {
			new ClassPathXmlApplicationContext("multipleMessageBusElements.xml", this.getClass());
		}
		catch (BeanDefinitionStoreException e) {
			exceptionThrown = true;
			assertEquals(IllegalStateException.class, e.getCause().getClass());
		}
		assertTrue(exceptionThrown);
	}

	@Test
	public void testMessageBusElementAndBean() {
		boolean exceptionThrown = false;
		try {
			new ClassPathXmlApplicationContext("messageBusElementAndBean.xml", this.getClass());
		}
		catch (BeanCreationException e) {
			exceptionThrown = true;
			assertEquals(IllegalStateException.class, e.getCause().getClass());
			assertEquals(e.getBeanName(), MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		}
		assertTrue(exceptionThrown);
	}

	@Test
	public void testAutoStartup() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithAutoStartup.xml", this.getClass());
		Lifecycle bus = (Lifecycle) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		assertTrue(bus.isRunning());
		bus.stop();
	}

	@Test
	public void testMulticasterIsSyncByDefault() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaults.xml", this.getClass());
		SimpleApplicationEventMulticaster multicaster = (SimpleApplicationEventMulticaster)
				context.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
		DirectFieldAccessor accessor = new DirectFieldAccessor(multicaster);
		Object taskExecutor = accessor.getPropertyValue("taskExecutor");
		assertEquals(SyncTaskExecutor.class, taskExecutor.getClass());
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
		assertEquals(SyncTaskExecutor.class, taskExecutor.getClass());
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
	public void testMessageBusEventListenerReceivesStartedEvent() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithListener.xml", this.getClass());
		Lifecycle messageBus = (Lifecycle) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		TestMessageBusEventListener listener = (TestMessageBusEventListener) context.getBean("listener");
		assertNull(listener.getStartedBus());
		assertNull(listener.getStoppedBus());
		messageBus.start();
		assertNotNull(listener.getStartedBus());
		assertEquals(messageBus, listener.getStartedBus());
		assertNull(listener.getStoppedBus());
	}

	@Test
	public void testMessageBusEventListenerReceivesStoppedEvent() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithListener.xml", this.getClass());
		Lifecycle messageBus = (Lifecycle) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		TestMessageBusEventListener listener = (TestMessageBusEventListener) context.getBean("listener");
		assertNull(listener.getStoppedBus());
		messageBus.start();
		messageBus.stop();
		assertNotNull(listener.getStoppedBus());
		assertEquals(messageBus, listener.getStoppedBus());
	}

	@Test
	public void testMessageBusWithTaskScheduler() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithTaskScheduler.xml", this.getClass());
		Object messageBus = context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		StubTaskScheduler schedulerBean = (StubTaskScheduler) context.getBean("testScheduler");
		TaskScheduler busScheduler = (TaskScheduler) new DirectFieldAccessor(messageBus).getPropertyValue("taskScheduler");
		assertNotNull(busScheduler);
		assertEquals(schedulerBean, busScheduler);
	}

}
