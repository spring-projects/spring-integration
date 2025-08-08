/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.graph;

/**
 * Statistics captured from a timer meter.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class TimerStats {

	private final long count;

	private final double mean;

	private final double max;

	public TimerStats(long count, double mean, double max) {
		this.count = count;
		this.mean = mean;
		this.max = max;
	}

	public long getCount() {
		return this.count;
	}

	public double getMean() {
		return this.mean;
	}

	public double getMax() {
		return this.max;
	}

}
