/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.integration.amqp.outbound;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.amqp.core.AmqpReplyTimeoutException;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.RabbitMessageFuture;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.listener.adapter.ReplyingMessageListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.amqp.support.NackedAmqpMessageException;
import org.springframework.integration.amqp.support.RabbitTestContainer;
import org.springframework.integration.amqp.support.ReturnedAmqpMessageException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 4.3
 *
 */
class AsyncAmqpGatewayTests implements RabbitTestContainer, TestApplicationContextAware {

	static final String ASYNC_QUEUE = "asyncQ1";

	static final String ASYNC_REPLY_QUEUE = "asyncRQ1";

	@BeforeAll
	static void initQueue() throws IOException, InterruptedException {
		for (String queue : List.of(ASYNC_QUEUE, ASYNC_REPLY_QUEUE)) {
			RABBITMQ.execInContainer("rabbitmqadmin", "declare", "queue", "name=" + queue);
		}
	}

	@AfterAll
	static void deleteQueue() throws IOException, InterruptedException {
		for (String queue : List.of(ASYNC_QUEUE, ASYNC_REPLY_QUEUE)) {
			RABBITMQ.execInContainer("rabbitmqadmin", "delete", "queue", "name=" + queue);
		}
	}

	@Test
	void testConfirmsAndReturns() throws Exception {
		CachingConnectionFactory ccf = new CachingConnectionFactory(RabbitTestContainer.amqpPort());
		ccf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
		ccf.setPublisherReturns(true);
		RabbitTemplate template = new RabbitTemplate(ccf);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(ccf);
		container.setBeanName("replyContainer");
		container.setQueueNames(ASYNC_REPLY_QUEUE);
		container.afterPropertiesSet();
		container.start();
		AsyncRabbitTemplate asyncTemplate = new AsyncRabbitTemplate(template, container);
		asyncTemplate.setEnableConfirms(true);
		asyncTemplate.setMandatory(true);

		SimpleMessageListenerContainer receiver = new SimpleMessageListenerContainer(ccf);
		receiver.setBeanName("receiver");
		receiver.setQueueNames(ASYNC_QUEUE);
		final CountDownLatch waitForAckBeforeReplying = new CountDownLatch(1);
		MessageListenerAdapter messageListener = new MessageListenerAdapter(
				(ReplyingMessageListener<String, String>) foo -> {
					try {
						waitForAckBeforeReplying.await(10, TimeUnit.SECONDS);
					}
					catch (@SuppressWarnings("unused") InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return foo.toUpperCase();
				});
		receiver.setMessageListener(messageListener);
		receiver.afterPropertiesSet();
		receiver.start();

		AsyncAmqpOutboundGateway gateway = new AsyncAmqpOutboundGateway(asyncTemplate);
		LogAccessor logger = spy((LogAccessor) TestUtils.getPropertyValue(gateway, "logger"));
		given(logger.isDebugEnabled()).willReturn(true);
		final CountDownLatch replyTimeoutLatch = new CountDownLatch(1);
		willAnswer(invocation -> {
			invocation.callRealMethod();
			replyTimeoutLatch.countDown();
			return null;
		}).given(logger)
				.debug(ArgumentMatchers.<Supplier<String>>argThat(logMessage ->
						logMessage.get().startsWith("Reply not required and async timeout for")));
		new DirectFieldAccessor(gateway).setPropertyValue("logger", logger);
		QueueChannel outputChannel = new QueueChannel();
		outputChannel.setBeanName("output");
		QueueChannel returnChannel = new QueueChannel();
		returnChannel.setBeanName("returns");
		QueueChannel ackChannel = new QueueChannel();
		ackChannel.setBeanName("acks");
		QueueChannel errorChannel = new QueueChannel();
		errorChannel.setBeanName("errors");
		gateway.setOutputChannel(outputChannel);
		gateway.setReturnChannel(returnChannel);
		gateway.setConfirmAckChannel(ackChannel);
		gateway.setConfirmNackChannel(ackChannel);
		gateway.setConfirmCorrelationExpressionString("#this");
		gateway.setExchangeName("");
		gateway.setRoutingKey("asyncQ1");
		gateway.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		gateway.afterPropertiesSet();
		gateway.start();

		Message<?> message = MessageBuilder.withPayload("foo").setErrorChannel(errorChannel).build();

		gateway.handleMessage(message);

		Message<?> ack = ackChannel.receive(10000);
		assertThat(ack).isNotNull();
		assertThat(ack.getPayload()).isEqualTo("foo");
		assertThat(ack.getHeaders().get(AmqpHeaders.PUBLISH_CONFIRM)).isEqualTo(true);
		waitForAckBeforeReplying.countDown();

		Message<?> received = outputChannel.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("FOO");

		// timeout tests
		asyncTemplate.setReceiveTimeout(10);

		receiver.setMessageListener(message1 -> {
		});
		// reply timeout with no requiresReply
		message = MessageBuilder.withPayload("bar").setErrorChannel(errorChannel).build();
		gateway.handleMessage(message);
		assertThat(replyTimeoutLatch.await(10, TimeUnit.SECONDS)).isTrue();

		// reply timeout with requiresReply
		gateway.setRequiresReply(true);
		message = MessageBuilder.withPayload("baz").setErrorChannel(errorChannel).build();
		gateway.handleMessage(message);

		received = errorChannel.receive(10000);
		assertThat(received).isInstanceOf(ErrorMessage.class);
		ErrorMessage error = (ErrorMessage) received;
		assertThat(error.getPayload()).isInstanceOf(MessagingException.class);
		assertThat(error.getPayload().getCause()).isInstanceOf(AmqpReplyTimeoutException.class);
		asyncTemplate.setReceiveTimeout(30000);
		receiver.setMessageListener(messageListener);

		// error on sending result
		DirectChannel errorForce = new DirectChannel();
		errorForce.setBeanName("errorForce");
		errorForce.subscribe(message1 -> {
			throw new RuntimeException("intentional");
		});
		gateway.setOutputChannel(errorForce);
		message = MessageBuilder.withPayload("qux").setErrorChannel(errorChannel).build();
		gateway.handleMessage(message);
		received = errorChannel.receive(10000);
		assertThat(received).isInstanceOf(ErrorMessage.class);
		error = (ErrorMessage) received;
		assertThat(error.getPayload()).isInstanceOf(MessagingException.class);
		assertThat(((MessagingException) error.getPayload()).getFailedMessage().getPayload()).isEqualTo("QUX");

		gateway.setRoutingKey(UUID.randomUUID().toString());
		message = MessageBuilder.withPayload("fiz").setErrorChannel(errorChannel).build();
		gateway.handleMessage(message);
		Message<?> returned = returnChannel.receive(10000);
		assertThat(returned).isNotNull();
		assertThat(returned).isInstanceOf(ErrorMessage.class);
		assertThat(returned.getPayload()).isInstanceOf(ReturnedAmqpMessageException.class);
		ReturnedAmqpMessageException payload = (ReturnedAmqpMessageException) returned.getPayload();
		assertThat(payload.getFailedMessage().getPayload()).isEqualTo("fiz");
		ackChannel.receive(10000);
		ackChannel.purge(null);

		RabbitMessageFuture future = mock(RabbitMessageFuture.class);
		willReturn("nacknack").given(future).getNackCause();
		willReturn(CompletableFuture.completedFuture(false)).given(future).getConfirm();

		asyncTemplate = mock(AsyncRabbitTemplate.class);
		willReturn(future).given(asyncTemplate).sendAndReceive(anyString(), anyString(),
				any(org.springframework.amqp.core.Message.class));
		new DirectFieldAccessor(gateway).setPropertyValue("template", asyncTemplate);

		message = MessageBuilder.withPayload("buz").setErrorChannel(errorChannel).build();
		gateway.handleMessage(message);

		ack = ackChannel.receive(10000);
		assertThat(ack).isNotNull();
		assertThat(returned).isInstanceOf(ErrorMessage.class);
		assertThat(returned.getPayload()).isInstanceOf(ReturnedAmqpMessageException.class);
		NackedAmqpMessageException nack = (NackedAmqpMessageException) ack.getPayload();
		assertThat(nack.getFailedMessage().getPayload()).isEqualTo("buz");
		assertThat(nack.getNackReason()).isEqualTo("nacknack");

		asyncTemplate.stop();
		receiver.stop();
		ccf.destroy();
	}

	@Test
	void confirmsAndReturnsNoChannels() throws Exception {
		CachingConnectionFactory ccf = new CachingConnectionFactory(RabbitTestContainer.amqpPort());
		ccf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
		ccf.setPublisherReturns(true);
		RabbitTemplate template = new RabbitTemplate(ccf);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(ccf);
		container.setBeanName("replyContainer");
		container.setQueueNames(ASYNC_REPLY_QUEUE);
		container.afterPropertiesSet();
		container.start();
		AsyncRabbitTemplate asyncTemplate = new AsyncRabbitTemplate(template, container);
		asyncTemplate.setEnableConfirms(true);
		asyncTemplate.setMandatory(true);

		AsyncAmqpOutboundGateway gateway = new AsyncAmqpOutboundGateway(asyncTemplate);
		gateway.setOutputChannel(new NullChannel());
		gateway.setExchangeName("");
		gateway.setRoutingKey("noRoute");
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();
		gateway.start();

		CorrelationData corr = new CorrelationData("foo");
		gateway.handleMessage(MessageBuilder.withPayload("test")
				.setHeader(AmqpHeaders.PUBLISH_CONFIRM_CORRELATION, corr)
				.build());
		assertThat(corr.getFuture().get(10, TimeUnit.SECONDS).ack()).isTrue();
		assertThat(corr.getReturned()).isNotNull();

		asyncTemplate.stop();
		ccf.destroy();
	}

}
