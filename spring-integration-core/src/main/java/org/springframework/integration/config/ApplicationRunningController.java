/*
 * Copyright 2025 the original author or authors.
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
