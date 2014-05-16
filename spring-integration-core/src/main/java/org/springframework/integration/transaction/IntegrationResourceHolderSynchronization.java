/*
 * Copyright 2014 the original author or authors.
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

import org.springframework.transaction.support.ResourceHolderSynchronization;

/**
 * The base {@link ResourceHolderSynchronization} for {@link IntegrationResourceHolder}.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public abstract class IntegrationResourceHolderSynchronization
		extends ResourceHolderSynchronization<IntegrationResourceHolder, Object> {

	protected final IntegrationResourceHolder resourceHolder;

	public IntegrationResourceHolderSynchronization(IntegrationResourceHolder resourceHolder,
			Object resourceKey) {
		super(resourceHolder, resourceKey);
		this.resourceHolder = resourceHolder;
	}

	public IntegrationResourceHolder getResourceHolder() {
		return resourceHolder;
	}

}
