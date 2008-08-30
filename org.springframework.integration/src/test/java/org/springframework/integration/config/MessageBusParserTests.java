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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.bus.DefaultMessageBus;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.bus.MessageBusInterceptorTests;
import org.springframework.integration.bus.TestMessageBusAwareImpl;
import org.springframework.integration.bus.TestMessageBusStartInterceptor;
import org.springframework.integration.bus.TestMessageBusStopInterceptor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.integration.scheduling.spi.ProviderTaskScheduler;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class MessageBusParserTests {

	@Test
	public void testErrorChannelReference() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithErrorChannel.xml", this.getClass());
		DefaultMessageBus bus = (DefaultMessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		bus.initialize();
		assertEquals(context.getBean("errorChannel"), bus.getErrorChannel());
	}

	@Test
	public void testDefaultErrorChannel() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithDefaults.xml", this.getClass());
		DefaultMessageBus bus = (DefaultMessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		bus.initialize();
		assertNotNull("bus should have created a default error channel", bus.getErrorChannel());
	}

	@Test
	public void testMultipleMessageBusElements() {
		boolean exceptionThrown = false;
		try {
			new ClassPathXmlApplicationContext("multipleMessageBusElements.xml", this.getClass());
		}
		catch (BeanDefinitionStoreException e) {
			exceptionThrown = true;
			assertEquals(ConfigurationException.class, e.getCause().getClass());
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
			// an exception is thrown when creating the post-processor, which
			// tries to get a reference to the message bus
			assertEquals(BeanCreationException.class, e.getCause().getClass());
			assertEquals(e.getBeanName(), MessageBusParser.MESSAGE_BUS_AWARE_POST_PROCESSOR_BEAN_NAME);
			assertEquals(ConfigurationException.class, (e.getCause()).getCause().getClass());
			assertEquals(((BeanCreationException) e.getCause()).getBeanName(), MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		}
		assertTrue(exceptionThrown);
	}

	@Test
	public void testAutoStartup() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithAutoStartup.xml", this.getClass());
		MessageBus bus = (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		assertTrue(bus.isRunning());
		bus.stop();
	}

	@Test
	public void testMessageBusAwareAutomaticallyAddedByNamespace() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithMessageBusAware.xml", this.getClass());
		TestMessageBusAwareImpl messageBusAware = (TestMessageBusAwareImpl) context.getBean("messageBusAwareBean");
		assertTrue(messageBusAware.getMessageBus() == context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME));
	}

	@Test
	public void testMessageBusWithChannelFactory() {
		ApplicationContext context = new ClassPathXmlApplicationContext("messageBusWithChannelFactory.xml",
				this.getClass());
		((MessageChannel)context.getBean("defaultTypeChannel")).getName();
		assertEquals(DirectChannel.class, context.getBean("defaultTypeChannel").getClass());
		assertEquals(QueueChannel.class, context.getBean("specifiedTypeChannel").getClass());
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
		assertEquals(ProviderTaskScheduler.class, taskExecutor.getClass());
	}

	@Test
	public void testMessageBusWithInterceptors() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithInterceptors.xml", this.getClass());
		MessageBus messageBus = (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		TestMessageBusStartInterceptor startInterceptor = (TestMessageBusStartInterceptor) context.getBean(
				"startInterceptor");
		TestMessageBusStopInterceptor stopInterceptor = (TestMessageBusStopInterceptor) context.getBean(
				"stopInterceptor");
		MessageBusInterceptorTests.executeInterceptorsTest(messageBus, startInterceptor, stopInterceptor);
	}

	@Test
	public void testMessageBusWithTaskScheduler() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"messageBusWithTaskScheduler.xml", this.getClass());
		MessageBus messageBus = (MessageBus) context.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		StubTaskScheduler schedulerBean = (StubTaskScheduler) context.getBean("testScheduler");
		TaskScheduler busScheduler = (TaskScheduler) new DirectFieldAccessor(messageBus).getPropertyValue("taskScheduler");
		assertNotNull(busScheduler);
		assertEquals(schedulerBean, busScheduler);
	}

}
