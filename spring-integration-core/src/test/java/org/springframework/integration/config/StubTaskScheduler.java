/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class StubTaskScheduler implements TaskScheduler {

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		return null;
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
		return null;
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
		return null;
	}

}
