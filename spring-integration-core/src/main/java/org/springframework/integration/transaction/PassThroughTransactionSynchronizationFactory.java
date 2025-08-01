/*
 * Copyright 2017-present the original author or authors.
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
