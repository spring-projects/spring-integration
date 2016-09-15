/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.file;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class FileInboundTransactionTests {

	@ClassRule
	public static TemporaryFolder tmpDir = new TemporaryFolder();

	@Autowired
	private SourcePollingChannelAdapter pseudoTx;

	@Autowired
	private SourcePollingChannelAdapter realTx;

	@Autowired
	private SubscribableChannel input;

	@Autowired
	private SubscribableChannel txInput;

	@Autowired
	private PollableChannel successChannel;

	@Autowired
	private PollableChannel failureChannel;

	@Autowired
	private DummyTxManager transactionManager;

	@Test
	public void testNoTx() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean crash = new AtomicBoolean();
		input.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				if (crash.get()) {
					throw new MessagingException("eek");
				}
				latch.countDown();
			}
		});
		pseudoTx.start();
		File file = new File(tmpDir.getRoot(), "si-test1/foo");
		file.createNewFile();
		Message<?> result = successChannel.receive(60000);
		assertNotNull(result);
		assertEquals(Boolean.TRUE, result.getPayload());
		assertFalse(file.delete());
		crash.set(true);
		file = new File(tmpDir.getRoot(), "si-test1/bar");
		file.createNewFile();
		result = failureChannel.receive(60000);
		assertNotNull(result);
		assertTrue(file.delete());
		assertEquals("foo", result.getPayload());
		pseudoTx.stop();
		assertFalse(transactionManager.getCommitted());
		assertFalse(transactionManager.getRolledBack());

		Object scanner = TestUtils.getPropertyValue(pseudoTx.getMessageSource(), "scanner");
		assertThat(scanner.getClass().getName(), containsString("FileReadingMessageSource$WatchServiceDirectoryScanner"));
	}

	@Test
	public void testTx() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean crash = new AtomicBoolean();
		txInput.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				if (crash.get()) {
					throw new MessagingException("eek");
				}
				latch.countDown();
			}
		});
		realTx.start();
		File file = new File(tmpDir.getRoot(), "si-test2/baz");
		file.createNewFile();
		Message<?> result = successChannel.receive(60000);
		assertNotNull(result);
		assertEquals(Boolean.TRUE, result.getPayload());
		assertTrue(file.delete());
		assertTrue(transactionManager.getCommitted());
		crash.set(true);
		file = new File(tmpDir.getRoot(), "si-test2/qux");
		file.createNewFile();
		result = failureChannel.receive(60000);
		assertNotNull(result);
		assertTrue(file.delete());
		assertEquals(Boolean.TRUE, result.getPayload());
		realTx.stop();
		assertTrue(transactionManager.getRolledBack());
	}

	public static class DummyTxManager extends AbstractPlatformTransactionManager {

		private static final long serialVersionUID = 1L;

		boolean committed;

		boolean rolledBack;

		@Override
		protected Object doGetTransaction() throws TransactionException {
			return new Object();
		}

		@Override
		protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		}

		@Override
		protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
			committed = true;
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
			rolledBack = true;
		}

		/**
		 * Evaluated in transactional onSuccessExpression - ensures we rolled back before evaluation
		 * @return
		 */
		public boolean getCommitted() {
			return committed;
		}

		/**
		 * Evaluated in transactional onFailureExpression - ensures we rolled back before evaluation
		 * @return
		 */
		public boolean getRolledBack() {
			return rolledBack;
		}

	}

}
