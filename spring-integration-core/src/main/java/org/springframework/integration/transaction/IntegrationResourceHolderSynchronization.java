/*
 * Copyright 2014-2019 the original author or authors.
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

import org.springframework.transaction.support.ResourceHolderSynchronization;

/**
 * The base {@link ResourceHolderSynchronization} for {@link IntegrationResourceHolder}.
 *
 * @author Artem Bilan
 * @author Andreas Baer
 *
 * @since 4.0
 */
public class IntegrationResourceHolderSynchronization
		extends ResourceHolderSynchronization<IntegrationResourceHolder, Object> {

	protected final IntegrationResourceHolder resourceHolder; // NOSONAR final

	private boolean shouldUnbindAtCompletion = true;

	public IntegrationResourceHolderSynchronization(IntegrationResourceHolder resourceHolder,
			Object resourceKey) {
		super(resourceHolder, resourceKey);
		this.resourceHolder = resourceHolder;
	}

	public IntegrationResourceHolder getResourceHolder() {
		return this.resourceHolder;
	}

	/**
	 * Specify if the {@link #resourceHolder} should be unbound from the Thread Local store
	 * at transaction completion or not. Default {@code true}.
	 * @param shouldUnbindAtCompletion unbind or not {@link #resourceHolder}
	 * at transaction completion
	 * @since 5.0
	 */
	public void setShouldUnbindAtCompletion(boolean shouldUnbindAtCompletion) {
		this.shouldUnbindAtCompletion = shouldUnbindAtCompletion;
	}

	@Override
	protected boolean shouldUnbindAtCompletion() {
		return this.shouldUnbindAtCompletion;
	}

}
