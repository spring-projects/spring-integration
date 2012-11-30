/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.sql.DataSource;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author Gunnar Hillert
 *
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

}
