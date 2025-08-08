/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.graph;

/**
 * Success and failure timer stats.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class SendTimers {

	private final TimerStats successes;

	private final TimerStats failures;

	public SendTimers(TimerStats successes, TimerStats failures) {
		this.successes = successes;
		this.failures = failures;
	}

	public TimerStats getSuccesses() {
		return this.successes;
	}

	public TimerStats getFailures() {
		return this.failures;
	}

}
