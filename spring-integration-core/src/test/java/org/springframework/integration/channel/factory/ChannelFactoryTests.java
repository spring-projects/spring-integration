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

package org.springframework.integration.channel.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * @author Marius Bogoevici
 */
public class ChannelFactoryTests {

	ArrayList<ChannelInterceptor> interceptors = null;

	DispatcherPolicy dispatcherPolicy = null;

	@Before
	public void createInterceptorsList() {
		interceptors = new ArrayList<ChannelInterceptor>();
		interceptors.add(new TestChannelInterceptor());
		interceptors.add(new TestChannelInterceptor());
	}

	@Before
	public void createDispatcherPolicy() {
		dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setMaxMessagesPerTask(100);
	}


	@Test
	public void testQueueChannelFactory() {
		QueueChannelFactory channelFactory = new QueueChannelFactory();
		channelFactory.setQueueCapacity(99);
		genericChannelFactoryTests(channelFactory, QueueChannel.class);
		assertEquals(99, channelFactory.getQueueCapacity());
	}

	@Test
	public void testDirectChannelFactory() {
		DirectChannelFactory channelFactory = new DirectChannelFactory();
		assertNotNull(interceptors);
		AbstractMessageChannel channel = (AbstractMessageChannel)
				channelFactory.getChannel(dispatcherPolicy, interceptors);
		assertEquals(DirectChannel.class, channel.getClass());
		assertInterceptors(channel);
	}

	@Test
	public void testRendezvousChannelFactory() {
		RendezvousChannelFactory channelFactory = new RendezvousChannelFactory();
		genericChannelFactoryTests(channelFactory, RendezvousChannel.class);
	}

	@Test
	public void testPriorityChannelFactory() {
		RendezvousChannelFactory channelFactory = new RendezvousChannelFactory();
		genericChannelFactoryTests(channelFactory, RendezvousChannel.class);
	}

	@Test
	public void testDefaultChannelFactoryBean() throws Exception{
		MessageBus messageBus = new MessageBus();
		ChannelFactory channelFactory = new StubChannelFactory();
		messageBus.setChannelFactory(channelFactory);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		BeanDefinitionBuilder messageBusDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(MessageBus.class);
		messageBusDefinitionBuilder.getBeanDefinition().getPropertyValues().addPropertyValue("channelFactory", channelFactory);
		applicationContext.registerBeanDefinition("messageBus", messageBusDefinitionBuilder.getBeanDefinition());
		DefaultChannelFactoryBean channelFactoryBean =  new DefaultChannelFactoryBean(dispatcherPolicy);
		channelFactoryBean.setApplicationContext(applicationContext);
		channelFactoryBean.setInterceptors(interceptors);
		StubChannel channel = (StubChannel)channelFactoryBean.getObject();
		assertTrue(dispatcherPolicy == channel.getDispatcherPolicy());
		assertInterceptors(channel);
	}


	private void genericChannelFactoryTests(ChannelFactory channelFactory, Class<?> expectedChannelClass) {
		assertNotNull(dispatcherPolicy);
		assertNotNull(interceptors);
		AbstractMessageChannel channel = (AbstractMessageChannel)
				channelFactory.getChannel(dispatcherPolicy, interceptors);
		assertEquals(expectedChannelClass, channel.getClass());
		assertTrue(channel.getDispatcherPolicy() == dispatcherPolicy);
		assertInterceptors(channel);
	}

	@SuppressWarnings("unchecked")
	private void assertInterceptors(AbstractMessageChannel channel) {
		Object interceptorsWrapper = new DirectFieldAccessor(channel).getPropertyValue("interceptors");
		List<ChannelInterceptor> interceptors = (List<ChannelInterceptor>)
				new DirectFieldAccessor(interceptorsWrapper).getPropertyValue("interceptors");
		assertTrue(interceptors.get(0) == interceptors.get(0));
		assertTrue(interceptors.get(1) == interceptors.get(1));
	}


	static class TestChannelInterceptor implements ChannelInterceptor {

		public void postReceive(Message<?> message, MessageChannel channel) {
		}

		public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
		}

		public boolean preReceive(MessageChannel channel) {
			return false;
		}

		public boolean preSend(Message<?> message, MessageChannel channel) {
			return false;
		}

	}


	static class StubChannel extends AbstractMessageChannel {

		public StubChannel(DispatcherPolicy dispatcherPolicy) {
			super(dispatcherPolicy);
		}

		@Override
		protected Message<?> doReceive(long timeout) {
			return null;
		}

		@Override
		protected boolean doSend(Message<?> message, long timeout) {
			return false;
		}

		public List<Message<?>> clear() {
			return null;
		}

		public List<Message<?>> purge(MessageSelector selector) {
			return null;
		}
	}


	static class StubChannelFactory extends AbstractChannelFactory {

		@Override
		protected AbstractMessageChannel createChannelInternal(DispatcherPolicy dispatcherPolicy) {
			return new StubChannel(dispatcherPolicy);
		}
	}

}
