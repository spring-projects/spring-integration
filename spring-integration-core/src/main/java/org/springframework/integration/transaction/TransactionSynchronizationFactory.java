/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
