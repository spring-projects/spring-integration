/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.channel;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.dispatcher.LoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.scheduling.support.ErrorHandler;
import org.springframework.util.Assert;

/**
 * An implementation of {@link MessageChannel} that delegates to an instance of
 * {@link UnicastingDispatcher} which in turn delegates all dispatching
 * invocations to a {@link TaskExecutor}.
 * <p>
 * <emphasis>NOTE: unlike DirectChannel, the ExecutorChannel does not support a
 * shared transactional context between sender and handler, because the
 * {@link TaskExecutor} typically does not block the sender's Thread since it
 * uses another Thread for the dispatch.</emphasis> (SyncTaskExecutor is an
 * exception but would provide no value for this channel. If synchronous
 * dispatching is required, a DirectChannel should be used instead). 
 * 
 * @author Mark Fisher
 * @since 1.0.3
 */
public class ExecutorChannel extends AbstractSubscribableChannel implements BeanFactoryAware {

	private volatile UnicastingDispatcher dispatcher;

	private volatile TaskExecutor taskExecutor;

	private volatile boolean failover = true;

	private volatile LoadBalancingStrategy loadBalancingStrategy;


	/**
	 * Create an ExecutorChannel that delegates to the provided
	 * {@link TaskExecutor} when dispatching Messages.
	 * <p>
	 * The TaskExecutor must not be null.
	 */
	public ExecutorChannel(TaskExecutor taskExecutor) {
		this(taskExecutor, null);
	}

	/**
	 * Create an ExecutorChannel with a {@link LoadBalancingStrategy} that
	 * delegates to the provided {@link TaskExecutor} when dispatching Messages.
	 * <p>
	 * The TaskExecutor must not be null.
	 */
	public ExecutorChannel(TaskExecutor taskExecutor, LoadBalancingStrategy loadBalancingStrategy) {
		Assert.notNull(taskExecutor, "taskExecutor must not be null");
		this.taskExecutor = taskExecutor;
		this.dispatcher = new UnicastingDispatcher(taskExecutor);
		if (loadBalancingStrategy != null) {
			this.loadBalancingStrategy = loadBalancingStrategy;
			this.dispatcher.setLoadBalancingStrategy(loadBalancingStrategy);
		}
	}


	/**
	 * Specify whether the channel's dispatcher should have failover enabled.
	 * By default, it will. Set this value to 'false' to disable it.
	 */
	public void setFailover(boolean failover) {
		this.failover = failover;
		this.dispatcher.setFailover(failover);
	}

	@Override
	protected UnicastingDispatcher getDispatcher() {
		return this.dispatcher;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(this.taskExecutor instanceof ErrorHandlingTaskExecutor)) {
			ErrorHandler errorHandler = new MessagePublishingErrorHandler(new BeanFactoryChannelResolver(beanFactory));
			this.taskExecutor = new ErrorHandlingTaskExecutor(this.taskExecutor, errorHandler);
		}
		this.dispatcher = new UnicastingDispatcher(this.taskExecutor);
		this.dispatcher.setFailover(this.failover);
		if (this.loadBalancingStrategy != null) {
			this.dispatcher.setLoadBalancingStrategy(this.loadBalancingStrategy);
		}
	}

}
