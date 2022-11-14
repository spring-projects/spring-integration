/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc.store;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 */
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

	@Autowired
	@Qualifier("service-relay")
	private AbstractEndpoint serviceRelay;

	@Before
	public void clear() {
		for (MessageGroup group : this.messageStore) {
			this.messageStore.removeMessageGroup(group.getGroupId());
		}
	}

	@After
	public void tearDown() {
		this.serviceRelay.stop();
	}

	@Test
	public void testSameTransactionDifferentChannelSendAndReceive() throws Exception {
		Service.reset(1);
		assertThat(this.durable.receive(100L)).isNull();
		assertThat(this.relay.receive(100L)).isNull();
		final StopWatch stopWatch = new StopWatch();

		boolean result =
				new TransactionTemplate(this.transactionManager)
						.execute(status -> {

							synchronized (this.storeLock) {

								boolean result1 = this.relay.send(new GenericMessage<>("foo"), 500L);
								// This will time out because the transaction has not committed yet
								try {
									Service.await(100);
									fail("Expected timeout");
								}
								catch (Exception e) {
									// expected
								}

								try {
									stopWatch.start();
									// It hasn't arrive yet because we are still in the sending transaction
									assertThat(this.durable.receive(100L)).isNull();
								}
								finally {
									stopWatch.stop();
								}

								return result1;

							}

						});

		assertThat(result).as("Could not send message").isTrue();
		// If the poll blocks in the RDBMS there is no way for the queue to respect the timeout
		assertThat(stopWatch.getTotalTimeMillis() < 10000).as("Timed out waiting for receive").isTrue();

		Service.await(10000);
		// Eventual activation
		assertThat(Service.messages.size()).isEqualTo(1);

		/*
		 * Without the storeLock:
		 *
		 * If we do this in a transaction it deadlocks occasionally. Without a transaction and it's pretty much every
		 * time.
		 *
		 * With the storeLock: It doesn't deadlock as long as the lock is injected into the poller as well.
		 */
		new TransactionTemplate(this.transactionManager)
				.execute(status -> {
					synchronized (this.storeLock) {

						try {
							stopWatch.start();
							this.durable.receive(100L);
							return null;
						}
						finally {
							stopWatch.stop();
						}

					}
				});

		// If the poll blocks in the RDBMS there is no way for the queue to respect the timeout
		assertThat(stopWatch.getTotalTimeMillis() < 10000).as("Timed out waiting for receive").isTrue();

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
