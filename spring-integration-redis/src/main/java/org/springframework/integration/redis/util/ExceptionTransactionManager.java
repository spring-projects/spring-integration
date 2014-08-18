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
package org.springframework.integration.redis.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
/**
 * An implementation of {@link PlatformTransactionManager} that provides transaction-like semantics to
 * {@link MessageSource}s that are not inherently transactional. It does <b>not</b> make such
 * sources transactional; rather, together with a {@link TransactionSynchronizationFactory}, it provides
 * the ability to synchronize operations after a flow completes, via beforeCommit, afterCommit and
 * afterRollback operations.
 *
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @since 2.2
 *
 */
public class ExceptionTransactionManager extends AbstractPlatformTransactionManager {

	private static final long serialVersionUID = 1L;

	private volatile RedisTemplate template;

	public void setTemplate(RedisTemplate template) {
		this.template = template;
	}

	@Override
	protected Object doGetTransaction() throws TransactionException {
		return new Object();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		this.template.setEnableTransactionSupport(true);
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
		throw new RuntimeException();
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
	}
}
