/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import io.nats.client.impl.NatsJetStreamPullSubscription;
import io.nats.client.impl.NatsJetStreamSubscription;
import io.nats.client.impl.NatsMessage;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.integration.nats.NatsMessageDrivenChannelAdapter.NatsMessageHandler;

/**
 * Unit testing of container components
*
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 */
public class NatsContainerTest {

	private final Message natsMessage1 =
			NatsMessage.builder()
					.data("Hello_1".getBytes())
					.subject("validSubjectForPullSubscription")
					.build();
	private final Message natsMessage2 =
			NatsMessage.builder()
					.data("Hello_2".getBytes())
					.subject("validSubjectForPullSubscription")
					.build();
	private final ConsumerProperties consumerProperties =
			new ConsumerProperties("validStream", "validSubject", "valid-consumer", "valid-group");
	private Connection natsConnection;
	private JetStream jetStreamContext;

	@Before
	public void initialize() throws IOException {
		this.natsConnection = mock(Connection.class);
	}

	/**
	 * Tests the createSubscription method of NatsMessageListenerContainer for all delivery modes push
	 * async, push sync and pull
	 *
	 * @throws IOException covers various communication issues with the NATS server such as timeout or
	 *     interruption
	 * @throws JetStreamApiException the request had an error related to the data
	 */
	@Test
	public void testCreateSubscription() throws IOException, JetStreamApiException {
		// Configuration of adapter and container components
		final NatsConsumerFactory consumerFactory = mock(NatsConsumerFactory.class);
		NatsMessageListenerContainer container =
				new NatsMessageListenerContainer(consumerFactory, NatsMessageDeliveryMode.PULL);
		final NatsMessageHandler<String> messageHandler = mock(NatsMessageHandler.class);
		// Invoke createSubscription method
		container.createSubscription(messageHandler);
		// verify method for pull mode is called
		verify(consumerFactory, times(1)).createSyncPullSubscription();
		container =
				new NatsMessageListenerContainer(consumerFactory, NatsMessageDeliveryMode.PUSH_ASYNC);
		container.setMessageHandler(messageHandler);
		// Invoke createSubscription method
		container.createSubscription(messageHandler);
		// verify method for PUSH ASYNC mode mode is called
		verify(consumerFactory, times(1)).createAsyncPushSubscription(messageHandler);
		// Test PUSH SYNC mode
		container =
				new NatsMessageListenerContainer(consumerFactory, NatsMessageDeliveryMode.PUSH_SYNC);
		// Invoke createSubscription method
		container.createSubscription(messageHandler);
		// verify method for PUSH SYNC mode mode is called
		verify(consumerFactory, times(1)).createSyncPushSubscription();
	}

	/**
	 * Tests the CreateSubscription method's exception handling scenario when consumerFactory throws
	 * error and verifies the running state of container
	 *
	 * @throws IOException covers various communication issues with the NATS server such as timeout or
	 *     interruption
	 * @throws JetStreamApiException the request had an error related to the data
	 */
	@Test
	public void testCreateSubscriptionExceptionHandlingScenario()
			throws IOException, JetStreamApiException {
		// Configuration of adapter and container components
		final NatsConsumerFactory consumerFactory = mock(NatsConsumerFactory.class);
		final NatsMessageHandler<String> messageHandler = mock(NatsMessageHandler.class);
		this.consumerProperties.setConsumerMaxWait(Duration.ofMillis(100));
		when(consumerFactory.getConsumerProperties()).thenReturn(this.consumerProperties);
		when(consumerFactory.createSyncPushSubscription()).thenThrow(IOException.class);
		// Test exception handling when consumerFactory throws error
		final NatsMessageListenerContainer exceptionContainer =
				new NatsMessageListenerContainer(consumerFactory, NatsMessageDeliveryMode.PUSH_SYNC);
		assertFalse(exceptionContainer.isRunning());
		// call create subscription method and verify exception scenario
		assertThrows(IOException.class, () -> exceptionContainer.createSubscription(messageHandler));
		// verify method for PUSH SYNC mode mode is called
		verify(consumerFactory, times(1)).createSyncPushSubscription();
		// check if the container is running after exception in consumer factory
		assertFalse(exceptionContainer.isRunning());
	}

