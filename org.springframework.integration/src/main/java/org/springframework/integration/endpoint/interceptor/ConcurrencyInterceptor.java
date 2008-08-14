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

package org.springframework.integration.endpoint.interceptor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.EndpointInterceptor;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.MessageHandlerRejectedExecutionException;
import org.springframework.integration.message.AsyncMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

/**
 * An {@link EndpointInterceptor} implementation that delegates to a
 * {@link TaskExecutor} for concurrent, asynchronous message handling.
 * 
 * @author Mark Fisher
 */
public class ConcurrencyInterceptor extends EndpointInterceptorAdapter
		implements DisposableBean, InitializingBean, ChannelRegistryAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final TaskExecutor executor;

	private volatile ErrorHandler errorHandler;

	private volatile ChannelRegistry channelRegistry;


	public ConcurrencyInterceptor(TaskExecutor executor) {
		Assert.notNull(executor, "TaskExecutor must not be null");
		this.executor = executor;
	}

	public ConcurrencyInterceptor(ConcurrencyPolicy concurrencyPolicy, String threadPrefix) {
		Assert.notNull(concurrencyPolicy, "ConcurrencyPolicy must not be null");
		int core = concurrencyPolicy.getCoreSize();
		int max = concurrencyPolicy.getMaxSize();
		int keepAlive = concurrencyPolicy.getKeepAliveSeconds();
		int capacity = concurrencyPolicy.getQueueCapacity();
		BlockingQueue<Runnable> queue = (capacity > 0) ?
				new LinkedBlockingQueue<Runnable>(capacity) : new SynchronousQueue<Runnable>();
		ThreadPoolExecutor tpe = new ThreadPoolExecutor(core, max, keepAlive, TimeUnit.SECONDS, queue);
		tpe.setThreadFactory(new CustomizableThreadFactory(threadPrefix));
		tpe.setRejectedExecutionHandler(new CallerRunsPolicy());
		this.executor = new ConcurrentTaskExecutor(tpe);
	}


	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	public void afterPropertiesSet() throws Exception {
		if (this.errorHandler == null && this.channelRegistry != null) {
			MessageChannel errorChannel = this.channelRegistry.lookupChannel(
					ChannelRegistry.ERROR_CHANNEL_NAME);
			if (errorChannel != null) {
				this.errorHandler = new MessagePublishingErrorHandler(errorChannel);
			}
		}
	}

	public void destroy() throws Exception {
		if (this.executor instanceof DisposableBean) {
			((DisposableBean) this.executor).destroy();
		}
		if (this.executor instanceof ConcurrentTaskExecutor) {
			Executor innerExecutor = ((ConcurrentTaskExecutor) this.executor).getConcurrentExecutor();
			if (innerExecutor instanceof ExecutorService) {
				ExecutorService executorService = (ExecutorService) innerExecutor;
				executorService.shutdown();
				if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
				}
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Message<?> aroundHandle(final Message<?> requestMessage, final MessageHandler handler) {
		try {
			FutureTask<Message<?>> task = new FutureTask<Message<?>>(new Callable<Message<?>>() {
				public Message<?> call() throws Exception {
					try {
						return handler.handle(requestMessage);
					}
					catch (Exception e) {
						if (logger.isDebugEnabled()) {
							logger.debug("error occurred in handler execution", e);
						}
						if (errorHandler != null) {
							errorHandler.handle(e);
						}
						else {
							if (logger.isWarnEnabled() && !logger.isDebugEnabled()) {
								logger.warn("error occurred in handler execution", e);
							}
							throw e;
						}
					}
					return null;
				}
			});	
			this.executor.execute(task);
			return new AsyncMessage(task);
		}
		catch (RuntimeException e) {
			throw new MessageHandlerRejectedExecutionException(requestMessage, e);
		}
	}

}
