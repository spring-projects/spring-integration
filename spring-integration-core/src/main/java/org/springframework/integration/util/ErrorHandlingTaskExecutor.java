/*
 * Copyright 2002-2024 the original author or authors.
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