	/**
	 * Tests the container start and stop method - Negative scenario
	 *
	 * <p>Scenario: calling these method twice from same bean should not reinitiate the behaviour
	 *
	 * @throws IOException covers various communication issues with the NATS server such as timeout or
	 *     interruption
	 * @throws JetStreamApiException the request had an error related to the data
	 */
	@Test
	public void testContainerStartAndStop() throws IOException, JetStreamApiException {
		// Configuration of adapter and container components
		final NatsJetStreamSubscription pushSubscription = mock(NatsJetStreamSubscription.class);
		final NatsConsumerFactory consumerFactory = mock(NatsConsumerFactory.class);
		when(consumerFactory.getConsumerProperties()).thenReturn(this.consumerProperties);
		when(consumerFactory.createSyncPushSubscription()).thenReturn(pushSubscription);
		// container setup
		final NatsMessageListenerContainer container =
				new NatsMessageListenerContainer(consumerFactory, NatsMessageDeliveryMode.PUSH_SYNC);
		final NatsMessageHandler<String> messageHandlerPullMode = mock(NatsMessageHandler.class);
		// Assert container is inactive
		assertFalse(container.isRunning());
		// call doStart method
		container.setMessageHandler(messageHandlerPullMode);
		container.doStart();
		// verify method for PUSH SYNC mode mode is called
		verify(consumerFactory, times(1)).createSyncPushSubscription();
		// check if the container is running
		assertTrue(container.isRunning());
		// call doStart method again to verify that subscription is not created
		// again
		container.doStart();
		// verify that subscription is not created again
		verify(consumerFactory, times(1)).createSyncPushSubscription();
		// check if the container is running after exception in consumer factory
		assertTrue(container.isRunning());
		// call doStop method
		container.doStop(() -> {
		});
		// check if the container is stopped and unsubscribe is called
		assertFalse(container.isRunning());
		verify(pushSubscription, times(1)).unsubscribe();
		// call doStart method again to verify that doStop block is not running
		// again
		container.doStop(() -> {
		});
		// verify that subscription is not unsubscribed again
		verify(consumerFactory, times(1)).createSyncPushSubscription();
	}

	/**
	 * Tests Message polling functionality of container and onMessage invocation (PULL mode)
	 *
	 * <p>Mocks PullSubscription to return predefined messages and check if the message Handler is
	 * called while polling
	 *
	 * @throws IOException covers various communication issues with the NATS server such as timeout or
	 *     interruption
	 * @throws InterruptedException if any thread has interrupted the current thread. The interrupted
	 *     status of the current thread is cleared when this exception is thrown
	 */
	@Test
	public void testPollingPullMode() throws IOException, InterruptedException {
		// Configure Mock for Jetstream context and subscription - PULL
		final NatsJetStreamPullSubscription pullSubscription =
				initJetStreamContextForPullSubscription();
		// Configuration of adapter and container components
		this.consumerProperties.setConsumerMaxWait(Duration.ofMillis(100));
		final NatsMessageListenerContainer containerPullMode =
				new NatsMessageListenerContainer(
						new NatsConsumerFactory(this.natsConnection, this.consumerProperties));
		containerPullMode.setBeanName("pull-container");
		final NatsMessageDrivenChannelAdapter adapterPullMode =
				new NatsMessageDrivenChannelAdapter(containerPullMode);
		final NatsMessageHandler<String> messageHandlerPullMode = mock(NatsMessageHandler.class);
		adapterPullMode.setBeanName("pull-adapter");
		containerPullMode.setMessageHandler(messageHandlerPullMode);
		// start adapter to initiate polling
		adapterPullMode.start();
		// Assert that adapter and container is running after start
		assertTrue(adapterPullMode.isRunning());
		assertTrue(containerPullMode.isRunning());
		// Check if the thread is created with name specified
		final Set<Thread> tSet = Thread.getAllStackTraces().keySet();
		// thread name format for single container : pull-container-nats-C-1 =>
		// <beanID>-nats-C-<thread-suffix-generated>
		final Optional<Thread> optionalThread =
				tSet.stream()
						.filter(t -> ("pull-container" + "-nats-C-1").equalsIgnoreCase(t.getName()))
						.findFirst();
		assertTrue(optionalThread.isPresent());
		final Thread thread = optionalThread.get();
		// check if the thread is alive and running
		assertTrue(thread.isAlive());
		// Wait till the messages are delivered via subscription
		Thread.sleep(500);
		verify(pullSubscription, atLeast(2)).iterate(50, Duration.ofMillis(100));
		// verify poll method invokes messageHandler's on Message method
		verify(messageHandlerPullMode, times(2)).onMessage(this.natsMessage1);
		verify(messageHandlerPullMode, times(2)).onMessage(this.natsMessage2);
		// stop adapter
		adapterPullMode.stop();
		// Assert that adapter and container is not running after stop
		assertFalse(adapterPullMode.isRunning());
		assertFalse(containerPullMode.isRunning());
		// check if the thread is stopped after container stop, wait for few
		// seconds
		Thread.sleep(2000);
		assertFalse(thread.isAlive());
	}

