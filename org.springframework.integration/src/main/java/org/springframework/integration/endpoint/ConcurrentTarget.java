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

package org.springframework.integration.endpoint;

import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.handler.MessageHandlerNotRunningException;
import org.springframework.integration.handler.MessageHandlerRejectedExecutionException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.util.Assert;

/**
 * A {@link MessageTarget} implementation that encapsulates an Executor and delegates
 * to a wrapped target for concurrent, asynchronous message handling.
 * 
 * @author Mark Fisher
 */
public class ConcurrentTarget implements MessageTarget, DisposableBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final MessageTarget target;

	private final ExecutorService executor;

	private volatile ErrorHandler errorHandler;


	public ConcurrentTarget(MessageTarget target, ExecutorService executor) {
		Assert.notNull(target, "'target' must not be null");
		Assert.notNull(executor, "'executor' must not be null");
		this.target = target;
		this.executor = executor;
	}


	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void destroy() {
		this.executor.shutdown();
	}

	public boolean send(Message<?> message) {
		if (this.executor.isShutdown()) {
			throw new MessageHandlerNotRunningException(message);
		}
		try {
			this.executor.execute(new TargetTask(message));
			return true;
		}
		catch (RuntimeException e) {
			throw new MessageHandlerRejectedExecutionException(message, e);
		}
	}


	private class TargetTask implements Runnable {

		private Message<?> message;

		TargetTask(Message<?> message) {
			this.message = message;
		}

		public void run() {
			try {
				if (!target.send(this.message)) {
					throw new MessageDeliveryException(message, "failed to send message to target");
				}
			}
			catch (Throwable t) {
				if (logger.isDebugEnabled()) {
					logger.debug("error occurred in handler execution", t);
				}
				if (errorHandler != null) {
					errorHandler.handle(t);
				}
				else if (logger.isWarnEnabled() && !logger.isDebugEnabled()) {
					logger.warn("error occurred in handler execution", t);
				}
			}
		}
	}

}
