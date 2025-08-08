/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.core;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * Endpoints implementing this interface can be paused/resumed. A paused endpoint might
 * still maintain a connection to an external system, but will not send or receive
 * messages.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0.3
 *
 */
public interface Pausable extends ManageableLifecycle {

	/**
	 * Pause the endpoint.
	 */
	@ManagedOperation(description = "Pause the component")
	@Reflective
	void pause();

	/**
	 * Resume the endpoint if paused.
	 */
	@ManagedOperation(description = "Resume the component")
	@Reflective
	void resume();

	/**
	 * Check if the endpoint is paused.
	 * @return true if paused.
	 * @since 5.4
	 */
	@ManagedAttribute(description = "Is the component paused?")
	@Reflective
	default boolean isPaused() {
		throw new UnsupportedOperationException("This component does not implement this method");
	}

}
