/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.filters.ResettableFileListFilter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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

		Object scanner = TestUtils.getPropertyValue(pseudoTx.getMessageSource(), "scanner");
		assertThat(scanner.getClass().getName()).contains("FileReadingMessageSource$WatchServiceDirectoryScanner");

		@SuppressWarnings("unchecked")
		ResettableFileListFilter<File> fileListFilter =
				spy(TestUtils.getPropertyValue(scanner, "filter", ResettableFileListFilter.class));

		new DirectFieldAccessor(scanner).setPropertyValue("filter", fileListFilter);

		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean crash = new AtomicBoolean();
		input.subscribe(message -> {
			if (crash.get()) {
				throw new MessagingException("eek");
			}
			latch.countDown();
		});
		pseudoTx.start();
		File file = new File(tmpDir.getRoot(), "si-test1/foo");
		file.createNewFile();
		Message<?> result = successChannel.receive(60000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(Boolean.TRUE);
		assertThat(file.delete()).isFalse();
		crash.set(true);
		file = new File(tmpDir.getRoot(), "si-test1/bar");
		file.createNewFile();
		result = failureChannel.receive(60000);
		assertThat(result).isNotNull();
		assertThat(file.delete()).isTrue();
		assertThat(result.getPayload()).isEqualTo("foo");
		pseudoTx.stop();
		assertThat(transactionManager.getCommitted()).isFalse();
		assertThat(transactionManager.getRolledBack()).isFalse();

		verify(fileListFilter).remove(new File(tmpDir.getRoot(), "si-test1/foo"));
	}

	@Test
	public void testTx() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean crash = new AtomicBoolean();
		txInput.subscribe(message -> {
			if (crash.get()) {
				throw new MessagingException("eek");
			}
			latch.countDown();
		});
		realTx.start();
		File file = new File(tmpDir.getRoot(), "si-test2/baz");
		file.createNewFile();
		Message<?> result = successChannel.receive(60000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(Boolean.TRUE);
		assertThat(file.delete()).isTrue();
		assertThat(transactionManager.getCommitted()).isTrue();
		crash.set(true);
		file = new File(tmpDir.getRoot(), "si-test2/qux");
		file.createNewFile();
		result = failureChannel.receive(60000);
		assertThat(result).isNotNull();
		assertThat(file.delete()).isTrue();
		assertThat(result.getPayload()).isEqualTo(Boolean.TRUE);
		realTx.stop();
		assertThat(transactionManager.getRolledBack()).isTrue();
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
