/*
 * Copyright 2002-2021 the original author or authors.
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChannelAdapterParserTests {

	@Autowired
	private AbstractApplicationContext applicationContext;

	@Autowired
	private TestBean testBean;

	@Test
	public void methodInvokingSourceStoppedByApplicationContext() {
		testBean.store("source test");
		PollableChannel channel = this.applicationContext.getBean("queueChannel", PollableChannel.class);
		Object adapter = this.applicationContext.getBean("methodInvokingSource");
		assertThat(adapter).isInstanceOf(SourcePollingChannelAdapter.class);
		assertThat(((SourcePollingChannelAdapter) adapter).getPhase()).isEqualTo(-1);
		this.applicationContext.start();
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("source test");
		this.applicationContext.stop();
		message = channel.receive(0);
		assertThat(message).isNull();
	}

	@Test
	public void methodInvokingSourceStoppedByApplicationContextInner() {
		AbstractApplicationContext applicationContextInner =
				new ClassPathXmlApplicationContext("ChannelAdapterParserTests-inner-context.xml", this.getClass());
		PollableChannel channel = applicationContextInner.getBean("queueChannel", PollableChannel.class);
		Object adapter = applicationContextInner.getBean("methodInvokingSource");
		assertThat(adapter).isInstanceOf(SourcePollingChannelAdapter.class);
		applicationContextInner.start();
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		applicationContextInner.stop();
		message = channel.receive(0);
		assertThat(message).isNull();
		applicationContextInner.close();
	}

	@Test
	public void targetOnly() {
		String beanName = "outboundWithImplicitChannel";
		Object channel = this.applicationContext.getBean(beanName);
		assertThat(channel).isInstanceOf(DirectChannel.class);
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertThat(channelResolver.resolveDestination(beanName)).isNotNull();
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertThat(adapter).isInstanceOf(EventDrivenConsumer.class);
		assertThat(((EventDrivenConsumer) adapter).isAutoStartup()).isFalse();
		assertThat(((EventDrivenConsumer) adapter).getPhase()).isEqualTo(-1);
		TestConsumer consumer = (TestConsumer) this.applicationContext.getBean("consumer");
		assertThat(consumer.getLastMessage()).isNull();
		Message<?> message = new GenericMessage<>("test");
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> ((MessageChannel) channel).send(message))
				.withCauseInstanceOf(MessageDispatchingException.class);

		((EventDrivenConsumer) adapter).start();
		((MessageChannel) channel).send(message);
		assertThat(consumer.getLastMessage()).isEqualTo(message);
	}

	@Test
	public void methodInvokingConsumer() {
		String beanName = "methodInvokingConsumer";
		Object channel = this.applicationContext.getBean(beanName);
		assertThat(channel).isInstanceOf(DirectChannel.class);
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertThat(channelResolver.resolveDestination(beanName)).isNotNull();
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertThat(adapter).isInstanceOf(EventDrivenConsumer.class);
		assertThat(testBean.getMessage()).isNull();
		Message<?> message = new GenericMessage<>("consumer test");
		assertThat(((MessageChannel) channel).send(message)).isTrue();
		assertThat(testBean.getMessage()).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("consumer test");
	}

	@Test
	public void expressionConsumer() {
		String beanName = "expressionConsumer";
		Object channel = this.applicationContext.getBean(beanName);
		assertThat(channel instanceof DirectChannel).isTrue();
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertThat(channelResolver.resolveDestination(beanName)).isNotNull();
		Object adapter = this.applicationContext.getBean(beanName + ".adapter");
		assertThat(adapter).isInstanceOf(EventDrivenConsumer.class);
		assertThat(testBean.getMessage()).isNull();
		Message<?> message = new GenericMessage<>("consumer test expression");
		assertThat(((MessageChannel) channel).send(message)).isTrue();
		assertThat(testBean.getMessage()).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("consumer test expression");
	}

	@Test
	public void methodInvokingSource() {
		String beanName = "methodInvokingSource";
		PollableChannel channel = (PollableChannel) this.applicationContext.getBean("queueChannel");
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean(beanName);
		assertThat(adapter).isInstanceOf(SourcePollingChannelAdapter.class);
		((SourcePollingChannelAdapter) adapter).start();
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("source test");
		((SourcePollingChannelAdapter) adapter).stop();
	}

	@Test
	public void methodInvokingSourceWithHeaders() {
		PollableChannel channel = this.applicationContext.getBean("queueChannelForHeadersTest", PollableChannel.class);
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean("methodInvokingSourceWithHeaders");
		assertThat(adapter).isInstanceOf(SourcePollingChannelAdapter.class);
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
		PollableChannel channel = this.applicationContext.getBean("queueChannel", PollableChannel.class);
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean("methodInvokingSource");
		assertThat(adapter).isInstanceOf(SourcePollingChannelAdapter.class);
		Message<?> message = channel.receive(0);
		assertThat(message).isNull();
	}

	@Test
	public void methodInvokingSourceStopped() {
		PollableChannel channel = this.applicationContext.getBean("queueChannel", PollableChannel.class);
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean("methodInvokingSource");
		assertThat(adapter).isInstanceOf(SourcePollingChannelAdapter.class);
		((SourcePollingChannelAdapter) adapter).start();
		Message<?> message = channel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("source test");
		((SourcePollingChannelAdapter) adapter).stop();
		message = channel.receive(0);
		assertThat(message).isNull();
	}

	@Test
	public void methodInvokingSourceStartedByApplicationContext() {
		PollableChannel channel = this.applicationContext.getBean("queueChannel", PollableChannel.class);
		testBean.store("source test");
		Object adapter = this.applicationContext.getBean("methodInvokingSource");
		assertThat(adapter).isInstanceOf(SourcePollingChannelAdapter.class);
		this.applicationContext.start();
		Message<?> message = channel.receive(1000);
		assertThat(message).isNotNull();
		assertThat(testBean.getMessage()).isEqualTo("source test");
		this.applicationContext.stop();
	}

	@Test
	public void methodInvokingSourceAdapterIsNotChannel() {
		BeanFactoryChannelResolver channelResolver = new BeanFactoryChannelResolver(this.applicationContext);
		assertThatExceptionOfType(DestinationResolutionException.class)
				.isThrownBy(() -> channelResolver.resolveDestination("methodInvokingSource"));
	}

	@Test
	public void methodInvokingSourceWithSendTimeout() {
		SourcePollingChannelAdapter adapter =
				this.applicationContext.getBean("methodInvokingSourceWithTimeout", SourcePollingChannelAdapter.class);
		assertThat(adapter).isNotNull();
		long sendTimeout = TestUtils.getPropertyValue(adapter, "messagingTemplate.sendTimeout", Long.class);
		assertThat(sendTimeout).isEqualTo(999);
	}

	@Test
	public void innerBeanAndExpressionFail() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"InboundChannelAdapterInnerBeanWithExpression-fail-context.xml",
								this.getClass()));
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

		public String getMessage() {
			return  "hello";
		}

	}

}

