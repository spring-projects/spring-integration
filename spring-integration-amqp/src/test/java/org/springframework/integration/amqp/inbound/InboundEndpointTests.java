/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.integration.amqp.inbound;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.rabbitmq.client.Channel;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.MessageListenerContainer;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.listener.ListenerExecutionFailedException;
import org.springframework.amqp.rabbit.batch.MessageBatch;
import org.springframework.amqp.rabbit.batch.SimpleBatchingStrategy;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareBatchMessageListener;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.retry.MessageBatchRecoverer;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter.BatchMode;
import org.springframework.integration.amqp.support.AmqpMessageHeaderErrorMessageStrategy;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.amqp.support.ManualAckListenerExecutionFailedException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.Transformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
public class InboundEndpointTests implements TestApplicationContextAware {

	@Test
	public void testInt2809JavaTypePropertiesToAmqp() throws Exception {
		Connection connection = mock();
		willReturn(mock(Channel.class)).given(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock();
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setAcknowledgeMode(AcknowledgeMode.MANUAL);

		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);
		adapter.setMessageConverter(new JacksonJsonMessageConverter());

		PollableChannel channel = new QueueChannel();

		adapter.setOutputChannel(channel);
		adapter.setBeanFactory(mock());
		adapter.setBindSourceMessage(true);
		adapter.afterPropertiesSet();

		Object payload = new Foo("bar1");

		Transformer objectToJsonTransformer = new ObjectToJsonTransformer();
		Message<?> jsonMessage = objectToJsonTransformer.transform(new GenericMessage<>(payload));

		MessageProperties amqpMessageProperties = new MessageProperties();
		amqpMessageProperties.setDeliveryTag(123L);
		org.springframework.amqp.core.Message amqpMessage =
				new SimpleMessageConverter().toMessage(jsonMessage.getPayload(), amqpMessageProperties);
		DefaultAmqpHeaderMapper.inboundMapper().fromHeadersToRequest(jsonMessage.getHeaders(), amqpMessageProperties);

		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		Channel rabbitChannel = mock(Channel.class);
		listener.onMessage(amqpMessage, rabbitChannel);

		Message<?> result = channel.receive(1000);
		assertThat(result.getPayload()).isEqualTo(payload);

		assertThat(result.getHeaders())
				.containsEntry(AmqpHeaders.CHANNEL, rabbitChannel)
				.containsEntry(AmqpHeaders.DELIVERY_TAG, 123L);
		org.springframework.amqp.core.Message sourceData = StaticMessageHeaderAccessor.getSourceData(result);
		assertThat(sourceData).isSameAs(amqpMessage);
	}

	@Test
	public void testInt2809JavaTypePropertiesFromAmqp() throws Exception {
		Connection connection = mock();
		willReturn(mock(Channel.class)).given(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock();
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);

		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);

		PollableChannel channel = new QueueChannel();

		adapter.setOutputChannel(channel);
		adapter.setBeanFactory(mock());
		adapter.afterPropertiesSet();

		Object payload = new Foo("bar1");

		MessageProperties amqpMessageProperties = new MessageProperties();
		org.springframework.amqp.core.Message amqpMessage =
				new JacksonJsonMessageConverter()
						.toMessage(payload, amqpMessageProperties);

		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		listener.onMessage(amqpMessage, null);

		Message<?> receive = channel.receive(1000);

		Message<?> result = new JsonToObjectTransformer().transform(receive);

