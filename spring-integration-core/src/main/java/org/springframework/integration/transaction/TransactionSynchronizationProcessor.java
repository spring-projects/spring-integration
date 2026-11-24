/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
