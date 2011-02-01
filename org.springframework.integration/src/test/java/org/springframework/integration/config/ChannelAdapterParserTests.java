/*
 * Copyright 2002-2011 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolutionException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.util.TestUtils;

/**
 * @author Mark Fisher
 */
public class ChannelAdapterParserTests {

	private AbstractApplicationContext applicationContext;


	@Before
	public void setUp() {
		this.applicationContext = new ClassPathXmlApplicationContext(
				"ChannelAdapterParserTests-context.xml", this.getClass());
	}

	@After
	public void tearDown() {
		this.applicationContext.close();
	}


	@Test
	public void methodInvokingSourceStoppedByApplicationContext() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		this.applicationContext.start();
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals("source test", testBean.getMessage());
		this.applicationContext.stop();
		message = channel.receive(100);
		assertNull(message);
	}

	@Test
	public void targetOnly() {
		String beanName = "outboundWithImplicitChannel";
		Object channel = this.applicationContext.getBean(beanName);
		assertTrue(channel instanceof DirectChannel);
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertNotNull(channelResolver.resolveChannelName(beanName));
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertNotNull(adapter);
		assertTrue(adapter instanceof EventDrivenConsumer);
		TestConsumer consumer = (TestConsumer) this.applicationContext.getBean("consumer");
		assertNull(consumer.getLastMessage());
		Message<?> message = new StringMessage("test");
		assertTrue(((MessageChannel) channel).send(message));
		assertNotNull(consumer.getLastMessage());
		assertEquals(message, consumer.getLastMessage());
	}

	@Test
	public void methodInvokingConsumer() {
		String beanName = "methodInvokingConsumer";
		Object channel = this.applicationContext.getBean(beanName);
		assertTrue(channel instanceof DirectChannel);
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertNotNull(channelResolver.resolveChannelName(beanName));
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertNotNull(adapter);
		assertTrue(adapter instanceof EventDrivenConsumer);
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		assertNull(testBean.getMessage());
		Message<?> message = new StringMessage("consumer test");
		assertTrue(((MessageChannel) channel).send(message));
		assertNotNull(testBean.getMessage());
		assertEquals("consumer test", testBean.getMessage());
	}

	@Test
	public void methodInvokingSource() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		((SourcePollingChannelAdapter) adapter).start();
		Message<?> message = channel.receive(100);
		assertNotNull(message);
		assertEquals("source test", testBean.getMessage());
		((SourcePollingChannelAdapter) adapter).stop();
	}

	@Test
	public void methodInvokingSourceNotStarted() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		Message<?> message = channel.receive(100);
		assertNull(message);
	}

	@Test
	public void methodInvokingSourceStopped() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		((SourcePollingChannelAdapter) adapter).start();
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals("source test", testBean.getMessage());
		((SourcePollingChannelAdapter) adapter).stop();
		message = channel.receive(100);
		assertNull(message);
	}

	@Test
	public void methodInvokingSourceStartedByApplicationContext() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		this.applicationContext.start();
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals("source test", testBean.getMessage());
		this.applicationContext.stop();
	}

	@Test(expected = ChannelResolutionException.class)
	public void methodInvokingSourceAdapterIsNotChannel() {
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		channelResolver.resolveChannelName("methodInvokingSource");
	}

	@Test
	public void methodInvokingSourceWithSendTimeout() throws Exception{
		String beanName = "methodInvokingSourceWithTimeout";
		SourcePollingChannelAdapter adapter = 
				(SourcePollingChannelAdapter) this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		long sendTimeout = TestUtils.getPropertyValue(adapter, "channelTemplate.sendTimeout", Long.class);
		assertEquals(999, sendTimeout);
	}

}