		assertThat(result.getPayload()).isEqualTo(payload);
		org.springframework.amqp.core.Message sourceData = StaticMessageHeaderAccessor.getSourceData(result);
		assertThat(sourceData).isNull();
	}

	@Test
	public void testMessageConverterJsonHeadersHavePrecedenceOverMessageHeaders() throws Exception {
		Connection connection = mock();
		willReturn(mock(Channel.class)).given(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock();
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setAcknowledgeMode(AcknowledgeMode.MANUAL);

		DirectChannel channel = new DirectChannel();

		Channel rabbitChannel = mock();

		channel.subscribe(new MessageTransformingHandler(message -> {
			assertThat(message.getHeaders().get(AmqpHeaders.CHANNEL)).isSameAs(rabbitChannel);
			assertThat(message.getHeaders().get(AmqpHeaders.DELIVERY_TAG)).isEqualTo(123L);
			return MessageBuilder.fromMessage(message)
					.setHeader(JsonHeaders.TYPE_ID, "foo")
					.setHeader(JsonHeaders.CONTENT_TYPE_ID, "bar")
					.setHeader(JsonHeaders.KEY_TYPE_ID, "baz")
					.build();
		}));

		RabbitTemplate rabbitTemplate = spy(new RabbitTemplate());
		rabbitTemplate.setMessageConverter(new JacksonJsonMessageConverter());

		CountDownLatch sendLatch = new CountDownLatch(1);

		Mockito.doAnswer(invocation -> {
					org.springframework.amqp.core.Message message =
							invocation.getArgument(2);
					Map<String, Object> headers = message.getMessageProperties().getHeaders();
					assertThat(headers)
							.doesNotContainKeys(JsonHeaders.CONTENT_TYPE_ID.replaceFirst(JsonHeaders.PREFIX, ""),
									JsonHeaders.KEY_TYPE_ID.replaceFirst(JsonHeaders.PREFIX, ""),
									JsonHeaders.TYPE_ID,
									JsonHeaders.KEY_TYPE_ID,
									JsonHeaders.CONTENT_TYPE_ID)
							.containsEntry(JsonHeaders.TYPE_ID.replaceFirst(JsonHeaders.PREFIX, ""),
									InboundEndpointTests.Foo.class.getName());
					sendLatch.countDown();
					return null;
				}).when(rabbitTemplate)
				.send(anyString(), anyString(), any(org.springframework.amqp.core.Message.class), isNull());

		AmqpInboundGateway gateway = new AmqpInboundGateway((MessageListenerContainer) container, rabbitTemplate);
		gateway.setMessageConverter(new JacksonJsonMessageConverter());
		gateway.setRequestChannel(channel);
		gateway.setBeanFactory(mock());
		gateway.setDefaultReplyTo("foo");
		gateway.setReplyHeadersMappedLast(true);
		gateway.afterPropertiesSet();

		Object payload = new Foo("bar1");

		MessageProperties amqpMessageProperties = new MessageProperties();
		amqpMessageProperties.setDeliveryTag(123L);
		org.springframework.amqp.core.Message amqpMessage =
				new JacksonJsonMessageConverter()
						.toMessage(payload, amqpMessageProperties);

		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		listener.onMessage(amqpMessage, rabbitChannel);

		assertThat(sendLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testAdapterConversionError() throws Exception {
		Connection connection = mock();
		willReturn(mock(Channel.class)).given(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock();
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		adapter.setMessageConverter(new SimpleMessageConverter() {

			@Override
			public Object fromMessage(org.springframework.amqp.core.Message message) throws MessageConversionException {
				throw new MessageConversionException("intended");
			}

		});
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		org.springframework.amqp.core.Message message = mock();
		MessageProperties props = new MessageProperties();
		props.setDeliveryTag(42L);
		given(message.getMessageProperties()).willReturn(props);
		((ChannelAwareMessageListener) container.getMessageListener())
				.onMessage(message, null);
		assertThat(outputChannel.receive(0)).isNull();
		Message<?> received = errorChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders().get(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE)).isNotNull();
		assertThat(received.getPayload()).isInstanceOf(ListenerExecutionFailedException.class);

		container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
		adapter.afterPropertiesSet(); // ack mode is now captured during init
		Channel channel = mock();
		((ChannelAwareMessageListener) container.getMessageListener())
				.onMessage(message, channel);
		assertThat(outputChannel.receive(0)).isNull();
		received = errorChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders()).containsKey(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE);
		assertThat(received.getPayload()).isInstanceOf(ManualAckListenerExecutionFailedException.class);
		ManualAckListenerExecutionFailedException ex = (ManualAckListenerExecutionFailedException) received
				.getPayload();
		assertThat(ex.getChannel()).isEqualTo(channel);
		assertThat(ex.getDeliveryTag()).isEqualTo(props.getDeliveryTag());
	}

	@Test
	public void testGatewayConversionError() throws Exception {
		Connection connection = mock();
		willReturn(mock(Channel.class)).given(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock();
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		AmqpInboundGateway adapter = new AmqpInboundGateway(container);
		QueueChannel outputChannel = new QueueChannel();
		adapter.setRequestChannel(outputChannel);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		adapter.setMessageConverter(new MessageConverter() {

			@Override
			public org.springframework.amqp.core.Message toMessage(Object object, MessageProperties messageProperties)
					throws MessageConversionException {
				throw new MessageConversionException("intended");
			}

			@Override
			public Object fromMessage(org.springframework.amqp.core.Message message) throws MessageConversionException {
				throw new MessageConversionException("intended");
			}

		});
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		org.springframework.amqp.core.Message message = mock();
		MessageProperties props = new MessageProperties();
		props.setDeliveryTag(42L);
		given(message.getMessageProperties()).willReturn(props);
		((ChannelAwareMessageListener) container.getMessageListener())
				.onMessage(message, null);
		assertThat(outputChannel.receive(0)).isNull();
		Message<?> received = errorChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders()).containsKey(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE);

		container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
		Channel channel = mock();
		((ChannelAwareMessageListener) container.getMessageListener())
				.onMessage(message, channel);
		assertThat(outputChannel.receive(0)).isNull();
		received = errorChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders()).containsKey(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE);
		assertThat(received.getPayload()).isInstanceOf(ManualAckListenerExecutionFailedException.class);
		ManualAckListenerExecutionFailedException ex = (ManualAckListenerExecutionFailedException) received
				.getPayload();
		assertThat(ex.getChannel()).isEqualTo(channel);
		assertThat(ex.getDeliveryTag()).isEqualTo(props.getDeliveryTag());
	}

	@Test
	public void testRetryWithinOnMessageAdapter() throws Exception {
		ConnectionFactory connectionFactory = mock();
		AbstractMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);
		adapter.setOutputChannel(new DirectChannel());
		adapter.setRetryTemplate(
				new RetryTemplate(RetryPolicy.builder()
						.maxRetries(2)
						.delay(Duration.ZERO)
						.build()));
		QueueChannel errors = new QueueChannel();
		ErrorMessageSendingRecoverer recoveryCallback = new ErrorMessageSendingRecoverer(errors);
		recoveryCallback.setErrorMessageStrategy(new AmqpMessageHeaderErrorMessageStrategy());
		adapter.setRecoveryCallback(recoveryCallback);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		listener.onMessage(org.springframework.amqp.core.MessageBuilder.withBody("foo".getBytes())
				.andProperties(new MessageProperties()).build(), null);
		Message<?> errorMessage = errors.receive(0);
		assertThat(errorMessage).extracting(Message::getPayload).isInstanceOf(MessagingException.class);
		MessagingException payload = (MessagingException) errorMessage.getPayload();
		assertThat(payload.getCause()).isInstanceOf(RetryException.class);
		payload = (MessagingException) payload.getCause().getCause();
		assertThat(payload.getMessage()).contains("Dispatcher has no");
		assertThat(StaticMessageHeaderAccessor.getDeliveryAttempt(payload.getFailedMessage()).get()).isEqualTo(3);
		org.springframework.amqp.core.Message amqpMessage = errorMessage.getHeaders()
				.get(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE,
						org.springframework.amqp.core.Message.class);
		assertThat(amqpMessage).isNotNull();
		assertThat(errors.receive(0)).isNull();
	}

	@Test
	public void testRetryWithMessageRecovererOnMessageAdapter() throws Exception {
		ConnectionFactory connectionFactory = mock();
		AbstractMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);
		adapter.setOutputChannel(new DirectChannel());
		adapter.setRetryTemplate(
				new RetryTemplate(RetryPolicy.builder()
						.maxRetries(2)
						.delay(Duration.ZERO)
						.build()));
		AtomicReference<org.springframework.amqp.core.Message> recoveredMessage = new AtomicReference<>();
		AtomicReference<Throwable> recoveredError = new AtomicReference<>();
		CountDownLatch recoveredLatch = new CountDownLatch(1);
		adapter.setMessageRecoverer((message, cause) -> {
			recoveredMessage.set(message);
			recoveredError.set(cause);
			recoveredLatch.countDown();
		});
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		org.springframework.amqp.core.Message amqpMessage =
				org.springframework.amqp.core.MessageBuilder.withBody("foo".getBytes())
						.andProperties(new MessageProperties())
						.build();
		listener.onMessage(amqpMessage, null);

		assertThat(recoveredLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(recoveredError.get())
				.isInstanceOf(RetryException.class)
				.cause()
				.isInstanceOf(MessagingException.class)
				.extracting(Throwable::getMessage, InstanceOfAssertFactories.STRING)
				.contains("Dispatcher has no");

		assertThat(recoveredMessage.get()).isSameAs(amqpMessage);
	}

	@Test
	public void testRetryWithinOnMessageGateway() throws Exception {
		ConnectionFactory connectionFactory = mock();
		AbstractMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
		AmqpInboundGateway adapter = new AmqpInboundGateway(container);
		adapter.setRequestChannel(new DirectChannel());
		adapter.setRetryTemplate(
				new RetryTemplate(RetryPolicy.builder()
						.maxRetries(2)
						.delay(Duration.ZERO)
						.build()));
		QueueChannel errors = new QueueChannel();
		ErrorMessageSendingRecoverer recoveryCallback = new ErrorMessageSendingRecoverer(errors);
		recoveryCallback.setErrorMessageStrategy(new AmqpMessageHeaderErrorMessageStrategy());
		adapter.setRecoveryCallback(recoveryCallback);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		listener.onMessage(org.springframework.amqp.core.MessageBuilder.withBody("foo".getBytes())
				.andProperties(new MessageProperties()).build(), null);
		Message<?> errorMessage = errors.receive(0);
		assertThat(errorMessage)
				.extracting(Message::getPayload)
				.isInstanceOf(MessagingException.class)
				.extracting("cause.cause.message")
				.asString()
				.contains("Dispatcher has no");
		assertThat(errorMessage)
				.extracting("payload.cause.cause.failedMessage")
				.asInstanceOf(type(Message.class))
				.extracting((message) -> StaticMessageHeaderAccessor.getDeliveryAttempt(message).get())
				.isEqualTo(3);
		org.springframework.amqp.core.Message amqpMessage = errorMessage.getHeaders()
				.get(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE,
						org.springframework.amqp.core.Message.class);
		assertThat(amqpMessage).isNotNull();
		assertThat(errors.receive(0)).isNull();
	}

	@Test
	public void testRetryWithMessageRecovererOnMessageGateway() throws Exception {
		ConnectionFactory connectionFactory = mock();
		AbstractMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
		AmqpInboundGateway adapter = new AmqpInboundGateway(container);
		adapter.setRequestChannel(new DirectChannel());
		adapter.setRetryTemplate(
				new RetryTemplate(
						RetryPolicy.builder()
								.maxRetries(2)
								.delay(Duration.ZERO)
								.build()));
		AtomicReference<org.springframework.amqp.core.Message> recoveredMessage = new AtomicReference<>();
		AtomicReference<Throwable> recoveredError = new AtomicReference<>();
		CountDownLatch recoveredLatch = new CountDownLatch(1);
		adapter.setMessageRecoverer((message, cause) -> {
			recoveredMessage.set(message);
			recoveredError.set(cause);
			recoveredLatch.countDown();
		});
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		org.springframework.amqp.core.Message amqpMessage =
				org.springframework.amqp.core.MessageBuilder.withBody("foo".getBytes())
						.andProperties(new MessageProperties())
						.build();
		listener.onMessage(amqpMessage, null);

		assertThat(recoveredLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(recoveredError.get())
				.isInstanceOf(RetryException.class)
				.cause()
				.isInstanceOf(MessagingException.class)
				.extracting(Throwable::getMessage, InstanceOfAssertFactories.STRING)
				.contains("Dispatcher has no");

		assertThat(recoveredMessage.get()).isSameAs(amqpMessage);
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testBatchAdapter() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(mock());
		container.setDeBatchingEnabled(false);
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		SimpleBatchingStrategy bs = new SimpleBatchingStrategy(2, 10_000, 10_000L);
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		org.springframework.amqp.core.Message message =
				new org.springframework.amqp.core.Message("test1".getBytes(), messageProperties);
		bs.addToBatch("foo", "bar", message);
		message = new org.springframework.amqp.core.Message("test2".getBytes(), messageProperties);
		MessageBatch batched = bs.addToBatch("foo", "bar", message);
		listener.onMessage(batched.message(), null);
		Message<?> received = out.receive(0);
		assertThat(received).isNotNull();
		assertThat(((List<String>) received.getPayload())).contains("test1", "test2");
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testBatchGateway() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(mock());
		container.setDeBatchingEnabled(false);
		AmqpInboundGateway gateway = new AmqpInboundGateway(container);
		QueueChannel out = new QueueChannel();
		gateway.setRequestChannel(out);
		gateway.setBindSourceMessage(true);
		gateway.setReplyTimeout(0);
		gateway.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		gateway.afterPropertiesSet();
		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		SimpleBatchingStrategy bs = new SimpleBatchingStrategy(2, 10_000, 10_000L);
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		org.springframework.amqp.core.Message message =
				new org.springframework.amqp.core.Message("test1".getBytes(), messageProperties);
		bs.addToBatch("foo", "bar", message);
		message = new org.springframework.amqp.core.Message("test2".getBytes(), messageProperties);
		MessageBatch batched = bs.addToBatch("foo", "bar", message);
		listener.onMessage(batched.message(), null);
		Message<?> received = out.receive(0);
		assertThat(received).isNotNull();
		assertThat(((List<String>) received.getPayload())).contains("test1", "test2");
		org.springframework.amqp.core.Message sourceData = StaticMessageHeaderAccessor.getSourceData(received);
		assertThat(sourceData).isSameAs(batched.message());
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testConsumerBatchExtract() {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(mock());
		container.setConsumerBatchEnabled(true);
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBatchMode(BatchMode.EXTRACT_PAYLOADS_WITH_HEADERS);
		adapter.setHeaderNameForBatchedHeaders("some_batch_headers");
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		ChannelAwareBatchMessageListener listener = (ChannelAwareBatchMessageListener) container.getMessageListener();
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		List<org.springframework.amqp.core.Message> messages = new ArrayList<>();
		messages.add(new org.springframework.amqp.core.Message("test1".getBytes(), messageProperties));
		messages.add(new org.springframework.amqp.core.Message("test2".getBytes(), messageProperties));
		listener.onMessageBatch(messages, null);
		Message<?> received = out.receive(0);
		assertThat(received).isNotNull();
		assertThat(((List<String>) received.getPayload())).contains("test1", "test2");
		assertThat(received.getHeaders().get("some_batch_headers", List.class))
				.hasSize(2);
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testConsumerBatch() {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(mock());
		container.setConsumerBatchEnabled(true);
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		ChannelAwareBatchMessageListener listener = (ChannelAwareBatchMessageListener) container.getMessageListener();
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		List<org.springframework.amqp.core.Message> messages = new ArrayList<>();
		messages.add(new org.springframework.amqp.core.Message("test1".getBytes(), messageProperties));
		messages.add(new org.springframework.amqp.core.Message("test2".getBytes(), messageProperties));
		listener.onMessageBatch(messages, null);
		Message<?> received = out.receive(0);
		assertThat(received).isNotNull();
		assertThat(((List<Message<String>>) received.getPayload()))
				.extracting(message -> message.getPayload())
				.contains("test1", "test2");
	}

	@Test
	public void testConsumerBatchAndWrongMessageRecoverer() {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(mock());
		container.setConsumerBatchEnabled(true);
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.setRetryTemplate(new RetryTemplate());
		adapter.setMessageRecoverer((message, cause) -> {
		});
		assertThatIllegalArgumentException()
				.isThrownBy(adapter::afterPropertiesSet)
				.withMessageStartingWith("The 'messageRecoverer' must be an instance of MessageBatchRecoverer " +
						"when consumer configured for batch mode");
	}

	@Test
	public void testExclusiveRecover() {
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(mock(MessageListenerContainer.class));
		adapter.setRetryTemplate(new RetryTemplate());
		adapter.setMessageRecoverer((message, cause) -> {
		});
		adapter.setRecoveryCallback((context, cause) -> null);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThatIllegalStateException()
				.isThrownBy(adapter::afterPropertiesSet)
				.withMessageStartingWith("Only one of 'recoveryCallback' or 'messageRecoverer' may be provided, " +
						"but not both");
	}

	@Test
	public void testAdapterConversionErrorConsumerBatchExtract() {
		Connection connection = mock();
		willReturn(mock(Channel.class)).given(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock();
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setConsumerBatchEnabled(true);
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		adapter.setMessageConverter(new SimpleMessageConverter() {

			@Override
			public Object fromMessage(org.springframework.amqp.core.Message message) throws MessageConversionException {
				throw new MessageConversionException("intended");
			}

		});
		adapter.setBatchMode(BatchMode.EXTRACT_PAYLOADS);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		messageProperties.setDeliveryTag(42L);
		List<org.springframework.amqp.core.Message> messages = new ArrayList<>();
		messages.add(new org.springframework.amqp.core.Message("test1".getBytes(), messageProperties));
		messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		messageProperties.setDeliveryTag(43L);
		messages.add(new org.springframework.amqp.core.Message("test2".getBytes(), messageProperties));
		((ChannelAwareBatchMessageListener) container.getMessageListener())
				.onMessageBatch(messages, null);
		assertThat(outputChannel.receive(0)).isNull();
		Message<?> received = errorChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders()).containsKey(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE);
		assertThat(received.getPayload()).isInstanceOf(ListenerExecutionFailedException.class);

		container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
		adapter.afterPropertiesSet(); // ack mode is now captured during init
		Channel channel = mock(Channel.class);
		((ChannelAwareBatchMessageListener) container.getMessageListener())
				.onMessageBatch(messages, channel);
		assertThat(outputChannel.receive(0)).isNull();
		received = errorChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders()).containsKey(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE);
		assertThat(received.getPayload()).isInstanceOf(ManualAckListenerExecutionFailedException.class);
		ManualAckListenerExecutionFailedException ex = (ManualAckListenerExecutionFailedException) received
				.getPayload();
		assertThat(ex.getChannel()).isEqualTo(channel);
		assertThat(ex.getDeliveryTag()).isEqualTo(43L);
	}

	@Test
	public void testAdapterConversionErrorConsumerBatch() {
		Connection connection = mock();
		willReturn(mock(Channel.class)).given(connection).createChannel(anyBoolean());
		ConnectionFactory connectionFactory = mock();
		when(connectionFactory.createConnection()).thenReturn(connection);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setConsumerBatchEnabled(true);
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		adapter.setMessageConverter(new SimpleMessageConverter() {

			@Override
			public Object fromMessage(org.springframework.amqp.core.Message message) throws MessageConversionException {
				throw new MessageConversionException("intended");
			}

		});
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		messageProperties.setDeliveryTag(42L);
		List<org.springframework.amqp.core.Message> messages = new ArrayList<>();
		messages.add(new org.springframework.amqp.core.Message("test1".getBytes(), messageProperties));
		messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		messageProperties.setDeliveryTag(43L);
		messages.add(new org.springframework.amqp.core.Message("test2".getBytes(), messageProperties));
		((ChannelAwareBatchMessageListener) container.getMessageListener())
				.onMessageBatch(messages, null);
		assertThat(outputChannel.receive(0)).isNull();
		Message<?> received = errorChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders()).containsKey(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE);
		assertThat(received.getPayload()).isInstanceOf(ListenerExecutionFailedException.class);

		container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
		adapter.afterPropertiesSet(); // ack mode is now captured during init
		Channel channel = mock(Channel.class);
		((ChannelAwareBatchMessageListener) container.getMessageListener())
				.onMessageBatch(messages, channel);
		assertThat(outputChannel.receive(0)).isNull();
		received = errorChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getHeaders()).containsKey(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE);
		assertThat(received.getPayload()).isInstanceOf(ManualAckListenerExecutionFailedException.class);
		ManualAckListenerExecutionFailedException ex = (ManualAckListenerExecutionFailedException) received
				.getPayload();
		assertThat(ex.getChannel()).isEqualTo(channel);
		assertThat(ex.getDeliveryTag()).isEqualTo(43L);
	}

	@Test
	public void testRetryWithinOnMessageAdapterConsumerBatch() {
		ConnectionFactory connectionFactory = mock();
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
		container.setConsumerBatchEnabled(true);
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);
		adapter.setOutputChannel(new DirectChannel());
		adapter.setRetryTemplate(
				new RetryTemplate(
						RetryPolicy.builder()
								.maxRetries(2)
								.delay(Duration.ZERO)
								.build()));
		QueueChannel errors = new QueueChannel();
		ErrorMessageSendingRecoverer recoveryCallback = new ErrorMessageSendingRecoverer(errors);
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		recoveryCallback.setErrorMessageStrategy(new AmqpMessageHeaderErrorMessageStrategy());
		adapter.setRecoveryCallback(recoveryCallback);
		adapter.afterPropertiesSet();
		ChannelAwareBatchMessageListener listener = (ChannelAwareBatchMessageListener) container.getMessageListener();
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		messageProperties.setDeliveryTag(42L);
		List<org.springframework.amqp.core.Message> messages = new ArrayList<>();
		messages.add(new org.springframework.amqp.core.Message("test1".getBytes(), messageProperties));
		messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		messageProperties.setDeliveryTag(43L);
		messages.add(new org.springframework.amqp.core.Message("test2".getBytes(), messageProperties));
		listener.onMessageBatch(messages, null);
		Message<?> errorMessage = errors.receive(0);
		assertThat(errorMessage).extracting(Message::getPayload).isInstanceOf(MessagingException.class);
		MessagingException payload = (MessagingException) errorMessage.getPayload();
		assertThat(payload.getCause()).isInstanceOf(RetryException.class);
		payload = (MessagingException) payload.getCause().getCause();
		assertThat(payload.getMessage()).contains("Dispatcher has no");
		assertThat(StaticMessageHeaderAccessor.getDeliveryAttempt(payload.getFailedMessage()).get()).isEqualTo(3);
		@SuppressWarnings("unchecked")
		List<org.springframework.amqp.core.Message> amqpMessages = errorMessage.getHeaders()
				.get(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE,
						List.class);
		assertThat(amqpMessages).hasSize(2);
		@SuppressWarnings("unchecked")
		List<Message<?>> msgs = (List<Message<?>>) payload.getFailedMessage().getPayload();
		assertThat(msgs).hasSize(2);
		assertThat(msgs).extracting(msg -> StaticMessageHeaderAccessor.getDeliveryAttempt(msg).get())
				.contains(3, 3);
		assertThat(msgs).extracting(msg -> msg.getHeaders().get(AmqpHeaders.DELIVERY_TAG, Long.class))
				.contains(42L, 43L);
		assertThat(errors.receive(0)).isNull();
	}

	@Test
	public void testRetryWithMessageRecovererOnMessageAdapterConsumerBatch() throws InterruptedException {
		ConnectionFactory connectionFactory = mock();
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
		container.setConsumerBatchEnabled(true);
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter((MessageListenerContainer) container);
		adapter.setOutputChannel(new DirectChannel());
		adapter.setRetryTemplate(
				new RetryTemplate(
						RetryPolicy.builder()
								.maxRetries(2)
								.delay(Duration.ZERO)
								.build()));
		AtomicReference<List<org.springframework.amqp.core.Message>> recoveredMessages = new AtomicReference<>();
		AtomicReference<Throwable> recoveredError = new AtomicReference<>();
		CountDownLatch recoveredLatch = new CountDownLatch(1);
		adapter.setMessageRecoverer((MessageBatchRecoverer) (messages, cause) -> {
			recoveredMessages.set(messages);
			recoveredError.set(cause);
			recoveredLatch.countDown();
		});
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		adapter.afterPropertiesSet();
		ChannelAwareBatchMessageListener listener = (ChannelAwareBatchMessageListener) container.getMessageListener();
		MessageProperties messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		messageProperties.setDeliveryTag(42L);
		List<org.springframework.amqp.core.Message> messages = new ArrayList<>();
		messages.add(new org.springframework.amqp.core.Message("test1".getBytes(), messageProperties));
		messageProperties = new MessageProperties();
		messageProperties.setContentType("text/plain");
		messageProperties.setDeliveryTag(43L);
		messages.add(new org.springframework.amqp.core.Message("test2".getBytes(), messageProperties));
		listener.onMessageBatch(messages, null);

		assertThat(recoveredLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(recoveredError.get())
				.isInstanceOf(RetryException.class)
				.cause()
				.isInstanceOf(MessagingException.class)
				.extracting(Throwable::getMessage, InstanceOfAssertFactories.STRING)
				.contains("Dispatcher has no");

		assertThat(recoveredMessages.get()).isSameAs(messages);
	}

	public record Foo(String bar) {

	}

}
