/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transaction;

/**
 * Strategy for implementing transaction synchronization processors.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
public interface TransactionSynchronizationProcessor {

	void processBeforeCommit(IntegrationResourceHolder holder);

	void processAfterCommit(IntegrationResourceHolder holder);

	void processAfterRollback(IntegrationResourceHolder holder);

}
