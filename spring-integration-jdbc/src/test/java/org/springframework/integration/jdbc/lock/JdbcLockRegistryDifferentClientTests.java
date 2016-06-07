/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.integration.jdbc.lock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
@ContextConfiguration("JdbcLockRegistryTests-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext // close at the end after class
public class JdbcLockRegistryDifferentClientTests {

	private static Log logger = LogFactory.getLog(JdbcLockRegistryDifferentClientTests.class);

	@Autowired
	private JdbcLockRegistry registry;

	@Autowired
	private JdbcClient client;

	@Autowired
	private ConfigurableApplicationContext context;

	private AnnotationConfigApplicationContext child;

	@Autowired
	private DataSource dataSource;

	@Before
	public void clear() {
		this.registry.expireUnusedOlderThan(0);
		this.client.close();
		this.child = new AnnotationConfigApplicationContext();
		this.child.register(JdbcClient.class);
		this.child.setParent(this.context);
		this.child.refresh();
	}

	@After
	public void close() {
		if (this.child != null) {
			this.child.close();
		}
	}

	@Test
	public void testSecondThreadLoses() throws Exception {

		for (int i = 0; i < 100; i++) {

			final JdbcLockRegistry registry1 = this.registry;
			final JdbcLockRegistry registry2 = this.child.getBean(JdbcLockRegistry.class);
			final Lock lock1 = registry1.obtain("foo");
			final AtomicBoolean locked = new AtomicBoolean();
			final CountDownLatch latch1 = new CountDownLatch(1);
			final CountDownLatch latch2 = new CountDownLatch(1);
			final CountDownLatch latch3 = new CountDownLatch(1);
			lock1.lockInterruptibly();
			Executors.newSingleThreadExecutor().execute(new Runnable() {

				@Override
				public void run() {
					Lock lock2 = registry2.obtain("foo");
					try {
						latch1.countDown();
						lock2.lockInterruptibly();
						latch2.await(10, TimeUnit.SECONDS);
						locked.set(true);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					finally {
						lock2.unlock();
						latch3.countDown();
					}
				}
			});
			assertTrue(latch1.await(10, TimeUnit.SECONDS));
			assertFalse(locked.get());
			lock1.unlock();
			latch2.countDown();
			assertTrue(latch3.await(10, TimeUnit.SECONDS));
			assertTrue(locked.get());

		}

	}

	@Test
	public void testBothLock() throws Exception {

		for (int i = 0; i < 100; i++) {

			final JdbcLockRegistry registry1 = this.registry;
			final JdbcLockRegistry registry2 = this.child.getBean(JdbcLockRegistry.class);
			final List<String> locked = new ArrayList<>();
			final CountDownLatch latch = new CountDownLatch(2);
			ExecutorService pool = Executors.newFixedThreadPool(2);
			pool.execute(new Runnable() {

				@Override
				public void run() {
					Lock lock = registry1.obtain("foo");
					try {
						lock.lockInterruptibly();
						locked.add("1");
						latch.countDown();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					finally {
						try {
							lock.unlock();
						}
						catch (Exception e) {
							// ignore
						}
					}
				}
			});

			pool.execute(new Runnable() {

				@Override
				public void run() {
					Lock lock = registry2.obtain("foo");
					try {
						lock.lockInterruptibly();
						locked.add("2");
						latch.countDown();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					finally {
						try {
							lock.unlock();
						}
						catch (Exception e) {
							// ignore
						}
					}
				}
			});

			assertTrue(latch.await(10, TimeUnit.SECONDS));
			// eventually they both get the lock and release it
			assertTrue(locked.contains("1"));
			assertTrue(locked.contains("2"));

		}

	}

	@Test
	public void testOnlyOneLock() throws Exception {

		for (int i = 0; i < 100; i++) {

			final List<String> locked = new ArrayList<>();
			final CountDownLatch latch = new CountDownLatch(20);
			ExecutorService pool = Executors.newFixedThreadPool(6);
			ArrayList<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();
			for (int j = 0; j < 20; j++) {
				final JdbcClient client = new JdbcClient(this.dataSource);
				this.context.getAutowireCapableBeanFactory().autowireBean(client);
				Callable<Boolean> task = new Callable<Boolean>() {

					@Override
					public Boolean call() {
						Lock lock = new JdbcLockRegistry(client).obtain("foo");
						try {
							if (locked.isEmpty() && lock.tryLock()) {
								if (locked.isEmpty()) {
									locked.add("done");
									return true;
								}
							}
						}
						finally {
							try {
								lock.unlock();
							}
							catch (Exception e) {
								// ignore
							}
							latch.countDown();
						}
						return false;
					}
				};
				tasks.add(task);
			}
			logger.info("Starting: " + i);
			pool.invokeAll(tasks);

			assertTrue(latch.await(10, TimeUnit.SECONDS));
			// eventually they both get the lock and release it
			assertEquals(1, locked.size());
			assertTrue(locked.contains("done"));

		}

	}
}
