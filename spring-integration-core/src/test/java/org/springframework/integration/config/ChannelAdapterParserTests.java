/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
		this.applicationContextInner.close();
	}


	@Test
	public void methodInvokingSourceStoppedByApplicationContext() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof SourcePollingChannelAdapter).isTrue();
		assertThat(((SourcePollingChannelAdapter) adapter).getPhase()).isEqualTo(-1);
		this.applicationContext.start();
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("source test");
		this.applicationContext.stop();
		message = channel.receive(100);
		assertThat(message).isNull();
	}

	@Test
	public void methodInvokingSourceStoppedByApplicationContextInner() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContextInner.getBean("queueChannel");
//		TestBean testBean = (TestBean) this.applicationContextInner.getBean("testBean");
//		testBean.store("source test");
		Object adapter = this.applicationContextInner.getBean(beanName);
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof SourcePollingChannelAdapter).isTrue();
		this.applicationContextInner.start();
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		//assertEquals("source test", testBean.getMessage());
		this.applicationContextInner.stop();
		message = channel.receive(100);
		assertThat(message).isNull();
	}

	@Test
	public void targetOnly() {
		String beanName = "outboundWithImplicitChannel";
		Object channel = this.applicationContext.getBean(beanName);
		assertThat(channel instanceof DirectChannel).isTrue();
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertThat(channelResolver.resolveDestination(beanName)).isNotNull();
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof EventDrivenConsumer).isTrue();
		assertThat(((EventDrivenConsumer) adapter).isAutoStartup()).isFalse();
		assertThat(((EventDrivenConsumer) adapter).getPhase()).isEqualTo(-1);
		TestConsumer consumer = (TestConsumer) this.applicationContext.getBean("consumer");
		assertThat(consumer.getLastMessage()).isNull();
		Message<?> message = new GenericMessage<String>("test");
		try {
			((MessageChannel) channel).send(message);
			fail("MessageDispatchingException is expected.");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(MessageDeliveryException.class);
			assertThat(e.getCause()).isInstanceOf(MessageDispatchingException.class);
		}

		((EventDrivenConsumer) adapter).start();
		((MessageChannel) channel).send(message);
		assertThat(consumer.getLastMessage()).isNotNull();
		assertThat(consumer.getLastMessage()).isEqualTo(message);
	}

	@Test
	public void methodInvokingConsumer() {
		String beanName = "methodInvokingConsumer";
		Object channel = this.applicationContext.getBean(beanName);
		assertThat(channel instanceof DirectChannel).isTrue();
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertThat(channelResolver.resolveDestination(beanName)).isNotNull();
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof EventDrivenConsumer).isTrue();
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		assertThat(testBean.getMessage()).isNull();
		Message<?> message = new GenericMessage<String>("consumer test");
		assertThat(((MessageChannel) channel).send(message)).isTrue();
		assertThat(testBean.getMessage()).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("consumer test");
	}

	@Test
	/**
	 * @since 2.1
	 */
	public void expressionConsumer() {
		String beanName = "expressionConsumer";
		Object channel = this.applicationContext.getBean(beanName);
		assertThat(channel instanceof DirectChannel).isTrue();
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertThat(channelResolver.resolveDestination(beanName)).isNotNull();
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof EventDrivenConsumer).isTrue();
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		assertThat(testBean.getMessage()).isNull();
		Message<?> message = new GenericMessage<String>("consumer test expression");
		assertThat(((MessageChannel) channel).send(message)).isTrue();
		assertThat(testBean.getMessage()).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("consumer test expression");
	}

	@Test
	public void methodInvokingSource() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof SourcePollingChannelAdapter).isTrue();
		((SourcePollingChannelAdapter) adapter).start();
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("source test");
		((SourcePollingChannelAdapter) adapter).stop();
	}

	@Test
	public void methodInvokingSourceWithHeaders() {
		String beanName = "methodInvokingSourceWithHeaders";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannelForHeadersTest");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof SourcePollingChannelAdapter).isTrue();
		((SourcePollingChannelAdapter) adapter).start();
		Message<?> message = channel.receive(10000);
		((SourcePollingChannelAdapter) adapter).stop();
		assertThat(message).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("source test");
		assertThat(message.getPayload()).isEqualTo("source test");
		assertThat(message.getHeaders().get("foo")).isEqualTo("ABC");
		assertThat(message.getHeaders().get("bar")).isEqualTo(123);
	}

	@Test
	public void methodInvokingSourceNotStarted() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof SourcePollingChannelAdapter).isTrue();
		Message<?> message = channel.receive(100);
		assertThat(message).isNull();
	}

	@Test
	public void methodInvokingSourceStopped() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof SourcePollingChannelAdapter).isTrue();
		((SourcePollingChannelAdapter) adapter).start();
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("source test");
		((SourcePollingChannelAdapter) adapter).stop();
		message = channel.receive(100);
		assertThat(message).isNull();
	}

	@Test
	public void methodInvokingSourceStartedByApplicationContext() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		TestBean testBean = (TestBean) this.applicationContext.getBean("testBean");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof SourcePollingChannelAdapter).isTrue();
		this.applicationContext.start();
		Message<?> message = channel.receive(1000);
		assertThat(message).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("source test");
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
		assertThat(adapter).isNotNull();
		long sendTimeout = TestUtils.getPropertyValue(adapter, "messagingTemplate.sendTimeout", Long.class);
		assertThat(sendTimeout).isEqualTo(999);
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void innerBeanAndExpressionFail() throws Exception {
		new ClassPathXmlApplicationContext("InboundChannelAdapterInnerBeanWithExpression-fail-context.xml",
				this.getClass()).close();
	}

	@Test
	public void testMessageSourceUniqueIds() {
		PollableChannel channel1 = this.applicationContext.getBean("channelAdapter1Channel", PollableChannel.class);
		PollableChannel channel2 = this.applicationContext.getBean("channelAdapter2Channel", PollableChannel.class);

		for (int i = 0; i < 10; i++) {
			Message<?> message = channel1.receive(5000);
			assertThat(message).isNotNull();
			assertThat(message.getPayload()).isEqualTo(i + 1);
			message = channel2.receive(5000);
			assertThat(message).isNotNull();
			assertThat(message.getPayload()).isEqualTo(i + 1);
		}
	}

	@Test
	public void testMessageSourceRef() {
		PollableChannel channel = this.applicationContext.getBean("messageSourceRefChannel", PollableChannel.class);

		Message<?> message = channel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("test");

		MessageSource<?> testMessageSource = this.applicationContext.getBean("testMessageSource", MessageSource.class);
		SourcePollingChannelAdapter adapterWithMessageSourceRef =
				this.applicationContext.getBean("adapterWithMessageSourceRef", SourcePollingChannelAdapter.class);
		MessageSource<?> source = TestUtils.getPropertyValue(adapterWithMessageSourceRef, "source", MessageSource.class);
		assertThat(source).isSameAs(testMessageSource);
	}

	public static class SampleBean {

		private final String message = "hello";

		String getMessage() {
			return message;
		}

	}

}

