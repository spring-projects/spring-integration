/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.channel.config;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.TestChannelInterceptor;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.converter.DefaultDatatypeChannelMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @see ChannelWithCustomQueueParserTests
 */
@SpringJUnitConfig(locations = {
		"/org/springframework/integration/channel/config/ChannelParserTests-context.xml",
		"/org/springframework/integration/channel/config/priorityChannelParserTests.xml"})
@DirtiesContext
public class ChannelParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testChannelWithoutId() {
		assertThatExceptionOfType(FatalBeanException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext("channelWithoutId.xml", getClass()));
	}

	@Test
	public void testChannelWithCapacity() {
		MessageChannel channel = (MessageChannel) context.getBean("capacityChannel");
		for (int i = 0; i < 10; i++) {
			boolean result = channel.send(new GenericMessage<>("test"), 10);
			assertThat(result).isTrue();
		}
		assertThat(channel.send(new GenericMessage<>("test"), 3)).isFalse();
	}

	@Test
	public void testDirectChannelByDefault() {
		MessageChannel channel = (MessageChannel) context.getBean("defaultChannel");
		assertThat(channel).isInstanceOf(DirectChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		Object dispatcher = accessor.getPropertyValue("dispatcher");
		assertThat(dispatcher).isInstanceOf(UnicastingDispatcher.class);
		assertThat(new DirectFieldAccessor(dispatcher).getPropertyValue("loadBalancingStrategy"))
				.isInstanceOf(RoundRobinLoadBalancingStrategy.class);
	}

	@Test
	public void testExecutorChannel() {
		MessageChannel channel = context.getBean("executorChannel", MessageChannel.class);
		assertThat(channel).isInstanceOf(ExecutorChannel.class);
		assertThat(TestUtils.<Object>getPropertyValue(channel, "messageConverter")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(channel, "messageConverter.conversionService"))
				.isNotNull();
	}

	@Test
	public void testExecutorChannelNoConverter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"ChannelParserTests-no-converter-context.xml", this.getClass());
		MessageChannel channel = context.getBean("executorChannel", MessageChannel.class);
		assertThat(channel).isInstanceOf(ExecutorChannel.class);
		assertThat(TestUtils.<Object>getPropertyValue(channel, "messageConverter")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(channel, "messageConverter.conversionService"))
				.isNotNull();
		context.close();
	}

	@Test
	public void channelWithFailoverDispatcherAttribute() {
		MessageChannel channel = (MessageChannel) context.getBean("channelWithFailover");
		assertThat(channel.getClass()).isEqualTo(DirectChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		Object dispatcher = accessor.getPropertyValue("dispatcher");
		assertThat(dispatcher).isInstanceOf(UnicastingDispatcher.class);
		assertThat(new DirectFieldAccessor(dispatcher).getPropertyValue("loadBalancingStrategy")).isNull();
	}

	@Test
	public void testPublishSubscribeChannel() {
		MessageChannel channel = (MessageChannel) context.getBean("publishSubscribeChannel");
		assertThat(channel.getClass()).isEqualTo(PublishSubscribeChannel.class);
	}

	@Test
	public void testPublishSubscribeChannelWithTaskExecutorReference() {
		MessageChannel channel = (MessageChannel) context.getBean("publishSubscribeChannelWithTaskExecutorRef");
		assertThat(channel.getClass()).isEqualTo(PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("dispatcher"));
		Object executorProperty = accessor.getPropertyValue("executor");
		assertThat(executorProperty).isNotNull();
		assertThat(executorProperty.getClass()).isEqualTo(ErrorHandlingTaskExecutor.class);
		DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executorProperty);
		Executor innerExecutor = (Executor) executorAccessor.getPropertyValue("executor");
		Object executorBean = context.getBean("taskExecutor");
		assertThat(innerExecutor).isEqualTo(executorBean);
	}

	@Test
	public void channelWithCustomQueue() {
		Object customQueue = context.getBean("customQueue");
		Object channelWithCustomQueue = context.getBean("channelWithCustomQueue");
		assertThat(channelWithCustomQueue.getClass()).isEqualTo(QueueChannel.class);
		Object actualQueue = new DirectFieldAccessor(channelWithCustomQueue).getPropertyValue("queue");
		assertThat(actualQueue).isSameAs(customQueue);
	}

	@Test
	public void testDatatypeChannelWithCorrectType() {
		MessageChannel channel = (MessageChannel) context.getBean("integerChannel");
		assertThat(channel.send(new GenericMessage<Integer>(123))).isTrue();
	}

	@Test
	public void testDatatypeChannelWithIncorrectType() {
		MessageChannel channel = (MessageChannel) context.getBean("integerChannel");
		assertThat(TestUtils.getPropertyValue(channel, "messageConverter") instanceof UselessMessageConverter).isTrue();
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> channel.send(new GenericMessage<>("incorrect type")));
	}

	@Test
	public void testDatatypeChannelGlobalConverter() {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("channelParserGlobalConverterTests.xml", getClass());
		MessageChannel channel = context.getBean("integerChannel", MessageChannel.class);
		context.close();
		assertThat(TestUtils.getPropertyValue(channel, "messageConverter") instanceof UselessMessageConverter).isTrue();
	}

	@Test
	public void testDatatypeChannelWithAssignableSubTypes() {
		MessageChannel channel = (MessageChannel) context.getBean("numberChannel");
		assertThat(channel.send(new GenericMessage<>(123))).isTrue();
		assertThat(channel.send(new GenericMessage<>(123.45))).isTrue();
		assertThat(channel.send(new GenericMessage<>(Boolean.TRUE))).isTrue();
		assertThat(TestUtils.<Object>getPropertyValue(channel, "messageConverter"))
				.isInstanceOf(DefaultDatatypeChannelMessageConverter.class);
		assertThat(TestUtils.<Object>getPropertyValue(channel, "messageConverter.conversionService"))
				.isNotNull();
	}

	@Test
	public void testMultipleDatatypeChannelWithCorrectTypes() {
		MessageChannel channel = (MessageChannel) context.getBean("stringOrNumberChannel");
		assertThat(channel.send(new GenericMessage<>(123))).isTrue();
		assertThat(channel.send(new GenericMessage<>("accepted type"))).isTrue();
	}

	@Test
	public void testMultipleDatatypeChannelWithIncorrectType() {
		MessageChannel channel = (MessageChannel) context.getBean("stringOrNumberChannel");
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> channel.send(new GenericMessage<>(Boolean.TRUE)));
	}

	@Test
	public void testChannelInteceptorRef() {
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("channelInterceptorParserTests.xml", getClass());
		PollableChannel channel = (PollableChannel) context.getBean("channelWithInterceptorRef");
		TestChannelInterceptor interceptor = (TestChannelInterceptor) context.getBean("interceptor");
		assertThat(interceptor.getSendCount()).isEqualTo(0);
		channel.send(new GenericMessage<>("test"));
		assertThat(interceptor.getSendCount()).isEqualTo(1);
		assertThat(interceptor.getReceiveCount()).isEqualTo(0);
		channel.receive();
		assertThat(interceptor.getReceiveCount()).isEqualTo(1);
		context.close();
	}

	@Test
	public void testChannelInterceptorInnerBean() {
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("channelInterceptorParserTests.xml", getClass());
		PollableChannel channel = (PollableChannel) context.getBean("channelWithInterceptorInnerBean");
		channel.send(new GenericMessage<>("test"));
		Message<?> transformed = channel.receive(1000);
		assertThat(transformed.getPayload()).isEqualTo("TEST");
		context.close();
	}

	@Test
	public void testPriorityChannelWithDefaultComparator() {
		PollableChannel channel = this.context.getBean("priorityChannelWithDefaultComparator", PollableChannel.class);
		Message<String> lowPriorityMessage = MessageBuilder.withPayload("low").setPriority(-14).build();
		Message<String> midPriorityMessage = MessageBuilder.withPayload("mid").setPriority(0).build();
		Message<String> highPriorityMessage = MessageBuilder.withPayload("high").setPriority(99).build();
		channel.send(lowPriorityMessage);
		channel.send(highPriorityMessage);
		channel.send(midPriorityMessage);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		Message<?> reply3 = channel.receive(0);
		assertThat(reply1.getPayload()).isEqualTo("high");
		assertThat(reply2.getPayload()).isEqualTo("mid");
		assertThat(reply3.getPayload()).isEqualTo("low");
	}

	@Test
	public void testPriorityChannelWithCustomComparator() {
		PollableChannel channel = this.context.getBean("priorityChannelWithCustomComparator", PollableChannel.class);
		channel.send(new GenericMessage<>("C"));
		channel.send(new GenericMessage<>("A"));
		channel.send(new GenericMessage<>("D"));
		channel.send(new GenericMessage<>("B"));
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		Message<?> reply3 = channel.receive(0);
		Message<?> reply4 = channel.receive(0);
		assertThat(reply1.getPayload()).isEqualTo("A");
		assertThat(reply2.getPayload()).isEqualTo("B");
		assertThat(reply3.getPayload()).isEqualTo("C");
		assertThat(reply4.getPayload()).isEqualTo("D");
	}

	@Test
	public void testPriorityChannelWithIntegerDatatypeEnforced() {
		PollableChannel channel = this.context.getBean("integerOnlyPriorityChannel", PollableChannel.class);
		channel.send(new GenericMessage<>(3));
		channel.send(new GenericMessage<>(2));
		channel.send(new GenericMessage<>(1));
		assertThat(channel.receive(0).getPayload()).isEqualTo(1);
		assertThat(channel.receive(0).getPayload()).isEqualTo(2);
		assertThat(channel.receive(0).getPayload()).isEqualTo(3);

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> channel.send(new GenericMessage<>("wrong type")))
				.extracting("failedMessage.payload")
				.isEqualTo("wrong type");
	}

	public static class TestInterceptor implements ChannelInterceptor {

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			return MessageBuilder.withPayload(message.getPayload().toString().toUpperCase()).build();
		}

	}

	public static class TestConverter implements Converter<Boolean, Number> {

		@Override
		public Number convert(Boolean source) {
			return source ? 1 : 0;
		}

	}

	public static class UselessMessageConverter implements MessageConverter {

		@Override
		public Object fromMessage(Message<?> message, Class<?> targetClass) {
			return null;
		}

		@Override
		public Message<?> toMessage(Object payload, MessageHeaders header) {
			return null;
		}

	}

}
