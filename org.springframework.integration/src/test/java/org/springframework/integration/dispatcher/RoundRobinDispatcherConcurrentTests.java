package org.springframework.integration.dispatcher;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnit44Runner;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@RunWith(MockitoJUnit44Runner.class)
public class RoundRobinDispatcherConcurrentTests {

	private static final int TOTAL_EXECUTIONS = 40;

	private AbstractWinningHandlerDispatcher dispatcher = new RoundRobinDispatcher();

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
	}

	@Test(timeout=2000)
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
				catch (MessageDeliveryException e) {
					//expected
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
}
