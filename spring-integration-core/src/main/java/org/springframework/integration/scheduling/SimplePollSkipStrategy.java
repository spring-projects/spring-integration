/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.scheduling;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * A simple {@link PollSkipStrategy} to be used with a {@code PollSkipAdvice}.
 * Invoke {@link #skipPolls()} to start skipping polls; invoke {@link #reset()}
 * to resume polling.
 *
 * @author Gary Russell
 * @since 4.2.5
 *
 */
@ManagedResource
public class SimplePollSkipStrategy implements PollSkipStrategy {

	private volatile boolean skip;

	@Override
	public boolean skipPoll() {
		return this.skip;
	}

	/**
	 * Skip future polls.
	 */
	@ManagedOperation
	public void skipPolls() {
		this.skip = true;
	}

	/**
	 * Resume polling at the next {@code Trigger} event.
	 */
	@ManagedOperation
	public void reset() {
		this.skip = false;
	}

}
