/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transaction;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Strategy for implementing factories that create {@link TransactionSynchronization}.
 *
 * @author Oleg Zhurakousky
 *
 * @since 2.2
 *
 */
public interface TransactionSynchronizationFactory {

	TransactionSynchronization create(Object key);

}
