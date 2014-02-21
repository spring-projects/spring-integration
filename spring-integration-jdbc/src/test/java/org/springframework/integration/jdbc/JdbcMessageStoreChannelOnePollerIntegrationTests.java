/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext // close at the end after class
public class JdbcMessageStoreChannelOnePollerIntegrationTests {

	@Autowired
	private QueueChannel relay;

	@Autowired
	private QueueChannel durable;

	@Autowired
	@Qualifier("lock")
	private Object storeLock;

	@Autowired
	private JdbcMessageStore messageStore;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Before
	public void clear() {
		for (MessageGroup group : messageStore) {
			messageStore.removeMessageGroup(group.getGroupId());
		}
	}

	@Test
	// @Repeat(50)
	public void testSameTransactionDifferentChannelSendAndReceive() throws Exception {

		Service.reset(1);
		assertNull(durable.receive(100L));
		assertNull(relay.receive(100L));
		final StopWatch stopWatch = new StopWatch();

		boolean result = new TransactionTemplate(transactionManager).execute(new TransactionCallback<Boolean>() {

			@Override
			public Boolean doInTransaction(TransactionStatus status) {

				synchronized (storeLock) {

					boolean result = relay.send(new GenericMessage<String>("foo"), 500L);
					// This will time out because the transaction has not committed yet
					try {
						Service.await(1000);
						fail("Expected timeout");
					} catch (Exception e) {
						// expected
					}

					try {
						stopWatch.start();
						// It hasn't arrive yet because we are still in the sending transaction
						assertNull(durable.receive(100L));
					} finally {
						stopWatch.stop();
					}

					return result;

				}

			}
		});

		assertTrue("Could not send message", result);
		// If the poll blocks in the RDBMS there is no way for the queue to respect the timeout
		assertTrue("Timed out waiting for receive", stopWatch.getTotalTimeMillis() < 10000);

		Service.await(1000);
		// Eventual activation
		assertEquals(1, Service.messages.size());

		/*
		 * Without the storeLock:
		 *
		 * If we do this in a transaction it deadlocks occasionally. Without a transaction and it's pretty much every
		 * time.
		 *
		 * With the storeLock: It doesn't deadlock as long as the lock is injected into the poller as well.
		 */
		new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {

			@Override
			public Void doInTransaction(TransactionStatus status) {
				synchronized (storeLock) {

					try {
						stopWatch.start();
						durable.receive(100L);
						return null;
					} finally {
						stopWatch.stop();
					}

				}
			}

		});

		// If the poll blocks in the RDBMS there is no way for the queue to respect the timeout
		assertTrue("Timed out waiting for receive", stopWatch.getTotalTimeMillis() < 10000);

	}

	public static class Service {
		private static boolean fail = false;

		private static List<String> messages = new CopyOnWriteArrayList<String>();

		private static CountDownLatch latch = new CountDownLatch(0);

		public static void reset(int count) {
			fail = false;
			messages.clear();
			latch = new CountDownLatch(count);
		}

		public static void await(long timeout) throws InterruptedException {
			if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
				throw new IllegalStateException("Timed out waiting for message");
			}
		}

		public String echo(String input) {
			messages.add(input);
			latch.countDown();
			if (fail) {
				throw new RuntimeException("Planned failure");
			}
			return input;
		}
	}

}
