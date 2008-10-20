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

package org.springframework.integration.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.config.xml.MessageBusParser;
import org.springframework.integration.util.TestUtils;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class MessageBusEventTests {

	@Test
	public void messageBusStartedEvent() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("listener", new RootBeanDefinition(TestMessageBusListener.class));
		ApplicationContextMessageBus messageBus = new ApplicationContextMessageBus();
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		messageBus.setApplicationContext(context);
		context.getBeanFactory().registerSingleton(MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		TestMessageBusListener listener = (TestMessageBusListener) context.getBean("listener");
		assertNull(listener.startedBus);
		assertNull(listener.stoppedBus);
		context.refresh(); // bus will start
		assertNotNull(listener.startedBus);
		assertNull(listener.stoppedBus);
		assertEquals(messageBus, listener.startedBus);
	}

	@Test
	public void messageBusStoppedEvent() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("listener", new RootBeanDefinition(TestMessageBusListener.class));
		ApplicationContextMessageBus messageBus = new ApplicationContextMessageBus();
		messageBus.setTaskScheduler(TestUtils.createTaskScheduler(10));
		messageBus.setApplicationContext(context);
		context.getBeanFactory().registerSingleton(MessageBusParser.MESSAGE_BUS_BEAN_NAME, messageBus);
		TestMessageBusListener listener = (TestMessageBusListener) context.getBean("listener");
		assertNull(listener.startedBus);
		assertNull(listener.stoppedBus);
		context.refresh();
		assertNotNull(listener.startedBus);
		assertNull(listener.stoppedBus);
		messageBus.stop();
		assertNotNull(listener.stoppedBus);
		assertEquals(messageBus, listener.stoppedBus);
	}


	public static class TestMessageBusListener implements ApplicationListener {

		private volatile MessageBus startedBus;

		private volatile MessageBus stoppedBus;


		public MessageBus getStartedBus() {
			return this.startedBus;
		}

		public MessageBus getStoppedBus() {
			return this.stoppedBus;
		}

		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof MessageBusStartedEvent) {
				this.startedBus = (MessageBus) event.getSource();
			}
			if (event instanceof MessageBusStoppedEvent) {
				this.stoppedBus = (MessageBus) event.getSource();
			}
		}
	}

}
