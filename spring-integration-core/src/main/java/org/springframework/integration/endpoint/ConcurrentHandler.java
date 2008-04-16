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
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.MessageHandlerNotRunningException;
import org.springframework.integration.handler.MessageHandlerRejectedExecutionException;
import org.springframework.integration.handler.ReplyHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} implementation that encapsulates a
 * {@link ThreadPoolTaskExecutor} and delegates to a wrapped handler for
 * concurrent, asynchronous message handling.
 * 
 * @author Mark Fisher
 */
public class ConcurrentHandler implements MessageHandler, DisposableBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final MessageHandler handler;

	private final ExecutorService executor;

	private volatile ErrorHandler errorHandler;

	private volatile ReplyHandler replyHandler;


	public ConcurrentHandler(MessageHandler handler, ExecutorService executor) {
		Assert.notNull(handler, "'handler' must not be null");
		Assert.notNull(executor, "'executor' must not be null");
		this.handler = handler;
		this.executor = executor;
	}


	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void setReplyHandler(ReplyHandler replyHandler) {
		this.replyHandler = replyHandler;
	}

	public void destroy() {
		this.executor.shutdownNow();
	}

	public Message<?> handle(Message<?> message) {
		if (this.executor.isShutdown()) {
			throw new MessageHandlerNotRunningException(message);
		}
		try {
			this.executor.execute(new HandlerTask(message));
			return null;
		}
		catch (RuntimeException e) {
			throw new MessageHandlerRejectedExecutionException(message, e);
		}
	}


	private class HandlerTask implements Runnable {

		private Message<?> message;

		HandlerTask(Message<?> message) {
			this.message = message;
		}

		public void run() {
			try {
				Message<?> reply = handler.handle(this.message);
				if (replyHandler != null) {
					replyHandler.handle(reply, this.message.getHeader());
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
