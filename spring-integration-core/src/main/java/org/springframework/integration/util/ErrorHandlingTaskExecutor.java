/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.util;

import java.util.concurrent.Executor;

import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * A {@link TaskExecutor} implementation that wraps an existing Executor
 * instance in order to catch any exceptions. If an exception is thrown, it
 * will be handled by the provided {@link ErrorHandler}.
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Gary Russell
 */
public class ErrorHandlingTaskExecutor implements TaskExecutor {

	private final Executor executor;

	private final ErrorHandler errorHandler;

	public ErrorHandlingTaskExecutor(Executor executor, ErrorHandler errorHandler) {
		Assert.notNull(executor, "executor must not be null");
		Assert.notNull(errorHandler, "errorHandler must not be null");
		this.executor = executor;
		this.errorHandler = errorHandler;
	}

	public boolean isSyncExecutor() {
		return this.executor instanceof SyncTaskExecutor;
	}

	@Override
	public void execute(final Runnable task) {
		this.executor.execute(() -> {
			try {
				task.run();
			}
			catch (Throwable t) { //NOSONAR
				ErrorHandlingTaskExecutor.this.errorHandler.handleError(t);
			}
		});
	}

}
