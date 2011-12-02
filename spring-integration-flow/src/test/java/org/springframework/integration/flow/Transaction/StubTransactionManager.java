/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.flow.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * @author David Turanski
 *
 */
@SuppressWarnings("serial")
public class StubTransactionManager extends AbstractPlatformTransactionManager {
	private static Log logger = LogFactory.getLog(StubTransactionManager.class);
	public boolean rolledback;
	public boolean committed;
	private String transaction="stub-transaction";
	/* (non-Javadoc)
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#doBegin(java.lang.Object, org.springframework.transaction.TransactionDefinition)
	 */
	@Override
	protected void doBegin(Object transaction, TransactionDefinition txDef) throws TransactionException {
		logger.debug("begining transaction:" + transaction +" def " + txDef); 
		rolledback = false;
		committed = false;
		
	}

	/* (non-Javadoc)
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#doCommit(org.springframework.transaction.support.DefaultTransactionStatus)
	 */
	@Override
	protected void doCommit(DefaultTransactionStatus arg0) throws TransactionException {
		logger.debug("committing transaction");
		committed = true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#doGetTransaction()
	 */
	@Override
	protected Object doGetTransaction() throws TransactionException {
		logger.debug("get transaction:" + this.transaction);
		return this.transaction;
	}

	/* (non-Javadoc)
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#doRollback(org.springframework.transaction.support.DefaultTransactionStatus)
	 */
	@Override
	protected void doRollback(DefaultTransactionStatus arg0) throws TransactionException {
		logger.debug("rolling back transaction");
		rolledback = true;
		
	}

}
