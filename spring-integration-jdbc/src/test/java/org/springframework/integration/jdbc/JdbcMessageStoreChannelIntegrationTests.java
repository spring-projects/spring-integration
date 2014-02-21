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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext // close at the end after class
public class JdbcMessageStoreChannelIntegrationTests {

	@Autowired
	private QueueChannel input;

	@Autowired
	@Qualifier("lock")
	private Object storeLock;

	@Autowired
	private JdbcMessageStore messageStore;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Before
	public void clear() {
		Service.reset(1);
		for (MessageGroup group : messageStore) {
			messageStore.removeMessageGroup(group.getGroupId());
		}
	}

	@Test
	public void testSendAndActivate() throws Exception {
		input.send(new GenericMessage<String>("foo"));
		Service.await(1000);
		assertEquals(1, Service.messages.size());
	}

	@Test
	public void testSendAndActivateWithRollback() throws Exception {
		Service.fail = true;
		input.send(new GenericMessage<String>("foo"));
		Service.await(1000);
		assertThat(Service.messages.size(), Matchers.greaterThanOrEqualTo(1));
		// After a rollback in the poller the message is still waiting to be delivered
		// but unless we use a transaction here there is a chance that the queue will
		// appear empty....
		new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {

			@Override
			public Void doInTransaction(TransactionStatus status) {

				synchronized (storeLock) {

					assertEquals(1, input.getQueueSize());
					assertNotNull(input.receive(100L));

				}
				return null;

			}
		});
	}

	@Test
	@Repeat(2)
	public void testTransactionalSendAndReceive() throws Exception {

		boolean result = new TransactionTemplate(transactionManager).execute(new TransactionCallback<Boolean>() {

			@Override
			public Boolean doInTransaction(TransactionStatus status) {

				synchronized (storeLock) {

					boolean result = input.send(new GenericMessage<String>("foo"), 500L);
					// This will time out because the transaction has not committed yet
					try {
						Service.await(3000);
						fail("Expected timeout");
					} catch (Exception e) {
						// expected
					}

					return result;

				}

			}
		});

		assertTrue("Could not send message", result);

		waitForMessage();

		StopWatch stopWatch = new StopWatch();
		try {
			stopWatch.start();
			// It might be null or not, but we don't want it to block
			input.receive(100L);
		} finally {
			stopWatch.stop();
		}

		// If the poll blocks in the RDBMS there is no way for the queue to respect the timeout
		assertTrue("Timed out waiting for receive", stopWatch.getTotalTimeMillis() < 10000);

	}

	protected void waitForMessage() throws InterruptedException {
		int n = 0;
		while (Service.messages.size() == 0) {
			if (n++ > 200) {
				fail("Message not received by Service");
			}
			Thread.sleep(50);
		}

		assertEquals(1, Service.messages.size());
	}

	@Test
	public void testSameTransactionSendAndReceive() throws Exception {

		final StopWatch stopWatch = new StopWatch();
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();

		// With a timeout on the transaction the test fails (after a long time) on the assertion in the transactional
		// receive.
		transactionDefinition.setTimeout(200);

		boolean result = new TransactionTemplate(transactionManager, transactionDefinition)
				.execute(new TransactionCallback<Boolean>() {

					@Override
					public Boolean doInTransaction(TransactionStatus status) {

						synchronized (storeLock) {

							boolean result = input.send(new GenericMessage<String>("foo"), 500L);
							// This will time out because the transaction has not committed yet
							try {
								Service.await(1000);
								fail("Expected timeout");
							} catch (Exception e) {
								// expected
							}

							try {
								stopWatch.start();
								assertNotNull(input.receive(100L));
							} finally {
								stopWatch.stop();
							}

							return result;

						}

					}
				});

		assertTrue("Could not send message", result);

		// So no activation
		assertEquals(0, Service.messages.size());

		// If the poll blocks in the RDBMS there is no way for the queue to respect the timeout
		assertTrue("Timed out waiting for receive", stopWatch.getTotalTimeMillis() < 1000);

	}

	public static class Service {
		private static boolean fail = false;

		private static List<String> messages = new CopyOnWriteArrayList<String>();

		private static CountDownLatch latch;

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
