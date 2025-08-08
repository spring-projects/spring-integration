/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.transaction;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.util.Assert;

/**
 * A simple {@link TransactionSynchronizationFactory} implementation which produces
 * an {@link IntegrationResourceHolderSynchronization} with an {@link IntegrationResourceHolder}.
 *
 * @author Andreas Baer
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class PassThroughTransactionSynchronizationFactory implements TransactionSynchronizationFactory {

	@Override
	public TransactionSynchronization create(Object key) {
		Assert.notNull(key, "'key' must not be null");
		return new IntegrationResourceHolderSynchronization(new IntegrationResourceHolder(), key);
	}

}
