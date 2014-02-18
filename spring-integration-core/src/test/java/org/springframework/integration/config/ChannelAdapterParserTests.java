/*
 * Copyright 2002-2014 the original author or authors.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class ChannelAdapterParserTests {

	private AbstractApplicationContext applicationContext;
	private AbstractApplicationContext applicationContextInner;


	@Before
	public void setUp() {
		this.applicationContext = new ClassPathXmlApplicationContext(
				"ChannelAdapterParserTests-context.xml", this.getClass());
		this.applicationContextInner = new ClassPathXmlApplicationContext(
				"ChannelAdapterParserTests-inner-context.xml", this.getClass());
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
		assertEquals(-1, ((SourcePollingChannelAdapter) adapter).getPhase());
		this.applicationContext.start();
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals("source test", testBean.getMessage());
		this.applicationContext.stop();
		message = channel.receive(100);
		assertNull(message);
	}

	@Test
	public void methodInvokingSourceStoppedByApplicationContextInner() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContextInner.getBean("queueChannel");
//		TestBean testBean = (TestBean) this.applicationContextInner.getBean("testBean");
//		testBean.store("source test");
		Object adapter = this.applicationContextInner.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		this.applicationContextInner.start();
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		//assertEquals("source test", testBean.getMessage());
		this.applicationContextInner.stop();
		message = channel.receive(100);
		assertNull(message);
	}

	@Test
	public void targetOnly() {
		String beanName = "outboundWithImplicitChannel";
		Object channel = this.applicationContext.getBean(beanName);
		assertTrue(channel instanceof DirectChannel);
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertNotNull(channelResolver.resolveDestination(beanName));
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertNotNull(adapter);
		assertTrue(adapter instanceof EventDrivenConsumer);
		assertFalse(((EventDrivenConsumer) adapter).isAutoStartup());
		assertEquals(-1, ((EventDrivenConsumer) adapter).getPhase());
		TestConsumer consumer = (TestConsumer) this.applicationContext.getBean("consumer");
		assertNull(consumer.getLastMessage());
		Message<?> message = new GenericMessage<String>("test");
		try {
			((MessageChannel) channel).send(message);
			fail("MessageDispatchingException is expected.");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(MessageDeliveryException.class));
			assertThat(e.getCause(), Matchers.instanceOf(MessageDispatchingException.class));
		}

		((EventDrivenConsumer) adapter).start();
		((MessageChannel) channel).send(message);
		assertNotNull(consumer.getLastMessage());
		assertEquals(message, consumer.getLastMessage());
	}

	@Test
	public void methodInvokingConsumer() {
		String beanName = "methodInvokingConsumer";
		Object channel = this.applicationContext.getBean(beanName);
		assertTrue(channel instanceof DirectChannel);
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertNotNull(channelResolver.resolveDestination(beanName));
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertNotNull(adapter);
		assertTrue(adapter instanceof EventDrivenConsumer);
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		assertNull(testBean.getMessage());
		Message<?> message = new GenericMessage<String>("consumer test");
		assertTrue(((MessageChannel) channel).send(message));
		assertNotNull(testBean.getMessage());
		assertEquals("consumer test", testBean.getMessage());
	}

	@Test
	/**
	 * @since 2.1
	 */
	public void expressionConsumer() {
		String beanName = "expressionConsumer";
		Object channel = this.applicationContext.getBean(beanName);
		assertTrue(channel instanceof DirectChannel);
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertNotNull(channelResolver.resolveDestination(beanName));
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertNotNull(adapter);
		assertTrue(adapter instanceof EventDrivenConsumer);
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		assertNull(testBean.getMessage());
		Message<?> message = new GenericMessage<String>("consumer test expression");
		assertTrue(((MessageChannel) channel).send(message));
		assertNotNull(testBean.getMessage());
		assertEquals("consumer test expression", testBean.getMessage());
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
	public void methodInvokingSourceWithHeaders() {
		String beanName = "methodInvokingSourceWithHeaders";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannelForHeadersTest");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertNotNull(adapter);
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		((SourcePollingChannelAdapter) adapter).start();
		Message<?> message = channel.receive(100);
		((SourcePollingChannelAdapter) adapter).stop();
		assertNotNull(message);
		assertEquals("source test", testBean.getMessage());
		assertEquals("source test", message.getPayload());
		assertEquals("ABC", message.getHeaders().get("foo"));
		assertEquals(123, message.getHeaders().get("bar"));
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

	@Test(expected = DestinationResolutionException.class)
	public void methodInvokingSourceAdapterIsNotChannel() {
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		channelResolver.resolveDestination("methodInvokingSource");
	}

	@Test
	public void methodInvokingSourceWithSendTimeout() throws Exception {
		String beanName = "methodInvokingSourceWithTimeout";

		SourcePollingChannelAdapter adapter =
				this.applicationContext.getBean(beanName, SourcePollingChannelAdapter.class);
		assertNotNull(adapter);
		long sendTimeout = TestUtils.getPropertyValue(adapter, "messagingTemplate.sendTimeout", Long.class);
		assertEquals(999, sendTimeout);
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void innerBeanAndExpressionFail() throws Exception {
		new ClassPathXmlApplicationContext("InboundChannelAdapterInnerBeanWithExpression-fail-context.xml", this.getClass());
	}

	@Test
	public void testMessageSourceUniqueIds() {
		PollableChannel channel1 = this.applicationContext.getBean("channelAdapter1Channel", PollableChannel.class);
		PollableChannel channel2 = this.applicationContext.getBean("channelAdapter2Channel", PollableChannel.class);

		for (int i = 0; i < 10; i++) {
			Message<?> message = channel1.receive(5000);
			assertNotNull(message);
			assertEquals(i + 1, message.getPayload());
			message = channel2.receive(5000);
			assertEquals(i + 1, message.getPayload());
		}
	}

	@Test
	public void testMessageSourceRef() {
		PollableChannel channel = this.applicationContext.getBean("messageSourceRefChannel", PollableChannel.class);

		Message<?> message = channel.receive(5000);
		assertNotNull(message);
		assertEquals("test", message.getPayload());

		MessageSource<?> testMessageSource = this.applicationContext.getBean("testMessageSource", MessageSource.class);
		SourcePollingChannelAdapter adapterWithMessageSourceRef = this.applicationContext.getBean("adapterWithMessageSourceRef", SourcePollingChannelAdapter.class);
		MessageSource<?> source = TestUtils.getPropertyValue(adapterWithMessageSourceRef, "source", MessageSource.class);
		assertSame(testMessageSource, source);
	}

	public static class SampleBean {
		private final String message = "hello";

		String getMessage() {
			return message;
		}
	}
}

