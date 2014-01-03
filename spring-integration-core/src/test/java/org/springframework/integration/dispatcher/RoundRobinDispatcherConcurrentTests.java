/* Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dispatcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.messaging.Message;
import org.springframework.integration.MessageRejectedException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


/**
 * @author Iwein Fuld
 */
@RunWith(MockitoJUnitRunner.class)
public class RoundRobinDispatcherConcurrentTests {

	private static final int TOTAL_EXECUTIONS = 40;

	private UnicastingDispatcher dispatcher = new UnicastingDispatcher();

	private ThreadPoolTaskExecutor scheduler = new ThreadPoolTaskExecutor();

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
	public void initialize() throws Exception {
		dispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
		scheduler.setCorePoolSize(10);
		scheduler.setMaxPoolSize(10);
		scheduler.initialize();
	}

	@Test(timeout = 1000)
	public void noHandlerExhaustion() throws Exception {
		dispatcher.addHandler(handler1);
		dispatcher.addHandler(handler2);
		dispatcher.addHandler(handler3);
		dispatcher.addHandler(handler4);
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch allDone = new CountDownLatch(TOTAL_EXECUTIONS);
		final Message<?> message = this.message;
		final AtomicBoolean failed = new AtomicBoolean(false);
		Runnable messageSenderTask = new Runnable() {
			public void run() {
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
			}
		};
		for (int i = 0; i < TOTAL_EXECUTIONS; i++) {
			scheduler.execute(messageSenderTask);
		}
		start.countDown();
		allDone.await();
		assertFalse("not all messages were accepted", failed.get());
		verify(handler1, times(TOTAL_EXECUTIONS / 4)).handleMessage(message);
		verify(handler2, times(TOTAL_EXECUTIONS / 4)).handleMessage(message);
		verify(handler3, times(TOTAL_EXECUTIONS / 4)).handleMessage(message);
		verify(handler4, times(TOTAL_EXECUTIONS / 4)).handleMessage(message);
	}

	@Test(timeout = 2000)
	public void unlockOnFailure() throws Exception {
		// dispatcher has no subscribers (shouldn't lead to deadlock)
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch allDone = new CountDownLatch(TOTAL_EXECUTIONS);
		final Message<?> message = this.message;
		Runnable messageSenderTask = new Runnable() {
			public void run() {
				try {
					start.await();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				try {
					dispatcher.dispatch(message);
					fail("this shouldn't happen");
				}
				catch (MessagingException e) {
					// expected
				}
				allDone.countDown();
			}
		};
		for (int i = 0; i < TOTAL_EXECUTIONS; i++) {
			scheduler.execute(messageSenderTask);
		}
		start.countDown();
		allDone.await();
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
		Runnable messageSenderTask = new Runnable() {
			public void run() {
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
			}
		};
		for (int i = 0; i < TOTAL_EXECUTIONS; i++) {
			scheduler.execute(messageSenderTask);
		}
		start.countDown();
		allDone.await(5000, TimeUnit.MILLISECONDS);
		assertFalse("not all messages were accepted", failed.get());
		verify(handler1, times(TOTAL_EXECUTIONS/2)).handleMessage(message);
		verify(handler2, times(TOTAL_EXECUTIONS)).handleMessage(message);
	}
}
