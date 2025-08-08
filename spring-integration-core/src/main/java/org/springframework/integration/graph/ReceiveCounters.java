/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.graph;

/**
 * Counters for components that maintain receive counters.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class ReceiveCounters {

	private final long successes;

	private final long failures;

	public ReceiveCounters(long successes, long failures) {
		this.successes = successes;
		this.failures = failures;
	}

	public long getSuccesses() {
		return this.successes;
	}

	public long getFailures() {
		return this.failures;
	}

}
