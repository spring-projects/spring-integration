/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.config;

import org.springframework.context.SmartLifecycle;

/**
 * An infrastructure bean to hold the status of the application context when
 * it is ready for interaction: refreshed or started.
 * <p>
 * Well-known {@link org.springframework.context.ConfigurableApplicationContext#isRunning()}
 * (or {@link org.springframework.context.event.ContextRefreshedEvent})
 * is good for target applications, when all the beans are already started,
 * but most of Spring Integration channel adapters initiate their logic
 * from the {@link SmartLifecycle#start()} implementation, so it would be false report
 * that application is not running during start.
 * <p>
 * This implementation uses {@value Integer#MIN_VALUE} for its phase to be started as early as possible.
 *
 * @author Artem Bilan
 *
 * @since 6.5
 */
class ApplicationRunningController implements SmartLifecycle {

	private volatile boolean running;

	@Override
	public void start() {
		this.running = true;
	}

	@Override
	public void stop() {
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return Integer.MIN_VALUE;
	}

}
