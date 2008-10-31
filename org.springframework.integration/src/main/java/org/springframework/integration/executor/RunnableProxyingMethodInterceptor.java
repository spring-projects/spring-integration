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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.util.ErrorHandler;

/**
 * Used by {@link ErrorHandlingExecutorBeanPostProcessor} to Proxy
 * {@link TaskExecutor} instances. This allow the wrapping of {@link Runnable}
 * instances in order to catch any errors thrown and delegate handling to an
 * instance of {@link ErrorHandler}.
 * 
 * @author Jonas Partner
 */
public class RunnableProxyingMethodInterceptor implements MethodInterceptor {

	private final ErrorHandler errorHandler;


	public RunnableProxyingMethodInterceptor(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}


	public Object invoke(MethodInvocation invocation) throws Throwable {
		Runnable runnable = (Runnable) invocation.getArguments()[0];
		invocation.getArguments()[0] = new ErrorHandlingRunnableWrapper(runnable, errorHandler);
		return invocation.proceed();
	}


	private static class ErrorHandlingRunnableWrapper implements Runnable {

		private final ErrorHandler errorHandler;

		private final Runnable runnableTarget;

		public ErrorHandlingRunnableWrapper(Runnable runnableTarget, ErrorHandler errorHandler) {
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