	/**
	 * Tests Message polling functionality of container and onMessage invocation (PUSH SYNC mode)
	 *
	 * <p>Mocks PushSubscription to return predefined messages and check if the message Handler is
	 * called while polling
	 *
	 * @throws IOException covers various communication issues with the NATS server such as timeout or
	 *     interruption
	 * @throws InterruptedException if any thread has interrupted the current thread. The interrupted
	 *     status of the current thread is cleared when this exception is thrown
	 */
	@Test
	public void testPollingPushMode() throws IOException, InterruptedException {
		// Configure Mock for Jetstream context and subscription - PUSH
		final NatsJetStreamSubscription pushSubscription = initJetStreamContextForPushSubscription();
		// Configuration of adapter and container components
		final NatsMessageListenerContainer container =
				new NatsMessageListenerContainer(
						new NatsConsumerFactory(this.natsConnection, this.consumerProperties),
						NatsMessageDeliveryMode.PUSH_SYNC);
		container.setBeanName("push-container");
		final NatsMessageDrivenChannelAdapter adapter = new NatsMessageDrivenChannelAdapter(container);
		final NatsMessageHandler<String> messageHandler = mock(NatsMessageHandler.class);
		adapter.setBeanName("push-adapter");
		container.setMessageHandler(messageHandler);
		// start adapter to initiate polling
		adapter.start();
		// Assert that adapter and container is running after start
		assertTrue(adapter.isRunning());
		assertTrue(container.isRunning());
		// Check if the thread is created with name specified
		final Set<Thread> tSet = Thread.getAllStackTraces().keySet();
		// thread name format for single container : push-container-nats-C-1 =>
		// <beanID>-nats-C-<thread-suffix-generated>
		final Optional<Thread> optionalThread =
				tSet.stream()
						.filter(t -> ("push-container" + "-nats-C-1").equalsIgnoreCase(t.getName()))
						.findFirst();
		assertTrue(optionalThread.isPresent());
		final Thread thread = optionalThread.get();
		// check if the thread is alive and running
		assertTrue(thread.isAlive());
		// Wait till the messages are delivered via subscription
		Thread.sleep(500);
		verify(pushSubscription, atLeast(2)).nextMessage(Duration.ofSeconds(30));
		// verify poll method invokes messageHandler's on Message method
		verify(messageHandler, times(1)).onMessage(this.natsMessage1);
		verify(messageHandler, times(1)).onMessage(this.natsMessage2);
		// stop adapter
		adapter.stop();
		// Assert that adapter and container is not running after stop
		assertFalse(adapter.isRunning());
		assertFalse(container.isRunning());
		// check if the thread is stopped after container stop, wait for few
		// seconds
		Thread.sleep(2000);
		assertFalse(thread.isAlive());
	}

	/**
	 * Mocks JetStream context of Nats connection to return Pull Subscription (with test messages)
	 *
	 * @return NatsJetStreamSubscription the NATS subscription object for message consumption
	 * @throws IOException covers various communication issues with the NATS server such as timeout or
	 *     interruption
	 */
	public NatsJetStreamPullSubscription initJetStreamContextForPullSubscription()
			throws IOException {
		this.natsConnection = mock(Connection.class);
		final NatsJetStreamPullSubscription pullSubscription =
				mock(NatsJetStreamPullSubscription.class);
		final List<Message> natsMessageList = Arrays.asList(this.natsMessage1, this.natsMessage2);
		when(pullSubscription.iterate(50, Duration.ofMillis(100))) //
				.thenReturn(natsMessageList.iterator()) //
				.thenReturn(natsMessageList.iterator());
		this.jetStreamContext =
				mock(
						JetStream.class,
						new Answer<NatsJetStreamPullSubscription>() {

							@Override
							public NatsJetStreamPullSubscription answer(final InvocationOnMock invocation)
									throws Throwable {
								return pullSubscription;
							}
						});
		when(this.natsConnection.jetStream()).thenReturn(this.jetStreamContext);
		return pullSubscription;
	}

