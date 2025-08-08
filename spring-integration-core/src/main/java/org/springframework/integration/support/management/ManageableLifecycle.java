/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.support.management;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.context.Lifecycle;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * Makes {@link Lifecycle} methods manageable.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.4
 *
 */
public interface ManageableLifecycle extends Lifecycle {

	@ManagedOperation(description = "Start the component")
	@Reflective
	@Override
	void start();

	@ManagedOperation(description = "Stop the component")
	@Reflective
	@Override
	void stop();

	@ManagedAttribute(description = "Is the component running?")
	@Reflective
	@Override
	boolean isRunning();

}
