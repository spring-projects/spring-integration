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

package org.springframework.integration.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MixedDispatcherConfigurationScenarioTests {

	private static final int TOTAL_EXECUTIONS = 40;

	private final CountDownLatch allDone = new CountDownLatch(TOTAL_EXECUTIONS);

	private final CountDownLatch start = new CountDownLatch(1);

	private final AtomicBoolean failed = new AtomicBoolean(false);

	private final Message<?> message = new GenericMessage<>("test");

	@SuppressWarnings("unchecked")
	private final List<Exception> exceptionRegistry = mock(List.class);

	private final MessageHandler handlerA = mock(MessageHandler.class);

	private final MessageHandler handlerB = mock(MessageHandler.class);

	private final MessageHandler handlerC = mock(MessageHandler.class);

	@Autowired
	@Qualifier("taskExecutor")
	private Executor executor;

	@Autowired
	private ConfigurableApplicationContext ac;

	@Test
	public void noFailoverNoLoadBalancing() {
		DirectChannel channel = (DirectChannel) ac.getBean("noLoadBalancerNoFailover");
		doThrow(new MessageRejectedException(message, null)).when(handlerA).handleMessage(message);
		UnicastingDispatcher dispatcher = channel.getDispatcher();
		dispatcher.addHandler(handlerA);
		dispatcher.addHandler(handlerB);
		try {
			channel.send(message);
		}
		catch (Exception e) { /* ignore */
		}
		try {
			channel.send(message);
		}
		catch (Exception e) { /* ignore */
		}
		verify(handlerA, times(2)).handleMessage(message);
		verify(handlerB, times(0)).handleMessage(message);
	}

	@Test
	public void noFailoverNoLoadBalancingConcurrent() throws Exception {
		final DirectChannel channel = (DirectChannel) ac.getBean("noLoadBalancerNoFailover");
		doThrow(new MessageRejectedException(message, null)).when(handlerA).handleMessage(message);
		UnicastingDispatcher dispatcher = channel.getDispatcher();
		dispatcher.addHandler(handlerA);
		dispatcher.addHandler(handlerB);

		Runnable messageSenderTask = () -> {
			try {
				start.await();
			}
			catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
			boolean sent = false;
			try {
				sent = channel.send(message);
			}
			catch (Exception e2) {
				exceptionRegistry.add(e2);
			}
			if (!sent) {
				failed.set(true);
			}
			allDone.countDown();
		};
		for (int i = 0; i < TOTAL_EXECUTIONS; i++) {
			executor.execute(messageSenderTask);
		}
		start.countDown();
		assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(failed.get()).as("not all messages were accepted").isTrue();
		verify(handlerA, times(TOTAL_EXECUTIONS)).handleMessage(message);
		verify(handlerB, times(0)).handleMessage(message);
		verify(exceptionRegistry, times(TOTAL_EXECUTIONS)).add(any(Exception.class));
	}

	@Test
	public void noFailoverNoLoadBalancingWithExecutorConcurrent() throws Exception {
		final ExecutorChannel channel = (ExecutorChannel) ac.getBean("noLoadBalancerNoFailoverExecutor");
		UnicastingDispatcher dispatcher = channel.getDispatcher();
		dispatcher.addHandler(handlerA);
		dispatcher.addHandler(handlerB);

		doAnswer(invocation -> {
			RuntimeException e = new RuntimeException();
			failed.set(true);
			exceptionRegistry.add(e);
			allDone.countDown();
			throw e;
		}).when(handlerA).handleMessage(message);


		Runnable messageSenderTask = () -> {
			try {
				start.await();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			channel.send(message);
		};
		for (int i = 0; i < TOTAL_EXECUTIONS; i++) {
			executor.execute(messageSenderTask);
		}
		start.countDown();
		assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(failed.get()).as("not all messages were accepted").isTrue();
		verify(handlerA, times(TOTAL_EXECUTIONS)).handleMessage(message);
		verify(handlerB, times(0)).handleMessage(message);
		verify(exceptionRegistry, times(TOTAL_EXECUTIONS)).add(any(Exception.class));
	}

	@Test
	public void noFailoverLoadBalancing() {
		DirectChannel channel = (DirectChannel) ac.getBean("loadBalancerNoFailover");
		doThrow(new MessageRejectedException(message, null)).when(handlerA).handleMessage(message);
		UnicastingDispatcher dispatcher = channel.getDispatcher();
		dispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
		dispatcher.addHandler(handlerA);
		dispatcher.addHandler(handlerB);
		dispatcher.addHandler(handlerC);
		InOrder inOrder = inOrder(handlerA, handlerB, handlerC);
		try {
			channel.send(message);
		}
		catch (Exception e) { /* ignore */
		}
		inOrder.verify(handlerA).handleMessage(message);
		try {
			channel.send(message);
		}
		catch (Exception e) { /* ignore */
		}
		inOrder.verify(handlerB).handleMessage(message);
		try {
			channel.send(message);
		}
		catch (Exception e) { /* ignore */
		}
		inOrder.verify(handlerC).handleMessage(message);

		verify(handlerA, times(1)).handleMessage(message);
		verify(handlerB, times(1)).handleMessage(message);
		verify(handlerC, times(1)).handleMessage(message);
	}

	@Test
	public void noFailoverLoadBalancingConcurrent() throws Exception {
		final DirectChannel channel = (DirectChannel) ac.getBean("loadBalancerNoFailover");
		doThrow(new MessageRejectedException(message, null)).when(handlerA).handleMessage(message);
		UnicastingDispatcher dispatcher = channel.getDispatcher();
		dispatcher.addHandler(handlerA);
		dispatcher.addHandler(handlerB);
		dispatcher.addHandler(handlerC);

		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch allDone = new CountDownLatch(TOTAL_EXECUTIONS);
		final Message<?> message = this.message;
		final AtomicBoolean failed = new AtomicBoolean(false);
		Runnable messageSenderTask = () -> {
			try {
				start.await();
			}
			catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
			boolean sent = false;
			try {
				sent = channel.send(message);
			}
			catch (Exception e2) {
				exceptionRegistry.add(e2);
			}
			if (!sent) {
				failed.set(true);
			}
			allDone.countDown();
		};
		for (int i = 0; i < TOTAL_EXECUTIONS; i++) {
			executor.execute(messageSenderTask);
		}
		start.countDown();
		assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(failed.get()).as("not all messages were accepted").isTrue();
		verify(handlerA, times(14)).handleMessage(message);
		verify(handlerB, times(13)).handleMessage(message);
		verify(handlerC, times(13)).handleMessage(message);
		verify(exceptionRegistry, times(14)).add(any(Exception.class));
	}

	@Test
	public void noFailoverLoadBalancingWithExecutorConcurrent() throws Exception {
		final ExecutorChannel channel = (ExecutorChannel) ac.getBean("loadBalancerNoFailoverExecutor");
		UnicastingDispatcher dispatcher = channel.getDispatcher();
		dispatcher.addHandler(handlerA);
		dispatcher.addHandler(handlerB);
		dispatcher.addHandler(handlerC);

		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch allDone = new CountDownLatch(TOTAL_EXECUTIONS);
		final Message<?> message = this.message;
		final AtomicBoolean failed = new AtomicBoolean(false);
		doAnswer(invocation -> {
			failed.set(true);
			RuntimeException e = new RuntimeException();
			exceptionRegistry.add(e);
			allDone.countDown();
			throw e;
		}).when(handlerA).handleMessage(message);
		doAnswer(invocation -> {
			allDone.countDown();
			return null;
		}).when(handlerB).handleMessage(message);
		doAnswer(invocation -> {
			allDone.countDown();
			return null;
		}).when(handlerC).handleMessage(message);

		Runnable messageSenderTask = () -> {
			try {
				start.await();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			channel.send(message);
		};
		for (int i = 0; i < TOTAL_EXECUTIONS; i++) {
			executor.execute(messageSenderTask);
		}
		start.countDown();
		assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(failed.get()).as("not all messages were accepted").isTrue();
		verify(handlerA, times(14)).handleMessage(message);
		verify(handlerB, times(13)).handleMessage(message);
		verify(handlerC, times(13)).handleMessage(message);
		verify(exceptionRegistry, times(14)).add(any(Exception.class));
	}

	@Test
	public void failoverNoLoadBalancing() {
		DirectChannel channel = ac.getBean("noLoadBalancerFailover", DirectChannel.class);
		doThrow(new MessageRejectedException(message, null)).when(handlerA)
				.handleMessage(message);
		UnicastingDispatcher dispatcher = channel.getDispatcher();
		dispatcher.addHandler(handlerA);
		dispatcher.addHandler(handlerB);
		InOrder inOrder = inOrder(handlerA, handlerB);

		try {
			channel.send(message);
		}
		catch (Exception e) { /* ignore */
		}
		inOrder.verify(handlerA).handleMessage(message);
		inOrder.verify(handlerB).handleMessage(message);

		try {
			channel.send(message);
		}
		catch (Exception e) { /* ignore */
		}
		inOrder.verify(handlerA).handleMessage(message);
		inOrder.verify(handlerB).handleMessage(message);

		verify(handlerA, times(2)).handleMessage(message);
		verify(handlerB, times(2)).handleMessage(message);
	}

	@Test
	public void failoverNoLoadBalancingConcurrent() throws Exception {
		final DirectChannel channel = ac.getBean("noLoadBalancerFailover", DirectChannel.class);
		doThrow(new MessageRejectedException(message, null)).when(handlerA).handleMessage(message);
		UnicastingDispatcher dispatcher = channel.getDispatcher();
		dispatcher.addHandler(handlerA);
		dispatcher.addHandler(handlerB);
		dispatcher.addHandler(handlerC);

		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch allDone = new CountDownLatch(TOTAL_EXECUTIONS);
		final Message<?> message = this.message;
		final AtomicBoolean failed = new AtomicBoolean(false);
		Runnable messageSenderTask = () -> {
			try {
				start.await();
			}
			catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
			boolean sent = false;
			try {
				sent = channel.send(message);
			}
			catch (Exception e2) {
				exceptionRegistry.add(e2);
			}
			if (!sent) {
				failed.set(true);
			}
			allDone.countDown();
		};
		for (int i = 0; i < TOTAL_EXECUTIONS; i++) {
			executor.execute(messageSenderTask);
		}
		start.countDown();
		assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(failed.get()).as("not all messages were accepted").isFalse();
		verify(handlerA, times(TOTAL_EXECUTIONS)).handleMessage(message);
		verify(handlerB, times(TOTAL_EXECUTIONS)).handleMessage(message);
		verify(handlerC, never()).handleMessage(message);
		verify(exceptionRegistry, never()).add(any(Exception.class));
	}

	@Test
	public void failoverNoLoadBalancingWithExecutorConcurrent() throws Exception {
		final ExecutorChannel channel = (ExecutorChannel) ac.getBean("noLoadBalancerFailoverExecutor");
		final UnicastingDispatcher dispatcher = channel.getDispatcher();
		dispatcher.addHandler(handlerA);
		dispatcher.addHandler(handlerB);
		dispatcher.addHandler(handlerC);

		doAnswer(invocation -> {
			RuntimeException e = new RuntimeException();
			failed.set(true);
			throw e;
		}).when(handlerA).handleMessage(message);
		doAnswer(invocation -> {
			allDone.countDown();
			return null;
		}).when(handlerB).handleMessage(message);

		Runnable messageSenderTask = () -> {
			try {
				start.await();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			channel.send(message);
		};
		for (int i = 0; i < TOTAL_EXECUTIONS; i++) {
			executor.execute(messageSenderTask);

		}
		start.countDown();
		assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();

		verify(handlerA, times(TOTAL_EXECUTIONS)).handleMessage(message);
		verify(handlerB, times(TOTAL_EXECUTIONS)).handleMessage(message);
		verify(handlerC, never()).handleMessage(message);
	}

}
