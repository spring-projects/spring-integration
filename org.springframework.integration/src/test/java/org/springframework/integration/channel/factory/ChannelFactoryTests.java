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
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.bus.DefaultChannelFactoryBean;
import org.springframework.integration.bus.DefaultMessageBus;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.integration.channel.ThreadLocalChannel;
import org.springframework.integration.dispatcher.DirectChannel;
import org.springframework.integration.dispatcher.DirectChannelFactory;
import org.springframework.integration.message.Message;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class ChannelFactoryTests {

	private final ArrayList<ChannelInterceptor> interceptors = new ArrayList<ChannelInterceptor>();

	private final DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();


	@Before
	public void initInterceptorsList() {
		interceptors.add(new TestChannelInterceptor());
		interceptors.add(new TestChannelInterceptor());
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
				channelFactory.getChannel("testChannel", dispatcherPolicy, interceptors);
		assertEquals(DirectChannel.class, channel.getClass());
		assertEquals("testChannel", channel.getName());
		assertInterceptors(channel);
	}

	@Test
	public void testRendezvousChannelFactory() {
		RendezvousChannelFactory channelFactory = new RendezvousChannelFactory();
		genericChannelFactoryTests(channelFactory, RendezvousChannel.class);
	}

	@Test
	public void testPriorityChannelFactory() {
		PriorityChannelFactory channelFactory = new PriorityChannelFactory();
		genericChannelFactoryTests(channelFactory, PriorityChannel.class);
	}

	@Test
	public void testThreadLocalChannelFactory() {
		ThreadLocalChannelFactory channelFactory = new ThreadLocalChannelFactory();
		assertNotNull(interceptors);
		AbstractMessageChannel channel = (AbstractMessageChannel)
				channelFactory.getChannel("testChannel", dispatcherPolicy, interceptors);
		assertEquals(ThreadLocalChannel.class, channel.getClass());
		assertEquals("testChannel", channel.getName());
		assertInterceptors(channel);
	}

	@Test
	public void testDefaultChannelFactoryBean() throws Exception{
		DefaultMessageBus messageBus = new DefaultMessageBus();
		ChannelFactory channelFactory = new StubChannelFactory();
		messageBus.setChannelFactory(channelFactory);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		BeanDefinitionBuilder messageBusDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(DefaultMessageBus.class);
		messageBusDefinitionBuilder.getBeanDefinition().getPropertyValues().addPropertyValue("channelFactory", channelFactory);
		applicationContext.registerBeanDefinition("messageBus", messageBusDefinitionBuilder.getBeanDefinition());
		DefaultChannelFactoryBean channelFactoryBean =  new DefaultChannelFactoryBean(dispatcherPolicy);
		channelFactoryBean.setBeanName("testChannel");
		channelFactoryBean.setApplicationContext(applicationContext);
		channelFactoryBean.setInterceptors(interceptors);
		StubChannel channel = (StubChannel) channelFactoryBean.getObject();
		assertEquals("testChannel", channel.getName());
		assertTrue(dispatcherPolicy == channel.getDispatcherPolicy());
		assertInterceptors(channel);
	}

	@Test
	public void testDefaultChannelFactoryBeanInApplicationContext() throws Exception{
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"defaultChannelFactoryBeanTests.xml", this.getClass());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		assertEquals(StubChannel.class, channel.getClass());
		assertEquals("testChannel", channel.getName());
		DispatcherPolicy dispatcherPolicy = (DispatcherPolicy) context.getBean("dispatcherPolicy");
		assertTrue(dispatcherPolicy == channel.getDispatcherPolicy());
	}


	private void genericChannelFactoryTests(ChannelFactory channelFactory, Class<?> expectedChannelClass) {
		assertNotNull(dispatcherPolicy);
		assertNotNull(interceptors);
		AbstractMessageChannel channel = (AbstractMessageChannel)
				channelFactory.getChannel("testChannel", dispatcherPolicy, interceptors);
		assertEquals(expectedChannelClass, channel.getClass());
		assertEquals("testChannel", channel.getName());
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


}
