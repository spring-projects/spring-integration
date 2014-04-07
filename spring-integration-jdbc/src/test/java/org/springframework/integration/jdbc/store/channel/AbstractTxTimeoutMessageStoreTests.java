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
package org.springframework.integration.jdbc.store.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext // close at the end after class
public abstract class AbstractTxTimeoutMessageStoreTests {

	private static final Log log = LogFactory.getLog(AbstractTxTimeoutMessageStoreTests.class);

	@Autowired
	protected DataSource dataSource;

	@Autowired
	protected MessageChannel inputChannel;

	@Autowired
	protected PlatformTransactionManager transactionManager;

	@Autowired
	protected TestService testService;

	@Autowired
	@Qualifier("store")
	protected JdbcChannelMessageStore jdbcChannelMessageStore;

	@Autowired
	private MessageChannel first;

	@Autowired
	private CountDownLatch successfulLatch;

	@Autowired
	private AtomicInteger errorAtomicInteger;

	@Autowired
	protected PollableChannel priorityChannel;

	@Test
	public void test() throws InterruptedException {

		int maxMessages = 10;
		int maxWaitTime = 30000;

		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		transactionTemplate.setIsolationLevel(Isolation.READ_COMMITTED.value());
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		for (int i = 1; i <= maxMessages; ++i) {
			final String message = "TEST MESSAGE " + i;
			log.info("Sending message: " + message);

			transactionTemplate.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					inputChannel.send(MessageBuilder.withPayload(message).build());
				}
			});

			log.info(String.format("Done sending message %s of %s: %s", i, maxMessages, message));
		}

		log.info("Done sending " + maxMessages + " messages.");

		Assert.assertTrue(String.format("Countdown latch did not count down from " +
				"%s to 0 in %sms.", maxMessages, maxWaitTime), testService.await(maxWaitTime));

		Thread.sleep(2000);

		Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(jdbcChannelMessageStore.getSizeOfIdCache()));
		Assert.assertEquals(Integer.valueOf(maxMessages), Integer.valueOf(testService.getSeenMessages().size()));
		Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(testService.getDuplicateMessagesCount()));
	}

	@Test
	public void testInt2993IdCacheConcurrency() throws InterruptedException, ExecutionException {
		final String groupId = "testInt2993Group";
		for (int i = 0; i < 100; i++) {
			this.jdbcChannelMessageStore.addMessageToGroup(groupId, new GenericMessage<String>("testInt2993Message"));
		}

		ExecutorService executorService = Executors.newCachedThreadPool();
		CompletionService<Boolean> completionService = new ExecutorCompletionService<Boolean>(executorService);

		final int concurrency = 5;

		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		for (int i = 0; i < concurrency; i++) {
			completionService.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					for (int i = 0; i < 100; i++) {
						boolean result = transactionTemplate.execute(new TransactionCallback<Boolean>() {
							@Override
							public Boolean doInTransaction(TransactionStatus status) {
								Message<?> message = null;
								try {
									message = jdbcChannelMessageStore.pollMessageFromGroup(groupId);
								}
								catch (Exception e) {
									log.error("IdCache race condition.", e);
									return false;
								}
								try {
									Thread.sleep(10);
								}
								catch (InterruptedException e) {
									log.error(e);
								}
								if (message != null) {
									jdbcChannelMessageStore.removeFromIdCache(message.getHeaders().getId().toString());
								}
								return true;
							}
						});
						if (!result) {
							return false;
						}
					}

					return true;
				}
			});
		}

		for (int j = 0; j < concurrency; j++) {
			assertTrue(completionService.take().get());
		}

		executorService.shutdown();
		assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS));
	}

	@Test
	public void testInt3181ConcurrentPolling() throws InterruptedException {
		for (int i = 0; i < 10; i++) {
			this.first.send(new GenericMessage<Object>("test"));
		}

		assertTrue(this.successfulLatch.await(5, TimeUnit.SECONDS));

		assertEquals(0, errorAtomicInteger.get());
	}

	@Test
	public void testMessageSequenceColumn() throws InterruptedException {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
		String messageGroup = "TEST_MESSAGE_GROUP";
		this.jdbcChannelMessageStore.addMessageToGroup(messageGroup, new GenericMessage<Object>("foo"));
		// The simple sleep to to be sure that messages are stored with different 'CREATED_DATE'
		Thread.sleep(10);
		this.jdbcChannelMessageStore.addMessageToGroup(messageGroup, new GenericMessage<Object>("bar"));

		List<Map<String, Object>> result =
				jdbcTemplate.queryForList("SELECT MESSAGE_SEQUENCE FROM INT_CHANNEL_MESSAGE " +
				"WHERE GROUP_KEY = ? ORDER BY CREATED_DATE", UUIDConverter.getUUID(messageGroup).toString());
		assertEquals(2, result.size());
		Object messageSequence1 = result.get(0).get("MESSAGE_SEQUENCE");
		Object messageSequence2 = result.get(1).get("MESSAGE_SEQUENCE");
		assertNotNull(messageSequence1);
		assertThat(messageSequence1, Matchers.instanceOf(Number.class));
		assertNotNull(messageSequence2);
		assertThat(messageSequence2, Matchers.instanceOf(Number.class));

		assertThat(((Number) messageSequence1).longValue(), Matchers.lessThan(((Number) messageSequence2).longValue()));

		this.jdbcChannelMessageStore.removeMessageGroup(messageGroup);
	}

	@Test
	public void testPriorityChannel() throws Exception {
		Message<String> message = MessageBuilder.withPayload("1").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 1).build();
		priorityChannel.send(message);
		message = MessageBuilder.withPayload("-1").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, -1).build();
		priorityChannel.send(message);
		message = MessageBuilder.withPayload("3").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 3).build();
		priorityChannel.send(message);
		message = MessageBuilder.withPayload("0").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 0).build();
		priorityChannel.send(message);
		message = MessageBuilder.withPayload("2").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 2).build();
		priorityChannel.send(message);
		message = MessageBuilder.withPayload("none").build();
		priorityChannel.send(message);
		message = MessageBuilder.withPayload("31").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 3).build();
		priorityChannel.send(message);

		Message<?> receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("3", receive.getPayload());

		receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("31", receive.getPayload());

		receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("2", receive.getPayload());

		receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("1", receive.getPayload());

		receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("0", receive.getPayload());

		receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("-1", receive.getPayload());

		receive = priorityChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("none", receive.getPayload());
	}

}
