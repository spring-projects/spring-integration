/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
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
