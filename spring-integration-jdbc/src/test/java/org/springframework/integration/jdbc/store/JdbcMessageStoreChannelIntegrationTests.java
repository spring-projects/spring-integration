/*
 * Copyright 2002-present the original author or authors.
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

import java.io.NotSerializableException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.store.MessageGroup;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
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

	@Autowired
	private MessageChannel routingSlip;

	@Autowired
	@Qualifier("service-activator")
	private AbstractEndpoint serviceActivator;

	@BeforeEach
	public void clear() {
		Service.reset(1);
		for (MessageGroup group : messageStore) {
			messageStore.removeMessageGroup(group.getGroupId());
		}

		this.serviceActivator.start();
	}

	@AfterEach
	public void tearDown() {
		this.serviceActivator.stop();
	}

	@Test
	public void testSendAndActivate() throws Exception {
		input.send(new GenericMessage<>("foo"));
		Service.await(10000);
		assertThat(Service.messages.size()).isEqualTo(1);
	}

	@Test
	public void testSendAndActivateWithRollback() throws Exception {
		Service.fail = true;
		input.send(new GenericMessage<>("foo"));
		Service.await(10000);
		assertThat(Service.messages.size()).isGreaterThanOrEqualTo(1);
		// After a rollback in the poller the message is still waiting to be delivered
		// but unless we use a transaction here there is a chance that the queue will
		// appear empty....
		new TransactionTemplate(transactionManager).execute(status -> {

			synchronized (storeLock) {

				assertThat(input.getQueueSize()).isEqualTo(1);
				assertThat(input.receive(100L)).isNotNull();

			}
			return null;

		});
	}

	@Test
	@Repeat(2)
	public void testTransactionalSendAndReceive() throws Exception {

		boolean result = new TransactionTemplate(transactionManager).execute(status -> {

			synchronized (storeLock) {

				boolean result1 = input.send(new GenericMessage<>("foo"), 100L);
				// This will time out because the transaction has not committed yet
				try {
					Service.await(100);
					fail("Expected timeout");
				}
				catch (Exception e) {
					// expected
				}

				return result1;

			}

		});

		assertThat(result).as("Could not send message").isTrue();

		waitForMessage();

		StopWatch stopWatch = new StopWatch();
		try {
			stopWatch.start();
			// It might be null or not, but we don't want it to block
			input.receive(100L);
		}
		finally {
			stopWatch.stop();
		}

		// If the poll blocks in the RDBMS there is no way for the queue to respect the timeout
		assertThat(stopWatch.getTotalTimeMillis() < 10000).as("Timed out waiting for receive").isTrue();

	}

	protected void waitForMessage() throws InterruptedException {
		int n = 0;
		while (Service.messages.size() == 0) {
			if (n++ > 200) {
				fail("Message not received by Service");
			}
			Thread.sleep(50);
		}

		assertThat(Service.messages.size()).isEqualTo(1);
	}

	@Test
	public void testSameTransactionSendAndReceive() {

		final StopWatch stopWatch = new StopWatch();
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();

		// With a timeout on the transaction the test fails (after a long time) on the assertion in the transactional
		// receive.
		transactionDefinition.setTimeout(200);

		boolean result = new TransactionTemplate(transactionManager, transactionDefinition)
				.execute(status -> {

					synchronized (storeLock) {

						boolean result1 = input.send(new GenericMessage<String>("foo"), 500L);
						// This will time out because the transaction has not committed yet
						try {
							Service.await(1000);
							fail("Expected timeout");
						}
						catch (Exception e) {
							// expected
						}

						try {
							stopWatch.start();
							assertThat(input.receive(100L)).isNotNull();
						}
						finally {
							stopWatch.stop();
						}

						return result1;

					}

				});

		assertThat(result).as("Could not send message").isTrue();

		// So no activation
		assertThat(Service.messages.size()).isEqualTo(0);

		// If the poll blocks in the RDBMS there is no way for the queue to respect the timeout
		assertThat(stopWatch.getTotalTimeMillis() < 1000).as("Timed out waiting for receive").isTrue();

	}

	@Test
	public void testWithRoutingSlip() {
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.routingSlip.send(new GenericMessage<>("foo")))
				.withCauseInstanceOf(SerializationFailedException.class)
				.withRootCauseInstanceOf(NotSerializableException.class)
				.withStackTraceContaining(
						"org.springframework.integration.routingslip.ExpressionEvaluatingRoutingSlipRouteStrategy");
	}

	public static class Service {

		private static boolean fail = false;

		private static final List<String> messages = new CopyOnWriteArrayList<>();

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
