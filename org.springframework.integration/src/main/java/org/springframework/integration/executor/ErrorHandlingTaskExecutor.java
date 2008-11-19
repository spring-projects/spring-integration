/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.executor;

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.util.Assert;

/**
 * A {@link TaskExecutor} implementation that wraps an existing TaskExecutor
 * instance in order to catch any exceptions. If an exception is thrown, it
 * will be handled by the provided {@link ErrorHandler}.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class ErrorHandlingTaskExecutor implements TaskExecutor {

	private final TaskExecutor taskExecutor;

	private final ErrorHandler errorHandler;


	public ErrorHandlingTaskExecutor(TaskExecutor taskExecutor, ErrorHandler errorHandler) {
		Assert.notNull(taskExecutor, "taskExecutor must not be null");
		Assert.notNull(errorHandler, "errorHandler must not be null");
		this.taskExecutor = taskExecutor;
		this.errorHandler = errorHandler;
	}


	public void execute(final Runnable task) {
		this.taskExecutor.execute(new Runnable() {
			public void run() {
				try {
					task.run();
				}
				catch (Throwable t) {
					errorHandler.handle(t);
				}
			}
		});
	}

}