	/**
	 * Mocks JetStream context of Nats connection to return Push Subscription (with test messages)
	 *
	 * @return NatsJetStreamSubscription the NATS subscription object for message consumption
	 * @throws IOException covers various communication issues with the NATS server such as timeout or
	 *     interruption
	 * @throws InterruptedException if one occurs while waiting for the message
	 */
	public NatsJetStreamSubscription initJetStreamContextForPushSubscription()
			throws IOException, InterruptedException {
		this.natsConnection = mock(Connection.class);
		final NatsJetStreamSubscription pushSubscription = mock(NatsJetStreamSubscription.class);
		when(pushSubscription.nextMessage(Duration.ofSeconds(30)))
				.thenReturn(this.natsMessage1, this.natsMessage2, null);
		this.jetStreamContext =
				mock(
						JetStream.class,
						new Answer<NatsJetStreamSubscription>() {

							@Override
							public NatsJetStreamSubscription answer(final InvocationOnMock invocation)
									throws Throwable {
								return pushSubscription;
							}
						});
		when(this.natsConnection.jetStream()).thenReturn(this.jetStreamContext);
		return pushSubscription;
	}

	/**
	 * Tests PUSH Async mode
	 *
	 * <p>Mocks PushSubscription to test that no new thread is created by us.For Push async, threads
	 * are created and managed by NATS client API
	 *
	 * @throws IOException covers various communication issues with the NATS server such as timeout or
	 *     interruption
	 * @throws JetStreamApiException the request had an error related to the data
	 */
	@Test
	public void testPushAsyncMode() throws IOException, JetStreamApiException {
		// Configure Mock for Jetstream context and subscription - PUSH ASYNC
		final NatsMessageHandler<String> messageHandlerPushAsyncMode = mock(NatsMessageHandler.class);
		final NatsJetStreamSubscription pushAsyncModeSubscription =
				mock(NatsJetStreamSubscription.class);
		when(pushAsyncModeSubscription.getDispatcher()).thenReturn(mock(Dispatcher.class));
		final NatsConsumerFactory consumerFactory = mock(NatsConsumerFactory.class);
		when(consumerFactory.getConsumerProperties()).thenReturn(this.consumerProperties);
		when(consumerFactory.createAsyncPushSubscription(messageHandlerPushAsyncMode))
				.thenReturn(pushAsyncModeSubscription);
		// Configuration of adapter and container components
		final NatsMessageListenerContainer containerPushAsyncMode =
				new NatsMessageListenerContainer(consumerFactory, NatsMessageDeliveryMode.PUSH_ASYNC);
		containerPushAsyncMode.setBeanName("push-async-container");
		final NatsMessageDrivenChannelAdapter adapterPushAsyncMode =
				new NatsMessageDrivenChannelAdapter(containerPushAsyncMode);
		adapterPushAsyncMode.setBeanName("push-async-adapter");
		containerPushAsyncMode.setMessageHandler(messageHandlerPushAsyncMode);
		// start adapter
		adapterPushAsyncMode.start();
		// Assert that adapter and container is running after start
		assertTrue(adapterPushAsyncMode.isRunning());
		assertTrue(adapterPushAsyncMode.isRunning());
		// Check if the thread is not created by container
		final Set<Thread> tSet = Thread.getAllStackTraces().keySet();
		// thread name format : push-container-nats-C-?
		final Optional<Thread> optionalThread =
				tSet.stream()
						.filter(t -> ("push-async-container" + "-nats-C-1").equalsIgnoreCase(t.getName()))
						.findFirst();
		assertFalse(optionalThread.isPresent());
		// stop adapter
		adapterPushAsyncMode.stop();
	}
}
