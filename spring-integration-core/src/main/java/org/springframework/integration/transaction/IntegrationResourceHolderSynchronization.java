/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
