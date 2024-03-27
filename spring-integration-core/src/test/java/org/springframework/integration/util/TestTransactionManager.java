/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * @author Mark Fisher
 */
@SuppressWarnings("serial")
public class TestTransactionManager extends AbstractPlatformTransactionManager {

	private final AtomicInteger commitCounter = new AtomicInteger();

	private final AtomicInteger rollbackCounter = new AtomicInteger();

	private volatile CountDownLatch latch = new CountDownLatch(1);

	private volatile TransactionDefinition lastDefinition;

	public void reset() {
		this.latch = new CountDownLatch(1);
		this.commitCounter.set(0);
		this.rollbackCounter.set(0);
	}

	public int getCommitCount() {
		return this.commitCounter.get();
	}

	public int getRollbackCount() {
		return this.rollbackCounter.get();
	}

	public TransactionDefinition getLastDefinition() {
		return this.lastDefinition;
	}

	public void waitForCompletion(long timeout) throws InterruptedException {
		this.latch.await(timeout, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		this.lastDefinition = definition;
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
		this.commitCounter.incrementAndGet();
	}

	@Override
	protected Object doGetTransaction() throws TransactionException {
		return new DefaultTransactionDefinition();
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
		this.rollbackCounter.incrementAndGet();
	}

	@Override
	protected void doCleanupAfterCompletion(Object o) {
		this.latch.countDown();
	}

}
