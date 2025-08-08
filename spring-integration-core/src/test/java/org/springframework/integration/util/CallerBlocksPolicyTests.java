/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0.3
 *
 */
public class CallerBlocksPolicyTests {

	@Test
	public void test0() throws Exception {
		ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
		te.setCorePoolSize(1);
		te.setMaxPoolSize(1);
		te.setQueueCapacity(0);
		te.setRejectedExecutionHandler(new CallerBlocksPolicy(10));
		te.initialize();
		AtomicReference<Throwable> e = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);
		Runnable task = new Runnable() {

			@Override
			public void run() {
				try {
					te.execute(this);
				}
				catch (TaskRejectedException tre) {
					e.set(tre.getCause());
				}
				latch.countDown();
			}
		};
		try {
			te.execute(task);
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(e.get())
					.isInstanceOf(RejectedExecutionException.class)
					.hasMessage("Max wait time expired to queue task");
		}
		finally {
			te.destroy();
		}
	}

	@Test
	public void test1() throws Exception {
		ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
		te.setCorePoolSize(2);
		te.setMaxPoolSize(2);
		te.setQueueCapacity(1);
		te.setRejectedExecutionHandler(new CallerBlocksPolicy(10000));
		te.initialize();
		AtomicReference<Throwable> e = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(3);
		try {
			te.execute(() -> {
				try {
					Runnable foo =
							() -> {
								try {
									Thread.sleep(10);
								}
								catch (InterruptedException e1) {
									Thread.currentThread().interrupt();
									throw new RuntimeException(e1);
								}
								finally {
									latch.countDown();
								}
							};
					te.execute(foo);
					te.execute(foo); // this one will be queued
					te.execute(foo); // this one will be blocked and successful later
				}
				catch (TaskRejectedException tre) {
					e.set(tre.getCause());
				}
			});
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(e.get()).isNull();
		}
		finally {
			te.destroy();
		}
	}

}
