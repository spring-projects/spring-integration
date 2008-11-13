/*
 * Copyright 2002-2007 the original author or authors.
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

/**
 * 
 * @author Jonas Partner
 *
 */
public class ErrorHandlingTaskExecutor implements TaskExecutor {


	private final ErrorHandler errorHandler;
	
	private final TaskExecutor taskExecutor;
	
	/**
	 * @param errorChannel
	 */
	public ErrorHandlingTaskExecutor(ErrorHandler errorHandler, TaskExecutor taskExecutor) {
		this.errorHandler = errorHandler;
		this.taskExecutor = taskExecutor;
	}
	
	public void execute(Runnable task) {
		taskExecutor.execute(new ErrorHandlingRunnableWrapper(task,errorHandler));
	}
	
	private static class ErrorHandlingRunnableWrapper implements Runnable {

		private final ErrorHandler errorHandler;

		private final Runnable runnableTarget;

		public ErrorHandlingRunnableWrapper(Runnable runnableTarget,ErrorHandler errorHandler) {
			this.runnableTarget = runnableTarget;
			this.errorHandler = errorHandler;
		}

		public void run() {
			try {
				runnableTarget.run();
			}
			catch (Throwable t) {
				errorHandler.handle(t);
			}
		}

	}
	
	
	
}
