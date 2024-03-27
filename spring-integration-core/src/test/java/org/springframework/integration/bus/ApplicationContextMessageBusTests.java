/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.bus;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.support.PeriodicTrigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Andreas Baer
 */
public class ApplicationContextMessageBusTests {

	private TestApplicationContext context;

	@BeforeEach
	void setup() {
		this.context = TestUtils.createTestApplicationContext();
	}

	@AfterEach
	void tearDown() {
		this.context.close();
	}

	@Test
	public void endpointRegistrationWithInputChannelReference() {
		QueueChannel sourceChannel = new QueueChannel();
		QueueChannel targetChannel = new QueueChannel();
		this.context.registerChannel("sourceChannel", sourceChannel);
		this.context.registerChannel("targetChannel", targetChannel);
		Message<String> message = MessageBuilder.withPayload("test")
				.setReplyChannelName("targetChannel").build();
		sourceChannel.send(message);
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			public Object handleRequestMessage(Message<?> message) {
				return message;
			}
		};
		handler.setBeanFactory(this.context);
		handler.afterPropertiesSet();
		PollingConsumer endpoint = new PollingConsumer(sourceChannel, handler);
		endpoint.setBeanFactory(mock(BeanFactory.class));
		this.context.registerEndpoint("testEndpoint", endpoint);
		this.context.refresh();
		Message<?> result = targetChannel.receive(10000);
		assertThat(result.getPayload()).isEqualTo("test");
	}

	@Test
	public void channelsWithoutHandlers() {
		QueueChannel sourceChannel = new QueueChannel();
		this.context.registerChannel("sourceChannel", sourceChannel);
		sourceChannel.send(new GenericMessage<>("test"));
		QueueChannel targetChannel = new QueueChannel();
		this.context.registerChannel("targetChannel", targetChannel);
		this.context.refresh();
		Message<?> result = targetChannel.receive(10);
		assertThat(result).isNull();
	}

	@Test
	public void autoDetectionWithApplicationContext() {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("messageBusTests.xml", this.getClass());
		context.start();
		PollableChannel sourceChannel = (PollableChannel) context.getBean("sourceChannel");
		sourceChannel.send(new GenericMessage<>("test"));
		PollableChannel targetChannel = (PollableChannel) context.getBean("targetChannel");
		Message<?> result = targetChannel.receive(10000);
		assertThat(result.getPayload()).isEqualTo("test");
		context.close();
	}

	@Test
	public void exactlyOneConsumerReceivesPointToPointMessage() {
		QueueChannel inputChannel = new QueueChannel();
		QueueChannel outputChannel1 = new QueueChannel();
		QueueChannel outputChannel2 = new QueueChannel();
		AbstractReplyProducingMessageHandler handler1 = new AbstractReplyProducingMessageHandler() {

			@Override
			public Object handleRequestMessage(Message<?> message) {
				return message;
			}
		};
		AbstractReplyProducingMessageHandler handler2 = new AbstractReplyProducingMessageHandler() {

			@Override
			public Object handleRequestMessage(Message<?> message) {
				return message;
			}
		};
		this.context.registerChannel("input", inputChannel);
		this.context.registerChannel("output1", outputChannel1);
		this.context.registerChannel("output2", outputChannel2);
		handler1.setOutputChannel(outputChannel1);
		handler2.setOutputChannel(outputChannel2);
		PollingConsumer endpoint1 = new PollingConsumer(inputChannel, handler1);
		endpoint1.setBeanFactory(mock(BeanFactory.class));
		PollingConsumer endpoint2 = new PollingConsumer(inputChannel, handler2);
		endpoint2.setBeanFactory(mock(BeanFactory.class));
		this.context.registerEndpoint("testEndpoint1", endpoint1);
		this.context.registerEndpoint("testEndpoint2", endpoint2);
		this.context.refresh();
		inputChannel.send(new GenericMessage<>("testing"));
		Message<?> message1 = outputChannel1.receive(100);
		Message<?> message2 = outputChannel2.receive(0);
		assertThat(message1 == null ^ message2 == null).as("exactly one message should be null").isTrue();
	}

	@Test
	public void bothConsumersReceivePublishSubscribeMessage() throws InterruptedException {
		PublishSubscribeChannel inputChannel = new PublishSubscribeChannel();
		QueueChannel outputChannel1 = new QueueChannel();
		QueueChannel outputChannel2 = new QueueChannel();
		final CountDownLatch latch = new CountDownLatch(2);
		AbstractReplyProducingMessageHandler handler1 = new AbstractReplyProducingMessageHandler() {

			@Override
			public Object handleRequestMessage(Message<?> message) {
				latch.countDown();
				return message;
			}
		};
		AbstractReplyProducingMessageHandler handler2 = new AbstractReplyProducingMessageHandler() {

			@Override
			public Object handleRequestMessage(Message<?> message) {
				latch.countDown();
				return message;
			}
		};
		this.context.registerChannel("input", inputChannel);
		this.context.registerChannel("output1", outputChannel1);
		this.context.registerChannel("output2", outputChannel2);
		handler1.setOutputChannel(outputChannel1);
		handler2.setOutputChannel(outputChannel2);
		EventDrivenConsumer endpoint1 = new EventDrivenConsumer(inputChannel, handler1);
		EventDrivenConsumer endpoint2 = new EventDrivenConsumer(inputChannel, handler2);
		this.context.registerEndpoint("testEndpoint1", endpoint1);
		this.context.registerEndpoint("testEndpoint2", endpoint2);
		this.context.refresh();
		inputChannel.send(new GenericMessage<>("testing"));
		assertThat(latch.await(500, TimeUnit.MILLISECONDS)).isTrue();
		Message<?> message1 = outputChannel1.receive(500);
		Message<?> message2 = outputChannel2.receive(500);
		assertThat(message1).as("both handlers should have replied to the message").isNotNull();
		assertThat(message2).as("both handlers should have replied to the message").isNotNull();
	}

	@Test
	public void errorChannelWithFailedDispatch() throws InterruptedException {
		QueueChannel errorChannel = new QueueChannel();
		QueueChannel outputChannel = new QueueChannel();
		this.context.registerChannel("errorChannel", errorChannel);
		CountDownLatch latch = new CountDownLatch(1);
		SourcePollingChannelAdapter channelAdapter = new SourcePollingChannelAdapter();
		channelAdapter.setSource(new FailingSource(latch));
		PollerMetadata pollerMetadata = new PollerMetadata();
		pollerMetadata.setTrigger(new PeriodicTrigger(Duration.ofSeconds(1)));
		channelAdapter.setOutputChannel(outputChannel);
		this.context.registerEndpoint("testChannel", channelAdapter);
		this.context.refresh();
		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
		Message<?> message = errorChannel.receive(5000);
		assertThat(outputChannel.receive(10)).isNull();
		assertThat(message).as("message should not be null").isNotNull();
		assertThat(message instanceof ErrorMessage).isTrue();
		Throwable exception = ((ErrorMessage) message).getPayload();
		assertThat(exception.getCause().getMessage()).isEqualTo("intentional test failure");
	}

	@Test
	public void consumerSubscribedToErrorChannel() throws InterruptedException {
		QueueChannel errorChannel = new QueueChannel();
		this.context.registerChannel(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME, errorChannel);
		final CountDownLatch latch = new CountDownLatch(1);
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			public Object handleRequestMessage(Message<?> message) {
				latch.countDown();
				return null;
			}
		};
		PollingConsumer endpoint = new PollingConsumer(errorChannel, handler);
		endpoint.setBeanFactory(mock(BeanFactory.class));
		this.context.registerEndpoint("testEndpoint", endpoint);
		this.context.refresh();
		errorChannel.send(new ErrorMessage(new RuntimeException("test-exception")));
		assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
	}

	private record FailingSource(CountDownLatch latch) implements MessageSource<Object> {

		@Override
		public Message<Object> receive() {
			latch.countDown();
			throw new RuntimeException("intentional test failure");
		}

	}

}
