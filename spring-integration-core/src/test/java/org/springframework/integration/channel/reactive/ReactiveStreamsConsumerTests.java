/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.channel.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.endpoint.ReactiveStreamsConsumer;
import org.springframework.integration.handler.MethodInvokingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.messaging.support.GenericMessage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.util.Loggers;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ReactiveStreamsConsumerTests {

	private static final Log LOGGER = LogFactory.getLog(ReactiveStreamsConsumerTests.class);

	@Test
	public void testReactiveStreamsConsumerFluxMessageChannel() throws InterruptedException {
		FluxMessageChannel testChannel = new FluxMessageChannel();

		List<Message<?>> result = new LinkedList<>();
		CountDownLatch stopLatch = new CountDownLatch(2);

		MessageHandler messageHandler = m -> {
			result.add(m);
			stopLatch.countDown();
		};

		MessageHandler testSubscriber = new MethodInvokingMessageHandler(messageHandler, (String) null);
		((MethodInvokingMessageHandler) testSubscriber).setBeanFactory(mock(BeanFactory.class));
		ReactiveStreamsConsumer reactiveConsumer = new ReactiveStreamsConsumer(testChannel, testSubscriber);
		reactiveConsumer.setBeanFactory(mock(BeanFactory.class));
		reactiveConsumer.afterPropertiesSet();
		reactiveConsumer.start();

		Message<?> testMessage = new GenericMessage<>("test");
		testChannel.send(testMessage);

		reactiveConsumer.stop();

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> testChannel.send(testMessage))
				.withCauseInstanceOf(IllegalStateException.class)
				.withMessageContaining("doesn't have subscribers to accept messages");

		reactiveConsumer.start();

		Message<?> testMessage2 = new GenericMessage<>("test2");
		testChannel.send(testMessage2);

		assertThat(stopLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(result).containsExactly(testMessage, testMessage2);

		reactiveConsumer.stop();
		testChannel.destroy();
	}


	@Test
	public void testReactiveStreamsConsumerDirectChannel() throws InterruptedException {
		DirectChannel testChannel = new DirectChannel();

		BlockingQueue<Message<?>> messages = new LinkedBlockingQueue<>();

		Subscriber<Message<?>> testSubscriber = Mockito.spy(new Subscriber<Message<?>>() {

			@Override
			public void onSubscribe(Subscription subscription) {
				subscription.request(1);
			}

			@Override
			public void onNext(Message<?> message) {
				messages.offer(message);
			}

			@Override
			public void onError(Throwable t) {

			}

			@Override
			public void onComplete() {

			}

		});

		ReactiveStreamsConsumer reactiveConsumer = new ReactiveStreamsConsumer(testChannel, testSubscriber);
		reactiveConsumer.setBeanFactory(mock(BeanFactory.class));
		reactiveConsumer.afterPropertiesSet();
		reactiveConsumer.start();

		final Message<?> testMessage = new GenericMessage<>("test");
		testChannel.send(testMessage);

		Message<?> message = messages.poll(10, TimeUnit.SECONDS);
		assertThat(message).isSameAs(testMessage);

		reactiveConsumer.stop();

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> testChannel.send(testMessage));

		reactiveConsumer.start();

		testChannel.send(testMessage);

		message = messages.poll(10, TimeUnit.SECONDS);
		assertThat(message).isSameAs(testMessage);

		verify(testSubscriber, never()).onError(any(Throwable.class));
		verify(testSubscriber, never()).onComplete();

		assertThat(messages.isEmpty()).isTrue();

		reactiveConsumer.stop();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testReactiveStreamsConsumerPollableChannel() throws InterruptedException {
		QueueChannel testChannel = new QueueChannel();

		Subscriber<Message<?>> testSubscriber = (Subscriber<Message<?>>) Mockito.mock(Subscriber.class);

		BlockingQueue<Message<?>> messages = new LinkedBlockingQueue<>();

		willAnswer(i -> {
			Message<?> message = i.getArgument(0);
			LOGGER.debug("Polled message: " + message);
			messages.put(message);
			return null;
		})
				.given(testSubscriber)
				.onNext(any(Message.class));

		ReactiveStreamsConsumer reactiveConsumer = new ReactiveStreamsConsumer(testChannel, testSubscriber);

		DirectFieldAccessor dfa = new DirectFieldAccessor(reactiveConsumer);
		Flux<?> publisher = (Flux<?>) dfa.getPropertyValue("publisher");
		publisher = publisher.log(Loggers.getLogger(ReactiveStreamsConsumerTests.class));
		dfa.setPropertyValue("publisher", publisher);

		reactiveConsumer.setBeanFactory(mock(BeanFactory.class));
		reactiveConsumer.afterPropertiesSet();
		reactiveConsumer.start();

		Message<?> testMessage = new GenericMessage<>("test");
		testChannel.send(testMessage);

		ArgumentCaptor<Subscription> subscriptionArgumentCaptor = ArgumentCaptor.forClass(Subscription.class);
		verify(testSubscriber).onSubscribe(subscriptionArgumentCaptor.capture());
		Subscription subscription = subscriptionArgumentCaptor.getValue();

		subscription.request(1);

		Message<?> message = messages.poll(10, TimeUnit.SECONDS);
		assertThat(message).isSameAs(testMessage);

		reactiveConsumer.stop();


		testChannel.send(testMessage);

		reactiveConsumer.start();

		verify(testSubscriber, times(2)).onSubscribe(subscriptionArgumentCaptor.capture());
		subscription = subscriptionArgumentCaptor.getValue();

		subscription.request(2);

		Message<?> testMessage2 = new GenericMessage<>("test2");

		testChannel.send(testMessage2);

		await().untilAsserted(() -> assertThat(messages).hasSizeGreaterThan(0));

		LOGGER.debug("Messages to poll: " + messages);

		message = messages.poll(10, TimeUnit.SECONDS);
		assertThat(message).isSameAs(testMessage);

		message = messages.poll(10, TimeUnit.SECONDS);
		assertThat(message).isSameAs(testMessage2);

		verify(testSubscriber, never()).onError(any(Throwable.class));
		verify(testSubscriber, never()).onComplete();

		assertThat(messages.isEmpty()).isTrue();

		reactiveConsumer.stop();
	}

	@Test
	public void testReactiveStreamsConsumerViaConsumerEndpointFactoryBean() throws Exception {
		FluxMessageChannel testChannel = new FluxMessageChannel();

		List<Message<?>> result = new LinkedList<>();
		CountDownLatch stopLatch = new CountDownLatch(3);

		MessageHandler messageHandler = m -> {
			result.add(m);
			stopLatch.countDown();
		};

		ConsumerEndpointFactoryBean endpointFactoryBean = new ConsumerEndpointFactoryBean();
		endpointFactoryBean.setBeanFactory(mock(ConfigurableBeanFactory.class));
		endpointFactoryBean.setInputChannel(testChannel);
		endpointFactoryBean.setHandler(messageHandler);
		endpointFactoryBean.setBeanName("reactiveConsumer");
		endpointFactoryBean.afterPropertiesSet();
		endpointFactoryBean.start();

		Message<?> testMessage = new GenericMessage<>("test");
		testChannel.send(testMessage);

		endpointFactoryBean.stop();

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> testChannel.send(testMessage))
				.withCauseInstanceOf(IllegalStateException.class)
				.withMessageContaining("doesn't have subscribers to accept messages");

		endpointFactoryBean.start();

		Message<?> testMessage2 = new GenericMessage<>("test2");

		testChannel.send(testMessage2);
		testChannel.send(testMessage2);

		assertThat(stopLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(result.size()).isEqualTo(3);
		assertThat(result).containsExactly(testMessage, testMessage2, testMessage2);

		endpointFactoryBean.stop();
		testChannel.destroy();
	}

	@Test
	public void testReactiveStreamsConsumerFluxMessageChannelReactiveMessageHandler() {
		FluxMessageChannel testChannel = new FluxMessageChannel();

		Sinks.Many<Object> sink = Sinks.many().multicast().onBackpressureBuffer(2, false);

		ReactiveMessageHandler messageHandler =
				m -> {
					sink.tryEmitNext(m);
					return Mono.empty();
				};

		ReactiveStreamsConsumer reactiveConsumer = new ReactiveStreamsConsumer(testChannel, messageHandler);
		reactiveConsumer.setBeanFactory(mock(BeanFactory.class));
		reactiveConsumer.afterPropertiesSet();
		reactiveConsumer.start();

		Message<?> testMessage = new GenericMessage<>("test");
		testChannel.send(testMessage);

		reactiveConsumer.stop();

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> testChannel.send(testMessage))
				.withCauseInstanceOf(IllegalStateException.class)
				.withMessageContaining("doesn't have subscribers to accept messages");

		reactiveConsumer.start();

		Message<?> testMessage2 = new GenericMessage<>("test2");
		testChannel.send(testMessage2);

		StepVerifier.create(sink.asFlux())
				.expectNext(testMessage, testMessage2)
				.thenCancel()
				.verify(Duration.ofSeconds(10));

		reactiveConsumer.stop();
		testChannel.destroy();
	}

	@Test
	public void testReactiveCustomizer() throws Exception {
		DirectChannel testChannel = new DirectChannel();

		AtomicReference<Message<?>> spied = new AtomicReference<>();
		AtomicReference<Message<?>> result = new AtomicReference<>();
		CountDownLatch stopLatch = new CountDownLatch(1);

		MessageHandler messageHandler = m -> {
			result.set(m);
			stopLatch.countDown();
		};

		ConsumerEndpointFactoryBean endpointFactoryBean = new ConsumerEndpointFactoryBean();
		endpointFactoryBean.setBeanFactory(mock(ConfigurableBeanFactory.class));
		endpointFactoryBean.setInputChannel(testChannel);
		endpointFactoryBean.setHandler(messageHandler);
		endpointFactoryBean.setBeanName("reactiveConsumer");
		endpointFactoryBean.setReactiveCustomizer(flux -> flux.doOnNext(spied::set));
		endpointFactoryBean.afterPropertiesSet();
		endpointFactoryBean.start();

		Message<?> testMessage = new GenericMessage<>("test");
		testChannel.send(testMessage);

		assertThat(stopLatch.await(10, TimeUnit.SECONDS)).isTrue();
		endpointFactoryBean.stop();

		assertThat(result.get()).isSameAs(testMessage);
		assertThat(spied.get()).isSameAs(testMessage);
	}

}
