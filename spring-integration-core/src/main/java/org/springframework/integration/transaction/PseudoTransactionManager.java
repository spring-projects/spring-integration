/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transaction;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * An implementation of {@link org.springframework.transaction.PlatformTransactionManager}
 * that provides transaction-like semantics to
 * {@link org.springframework.integration.core.MessageSource}s that are not inherently
 * transactional. It does <b>not</b> make such
 * sources transactional; rather, together with a {@link TransactionSynchronizationFactory}, it provides
 * the ability to synchronize operations after a flow completes, via beforeCommit, afterCommit and
 * afterRollback operations.
 *
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @since 2.2
 *
 */
public class PseudoTransactionManager extends AbstractPlatformTransactionManager {

	private static final long serialVersionUID = 1L;

	@Override
	protected Object doGetTransaction() throws TransactionException {
		return new Object();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		//noop
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
		//noop
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
		//noop
	}

}
