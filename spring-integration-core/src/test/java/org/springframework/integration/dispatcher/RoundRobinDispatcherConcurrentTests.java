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

package org.springframework.integration.dispatcher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.integration.MessageRejectedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Iwein Fuld
 * @author Artem Bilan
 */
@RunWith(MockitoJUnitRunner.class)
public class RoundRobinDispatcherConcurrentTests {

	private static final int TOTAL_EXECUTIONS = 40;

	private final UnicastingDispatcher dispatcher = new UnicastingDispatcher();

	private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

	@Mock
	private MessageHandler handler1;

	@Mock
	private MessageHandler handler2;

	@Mock
	private MessageHandler handler3;

	@Mock
	private MessageHandler handler4;

	@Mock
	private Message<?> message;

	@Before
	public void initialize() {
		dispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(10);
		executor.initialize();
	}

	@After
	public void tearDown() {
		this.executor.shutdown();
	}

	@Test
	public void noHandlerExhaustion() throws Exception {
		dispatcher.addHandler(handler1);
		dispatcher.addHandler(handler2);
		dispatcher.addHandler(handler3);
		dispatcher.addHandler(handler4);
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch allDone = new CountDownLatch(TOTAL_EXECUTIONS);
		final Message<?> message = this.message;
		final AtomicBoolean failed = new AtomicBoolean(false);
		Runnable messageSenderTask = () -> {
			try {
				start.await();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (!dispatcher.dispatch(message)) {
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
		verify(handler1, times(TOTAL_EXECUTIONS / 4)).handleMessage(message);
		verify(handler2, times(TOTAL_EXECUTIONS / 4)).handleMessage(message);
		verify(handler3, times(TOTAL_EXECUTIONS / 4)).handleMessage(message);
		verify(handler4, times(TOTAL_EXECUTIONS / 4)).handleMessage(message);
	}

	@Test
	public void unlockOnFailure() throws Exception {
		// dispatcher has no subscribers (shouldn't lead to deadlock)
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch allDone = new CountDownLatch(TOTAL_EXECUTIONS);
		final Message<?> message = this.message;
		Runnable messageSenderTask = () -> {
			try {
				start.await();
			}
			catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
			try {
				dispatcher.dispatch(message);
				fail("this shouldn't happen");
			}
			catch (MessagingException e2) {
				// expected
			}
			allDone.countDown();
		};
		for (int i = 0; i < TOTAL_EXECUTIONS; i++) {
			executor.execute(messageSenderTask);
		}
		start.countDown();
		assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void noHandlerSkipUnderConcurrentFailureWithFailover() throws Exception {
		dispatcher.addHandler(handler1);
		dispatcher.addHandler(handler2);
		doThrow(new MessageRejectedException(message, null)).when(handler1).handleMessage(message);
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch allDone = new CountDownLatch(TOTAL_EXECUTIONS);
		final Message<?> message = this.message;
		final AtomicBoolean failed = new AtomicBoolean(false);
		Runnable messageSenderTask = () -> {
			try {
				start.await();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (!dispatcher.dispatch(message)) {
				failed.set(true);
			}
			else {
				allDone.countDown();
			}
		};
		for (int i = 0; i < TOTAL_EXECUTIONS; i++) {
			executor.execute(messageSenderTask);
		}
		start.countDown();
		assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(failed.get()).as("not all messages were accepted").isFalse();
		verify(handler1, times(TOTAL_EXECUTIONS / 2)).handleMessage(message);
		verify(handler2, times(TOTAL_EXECUTIONS)).handleMessage(message);
	}

}
