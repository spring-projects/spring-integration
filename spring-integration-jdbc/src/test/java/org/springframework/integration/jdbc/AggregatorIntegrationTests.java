/*
 * Copyright 2014-2018 the original author or authors.
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

package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Artem Bilan
 *
 * @since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class AggregatorIntegrationTests {

	@Autowired
	private MessageChannel transactionalAggregatorInput;

	@Autowired
	private MessageGroupStore messageGroupStore;

	@Autowired
	private AggregatingMessageHandler aggregatingMessageHandler;

	@After
	public void tearDown() {
		this.aggregatingMessageHandler.stop();
	}

	@Test
	public void testTransactionalAggregatorGroupTimeout() throws InterruptedException {
		this.transactionalAggregatorInput.send(new GenericMessage<Integer>(1, stubHeaders(1, 2, 1)));

		assertTrue(RollbackTxSync.latch.await(20, TimeUnit.SECONDS));

		//As far as we have been within TX, the message group should still be in the MessageStore
		assertEquals(1, this.messageGroupStore.messageGroupSize(1));
	}

	private Map<String, Object> stubHeaders(int sequenceNumber, int sequenceSize, int correlationId) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, sequenceNumber);
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, sequenceSize);
		headers.put(IntegrationMessageHeaderAccessor.CORRELATION_ID, correlationId);
		return headers;
	}

	@SuppressWarnings("unused")
	private static class ExceptionMessageHandler implements MessageHandler {

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			TransactionSynchronizationManager.registerSynchronization(new RollbackTxSync());
			throw new RuntimeException("intentional");
		}

	}

	private static class RollbackTxSync extends TransactionSynchronizationAdapter {

		public static CountDownLatch latch = new CountDownLatch(1);

		RollbackTxSync() {
			super();
		}

		@Override
		public void afterCompletion(int status) {
			if (TransactionSynchronization.STATUS_ROLLED_BACK == status) {
				latch.countDown();
			}
		}

	}

}
