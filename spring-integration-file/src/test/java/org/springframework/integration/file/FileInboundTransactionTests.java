/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileInboundTransactionTests {

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

	@Value("${java.io.tmpdir}")
	private String tmpDir;

	@Test
	public void testNoTx() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean crash = new AtomicBoolean();
		input.subscribe(new MessageHandler() {

			public void handleMessage(Message<?> message) throws MessagingException {
				System.out.println(message);
				if (crash.get()) {
					throw new MessagingException("eek");
				}
				latch.countDown();
			}
		});
		pseudoTx.start();
		new File(tmpDir + "/si-test1").mkdir();
		File file = new File(tmpDir + "/si-test1/foo");
		file.createNewFile();
		Message<?> result = successChannel.receive(10000);
		assertNotNull(result);
		assertEquals(Boolean.TRUE, result.getPayload());
		System.out.println(result);
		assertFalse(file.delete());
		crash.set(true);
		file = new File(tmpDir + "/si-test1/bar");
		file.createNewFile();
		result = failureChannel.receive(10000);
		assertNotNull(result);
		System.out.println(result);
		assertTrue(file.delete());
		assertEquals("foo", result.getPayload());
		pseudoTx.stop();
		assertFalse(transactionManager.getCommitted());
		assertFalse(transactionManager.getRolledBack());
	}

	@Test
	public void testTx() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean crash = new AtomicBoolean();
		txInput.subscribe(new MessageHandler() {

			public void handleMessage(Message<?> message) throws MessagingException {
				System.out.println(message);
				if (crash.get()) {
					throw new MessagingException("eek");
				}
				latch.countDown();
			}
		});
		realTx.start();
		new File(tmpDir + "/si-test2").mkdir();
		File file = new File(tmpDir + "/si-test2/baz");
		file.createNewFile();
		Message<?> result = successChannel.receive(10000);
		assertNotNull(result);
		assertEquals(Boolean.TRUE, result.getPayload());
		assertTrue(file.delete());
		System.out.println(result);
		assertTrue(transactionManager.getCommitted());
		crash.set(true);
		file = new File(tmpDir + "/si-test2/qux");
		file.createNewFile();
		result = failureChannel.receive(10000);
		assertNotNull(result);
		System.out.println(result);
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
