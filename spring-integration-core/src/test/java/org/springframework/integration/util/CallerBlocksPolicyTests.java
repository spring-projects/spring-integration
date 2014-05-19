/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.util;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Gary Russell
 * @since 3.0.3
 *
 */
public class CallerBlocksPolicyTests {

	@Test
	public void test0() throws Exception {
		final ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
		te.setCorePoolSize(1);
		te.setMaxPoolSize(1);
		te.setQueueCapacity(0);
		te.setRejectedExecutionHandler(new CallerBlocksPolicy(1000));
		te.initialize();
		final AtomicReference<Throwable> e = new AtomicReference<Throwable>();
		final CountDownLatch latch = new CountDownLatch(1);
		te.execute(new Runnable() {

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
		});
		assertTrue(latch.await(10,  TimeUnit.SECONDS));
		assertThat(e.get(), instanceOf(RejectedExecutionException.class));
		assertEquals("Max wait time expired to queue task", e.get().getMessage());
	}

	@Test
	public void test1() throws Exception {
		final ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
		te.setCorePoolSize(2);
		te.setMaxPoolSize(2);
		te.setQueueCapacity(1);
		te.setRejectedExecutionHandler(new CallerBlocksPolicy(10000));
		te.initialize();
		final AtomicReference<Throwable> e = new AtomicReference<Throwable>();
		final CountDownLatch latch = new CountDownLatch(3);
		te.execute(new Runnable() {

			@Override
			public void run() {
				try {
					Runnable foo = new Runnable() {

						@Override
						public void run() {
							try {
								Thread.sleep(1000);
							}
							catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								throw new RuntimeException();
							}
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
			}
		});
		assertTrue(latch.await(10,  TimeUnit.SECONDS));
		assertNull(e.get());
	}

}
