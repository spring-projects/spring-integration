/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.redis.inbound;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.integration.channel.QueueChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Artem Bilan
 */
class RedisQueueMessageDrivenEndpointUnitTests {

	@Test
	@SuppressWarnings("unchecked")
	void stopCallbackIsRegisteredBeforeListenerExits() throws Exception {
		CountDownLatch receiveStarted = new CountDownLatch(1);
		CountDownLatch stopRequested = new CountDownLatch(1);
		CountDownLatch listenerExited = new CountDownLatch(1);
		RedisQueueMessageDrivenEndpoint endpoint =
				new RedisQueueMessageDrivenEndpoint("stopCallbackQueue", mock(RedisConnectionFactory.class)) {

			@Override
			protected void doStop(Runnable callback) {
				// Let the listener finish before the stop callback has been registered.
				stopRequested.countDown();
				await().until(() -> listenerExited.getCount() == 0 || this.lifecycleLock.hasQueuedThreads());
				super.doStop(callback);
			}

		};
		BoundListOperations<String, byte[]> listOperations = mock(BoundListOperations.class);
		when(listOperations.rightPop(anyLong(), any(TimeUnit.class))).thenAnswer(invocation -> {
			receiveStarted.countDown();
			assertThat(stopRequested.await(10, TimeUnit.SECONDS)).isTrue();
			return null;
		});
		new DirectFieldAccessor(endpoint).setPropertyValue("boundListOperations", listOperations);
		endpoint.setBeanFactory(mock(BeanFactory.class));
		endpoint.setOutputChannel(new QueueChannel());
		ExecutorService executor = Executors.newSingleThreadExecutor();
		endpoint.setTaskExecutor(task -> executor.execute(() -> {
			try {
				task.run();
			}
			finally {
				listenerExited.countDown();
			}
		}));
		endpoint.afterPropertiesSet();
		try {
			endpoint.start();
			assertThat(receiveStarted.await(10, TimeUnit.SECONDS)).isTrue();
			AtomicInteger callbackCount = new AtomicInteger();
			endpoint.stop(callbackCount::incrementAndGet);
			executor.shutdown();
			assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
			assertThat(callbackCount.get()).isEqualTo(1);
		}
		finally {
			endpoint.stop();
			stopRequested.countDown();
			executor.shutdownNow();
		}
	}

}
