/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.transaction;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * A simple {@link TransactionSynchronizationFactory} implementation which produces
 * an {@link IntegrationResourceHolderSynchronization} and registers
 * an {@link IntegrationResourceHolder} under the provided {@code key} with
 * the current transaction scope.
 * <p>
 * If resource under the provided {@code key} is already registered, the factory returns {@code null}.
 *
 * @author Andreas Baer
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see TransactionSynchronizationManager#bindResource(Object, Object)
 */
public class PassThroughTransactionSynchronizationFactory implements TransactionSynchronizationFactory {


	@Override
	public TransactionSynchronization create(Object key) {
		Assert.notNull(key, "'key' must not be null");
		if (!TransactionSynchronizationManager.hasResource(key)) {
			IntegrationResourceHolderSynchronization synchronization =
					new IntegrationResourceHolderSynchronization(new IntegrationResourceHolder(), key);
			TransactionSynchronizationManager.bindResource(key, synchronization.getResourceHolder());
			return synchronization;
		}

		return null;
	}

}
