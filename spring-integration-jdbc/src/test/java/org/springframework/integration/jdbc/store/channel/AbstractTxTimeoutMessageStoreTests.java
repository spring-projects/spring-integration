/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

import org.junit.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
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
abstract class AbstractTxTimeoutMessageStoreTests {

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
	protected JdbcChannelMessageStore jdbcChannelMessageStore;

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

		Assert.assertTrue(String.format("Contdown latch did not count down from " +
				"%s to 0 in %sms.", maxMessages, maxWaitTime), testService.await(maxWaitTime));

		Thread.sleep(2000);

		Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(jdbcChannelMessageStore.getSizeOfIdCache()));
		Assert.assertEquals(Integer.valueOf(maxMessages), Integer.valueOf(testService.getSeenMessages().size()));
		Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(testService.getDuplicateMessagesCount()));
	}

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
						if (!result) return false;
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

}
