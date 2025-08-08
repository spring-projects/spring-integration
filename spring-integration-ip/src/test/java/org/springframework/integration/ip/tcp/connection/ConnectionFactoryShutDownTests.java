/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.MessagingException;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class ConnectionFactoryShutDownTests {

	@Test
	public void testShutdownDoesntDeadlock() throws Exception {
		final AbstractConnectionFactory factory = new AbstractConnectionFactory(0) {

			@Override
			public TcpConnection getConnection() {
				return null;
			}

		};
		factory.setActive(true);
		Executor executor = factory.getTaskExecutor();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		executor.execute(() -> {
			latch1.countDown();
			try {
				while (true) {
					factory.getTaskExecutor();
					Thread.sleep(10);
				}
			}
			catch (MessagingException e1) {
			}
			catch (InterruptedException e2) {
				Thread.currentThread().interrupt();
			}
			latch2.countDown();
		});
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		StopWatch watch = new StopWatch();
		watch.start();
		factory.stop();
		watch.stop();
		assertThat(watch.lastTaskInfo().getTimeMillis() < 10000)
				.as("Expected < 10000, was: " + watch.lastTaskInfo().getTimeMillis())
				.isTrue();
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
	}

}
