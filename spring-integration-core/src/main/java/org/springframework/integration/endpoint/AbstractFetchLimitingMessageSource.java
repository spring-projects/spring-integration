/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import org.springframework.integration.support.management.MessageSourceManagement;

/**
 * A message source that can limit the number of remote objects it fetches.
 *
 * @param <T> the expected payload type.
 *
 * @author Gary Russell
 *
 * @since 5.0
 */
public abstract class AbstractFetchLimitingMessageSource<T> extends AbstractMessageSource<T>
		implements MessageSourceManagement {

	private volatile int maxFetchSize = Integer.MIN_VALUE;

	@Override
	public void setMaxFetchSize(int maxFetchSize) {
		this.maxFetchSize = maxFetchSize;
	}

	@Override
	public int getMaxFetchSize() {
		return this.maxFetchSize;
	}

	@Override
	protected Object doReceive() {
		return doReceive(this.maxFetchSize);
	}

	/**
	 * Subclasses must implement this method. Typically the returned value will be the
	 * payload of type T, but the returned value may also be a Message instance whose
	 * payload is of type T.
	 * @param maxFetchSizeToReceive the maximum number of messages to fetch if a fetch is
	 * necessary.
	 * @return The value returned.
	 */
	protected abstract Object doReceive(int maxFetchSizeToReceive);

}
