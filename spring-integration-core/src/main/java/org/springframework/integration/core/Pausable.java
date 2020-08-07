/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.integration.core;

import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * Endpoints implementing this interface can be paused/resumed. A paused endpoint might
 * still maintain a connection to an external system, but will not send or receive
 * messages.
 *
 * @author Gary Russell
 * @since 5.0.3
 *
 */
public interface Pausable extends ManageableLifecycle {

	/**
	 * Pause the endpoint.
	 */
	@ManagedOperation(description = "Pause the component")
	void pause();

	/**
	 * Resume the endpoint if paused.
	 */
	@ManagedOperation(description = "Resume the component")
	void resume();

	/**
	 * Check if the endpoint is paused.
	 * @return true if paused.
	 * @since 5.4
	 */
	@ManagedAttribute(description = "Is the component paused?")
	default boolean isPaused() {
		throw new UnsupportedOperationException("This component does not implement this method");
	}

}
