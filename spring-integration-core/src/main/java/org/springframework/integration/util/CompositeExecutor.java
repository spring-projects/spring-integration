/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.util;

import java.util.concurrent.Executor;

import org.springframework.util.Assert;

/**
 * An {@link Executor} that encapsulates two underlying executors. Used in cases
 * where two distinct operation types are being used where sharing threads could
 * adversely affect the operation of the system. For example, NIO event processing
 * threads being used for other blocking operations (such as assembling messages).
 * If a {@code CallerRunsPolicy} rejected execution policy is used, the NIO event thread
 * might deadlock, and stop processing events.
 * <p>
 * In order to work in such an environment, the secondary executor must <b>not</b>
 * have a {@code CallerRunsPolicy} rejected execution policy.
 * <p>
 * It is generally recommended to use a {@code CallerBlocksPolicy} on both
 * executors.
 *
 * @author Gary Russell
 * @since 3.0.3
 *
 */
public class CompositeExecutor implements Executor {

	private final Executor primaryTaskExecutor;

	private final Executor secondaryTaskExecutor;

	public CompositeExecutor(Executor primaryTaskExecutor, Executor secondaryTaskExecutor) {
		Assert.notNull(primaryTaskExecutor, "'primaryTaskExecutor' cannot be null");
		Assert.notNull(primaryTaskExecutor, "'secondaryTaskExecutor' cannot be null");
		this.primaryTaskExecutor = primaryTaskExecutor;
		this.secondaryTaskExecutor = secondaryTaskExecutor;
	}

	/**
	 * Execute using the primary executor.
	 * @param task the task to run.
	 */
	@Override
	public void execute(Runnable task) {
		this.primaryTaskExecutor.execute(task);
	}

	/**
	 * Execute using the secondary executor.
	 * @param task the task to run.
	 */
	public void execute2(Runnable task) {
		this.secondaryTaskExecutor.execute(task);
	}

}
