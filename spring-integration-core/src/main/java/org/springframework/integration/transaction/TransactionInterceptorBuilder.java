/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.transaction;

import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;

/**
 * Provides a fluent API to build a transaction interceptor. See
 * {@link TransactionAttribute} for property meanings; if a {@link TransactionAttribute}
 * is provided, the individual properties are ignored. If a
 * {@link TransactionManager} is not provided, a single instance of
 * {@link TransactionManager} will be discovered at runtime; if you have more
 * than one transaction manager, you must inject the one you want to use here.
 * <p>
 * When the {@code handleMessageAdvice} option is in use, this builder produces
 * {@link TransactionHandleMessageAdvice} instance.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class TransactionInterceptorBuilder {

	private final DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute();

	private final TransactionInterceptor transactionInterceptor;

	public TransactionInterceptorBuilder() {
		this(false);
	}

	public TransactionInterceptorBuilder(boolean handleMessageAdvice) {
		if (handleMessageAdvice) {
			this.transactionInterceptor = new TransactionHandleMessageAdvice();
		}
		else {
			this.transactionInterceptor = new TransactionInterceptor();
		}
		transactionAttribute(this.transactionAttribute);
	}

	public TransactionInterceptorBuilder propagation(Propagation propagation) {
		Assert.notNull(propagation, "'propagation' must not be null.");
		this.transactionAttribute.setPropagationBehavior(propagation.value());
		return this;
	}

	public TransactionInterceptorBuilder isolation(Isolation isolation) {
		Assert.notNull(isolation, "'isolation' must not be null.");
		this.transactionAttribute.setIsolationLevel(isolation.value());
		return this;
	}

	public TransactionInterceptorBuilder timeout(int timeout) {
		this.transactionAttribute.setTimeout(timeout);
		return this;
	}

	public TransactionInterceptorBuilder readOnly(boolean readOnly) {
		this.transactionAttribute.setReadOnly(readOnly);
		return this;
	}

	public final TransactionInterceptorBuilder transactionAttribute(TransactionAttribute transactionAttribute) {
		MatchAlwaysTransactionAttributeSource txAttributeSource = new MatchAlwaysTransactionAttributeSource();
		txAttributeSource.setTransactionAttribute(transactionAttribute);
		this.transactionInterceptor.setTransactionAttributeSource(txAttributeSource);
		return this;
	}

	/**
	 * Provide a {@link TransactionManager} instance to use.
	 * @param transactionManager the {@link TransactionManager} to use
	 * @return the builder
	 */
	public TransactionInterceptorBuilder transactionManager(TransactionManager transactionManager) {
		this.transactionInterceptor.setTransactionManager(transactionManager);
		return this;
	}

	public TransactionInterceptor build() {
		return this.transactionInterceptor;
	}

}
