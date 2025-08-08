/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.context;

/**
 * Interface for components that wish to be considered for
 * an orderly shutdown using management interfaces. beforeShutdown()
 * will be called before schedulers, executors etc, are stopped.
 * afterShutdown() is called after the shutdown delay.
 *
 * @author Gary Russell
 * @since 2.2
 *
 */
public interface OrderlyShutdownCapable {

	/**
	 * Called before shutdown begins. Implementations should
	 * stop accepting new messages. Can optionally return the
	 * number of active messages in process.
	 * @return The number of active messages if available.
	 */
	int beforeShutdown();

	/**
	 * Called after normal shutdown of schedulers, executors etc,
	 * and after the shutdown delay has elapsed, but before any
	 * forced shutdown of any remaining active scheduler/executor
	 * threads.Can optionally return the number of active messages
	 * still in process.
	 * @return The number of active messages if available.
	 */
	int afterShutdown();

}
