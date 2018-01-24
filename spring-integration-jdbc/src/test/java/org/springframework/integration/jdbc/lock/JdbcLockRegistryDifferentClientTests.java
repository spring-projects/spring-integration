/*
 * Copyright 2016-2018 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
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
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StopWatch;

/**
 * @author Dave Syer
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 4.3
 */
@ContextConfiguration("JdbcLockRegistryTests-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class JdbcLockRegistryDifferentClientTests {

	private static Log logger = LogFactory.getLog(JdbcLockRegistryDifferentClientTests.class);

	@Autowired
	private JdbcLockRegistry registry;

	@Autowired
	private LockRepository client;

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
		this.child.register(DefaultLockRepository.class);
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
			new SimpleAsyncTaskExecutor()
					.execute(() -> {
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
			pool.execute(() -> {
				Lock lock = registry1.obtain("foo");
				try {
					lock.lockInterruptibly();
					locked.add("1");
					latch.countDown();
				}
				catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
				finally {
					try {
						lock.unlock();
					}
					catch (Exception e2) {
						// ignore
					}
				}
			});

			pool.execute(() -> {
				Lock lock = registry2.obtain("foo");
				try {
					lock.lockInterruptibly();
					locked.add("2");
					latch.countDown();
				}
				catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
				finally {
					try {
						lock.unlock();
					}
					catch (Exception e2) {
						// ignore
					}
				}
			});

			assertTrue(latch.await(10, TimeUnit.SECONDS));
			// eventually they both get the lock and release it
			assertTrue(locked.contains("1"));
			assertTrue(locked.contains("2"));
			pool.shutdownNow();
		}
	}

	@Test
	public void testOnlyOneLock() throws Exception {
		for (int i = 0; i < 100; i++) {
			final BlockingQueue<String> locked = new LinkedBlockingQueue<>();
			final CountDownLatch latch = new CountDownLatch(20);
			ExecutorService pool = Executors.newFixedThreadPool(20);
			ArrayList<Callable<Boolean>> tasks = new ArrayList<>();

			for (int j = 0; j < 20; j++) {
				Callable<Boolean> task = () -> {
					DefaultLockRepository client = new DefaultLockRepository(this.dataSource);
					client.afterPropertiesSet();
					Lock lock = new JdbcLockRegistry(client).obtain("foo");
					try {
						if (locked.isEmpty() && lock.tryLock(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
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
				};
				tasks.add(task);
			}
			logger.info("Starting: " + i);
			pool.invokeAll(tasks);

			assertTrue(latch.await(10, TimeUnit.SECONDS));
			assertEquals(1, locked.size());
			assertTrue(locked.contains("done"));
			pool.shutdownNow();
		}
	}

	@Test
	public void testExclusiveAccess() throws Exception {
		DefaultLockRepository client1 = new DefaultLockRepository(dataSource);
		client1.afterPropertiesSet();
		final DefaultLockRepository client2 = new DefaultLockRepository(dataSource);
		client2.afterPropertiesSet();
		Lock lock1 = new JdbcLockRegistry(client1).obtain("foo");
		final BlockingQueue<Integer> data = new LinkedBlockingQueue<>();
		final CountDownLatch latch1 = new CountDownLatch(1);
		lock1.lockInterruptibly();
		new SimpleAsyncTaskExecutor()
				.execute(() -> {
					Lock lock2 = new JdbcLockRegistry(client2).obtain("foo");
					try {
						latch1.countDown();
						StopWatch stopWatch = new StopWatch();
						stopWatch.start();
						lock2.lockInterruptibly();
						stopWatch.stop();
						data.add(4);
						Thread.sleep(10);
						data.add(5);
						Thread.sleep(10);
						data.add(6);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					finally {
						lock2.unlock();
					}
				});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		data.add(1);
		Thread.sleep(100);
		data.add(2);
		Thread.sleep(100);
		data.add(3);
		lock1.unlock();
		for (int i = 0; i < 6; i++) {
			Integer integer = data.poll(10, TimeUnit.SECONDS);
			assertNotNull(integer);
			assertEquals(i + 1, integer.intValue());
		}
	}

}
