/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.concurrent.Executor;

import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.dispatcher.LoadBalancingStrategy;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * An implementation of {@link MessageChannel} that delegates to an instance of
 * {@link UnicastingDispatcher} which in turn delegates all dispatching
 * invocations to an {@link Executor}.
 * <p>
 * <em><b>NOTE</b>: unlike DirectChannel, the ExecutorChannel does not support a
 * shared transactional context between sender and handler, because the
 * {@link Executor} typically does not block the sender's Thread since it
 * uses another Thread for the dispatch.</em> (SyncTaskExecutor is an
 * exception but would provide no value for this channel. If synchronous
 * dispatching is required, a DirectChannel should be used instead).
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @since 1.0.3
 */
public class ExecutorChannel extends AbstractSubscribableChannel {

	private volatile UnicastingDispatcher dispatcher;

	private volatile Executor executor;

	private volatile boolean failover = true;

	private volatile Integer maxSubscribers;

	private volatile LoadBalancingStrategy loadBalancingStrategy;


	/**
	 * Create an ExecutorChannel that delegates to the provided
	 * {@link Executor} when dispatching Messages.
	 * <p>
	 * The Executor must not be null.
	 *
	 * @param executor The executor.
	 */
	public ExecutorChannel(Executor executor) {
		this(executor, new RoundRobinLoadBalancingStrategy());
	}

	/**
	 * Create an ExecutorChannel with a {@link LoadBalancingStrategy} that
	 * delegates to the provided {@link Executor} when dispatching Messages.
	 * <p>
	 * The Executor must not be null.
	 *
	 * @param executor The executor.
	 * @param loadBalancingStrategy The load balancing strategy implementation.
	 */
	public ExecutorChannel(Executor executor, LoadBalancingStrategy loadBalancingStrategy) {
		Assert.notNull(executor, "executor must not be null");
		this.executor = executor;
		this.dispatcher = new UnicastingDispatcher(executor);
		if (loadBalancingStrategy != null) {
			this.loadBalancingStrategy = loadBalancingStrategy;
			this.dispatcher.setLoadBalancingStrategy(loadBalancingStrategy);
		}
	}


	/**
	 * Specify whether the channel's dispatcher should have failover enabled.
	 * By default, it will. Set this value to 'false' to disable it.
	 *
	 * @param failover The failover boolean.
	 */
	public void setFailover(boolean failover) {
		this.failover = failover;
		this.dispatcher.setFailover(failover);
	}

	/**
	 * Specify the maximum number of subscribers supported by the
	 * channel's dispatcher.
	 *
	 * @param maxSubscribers The maximum number of subscribers allowed.
	 */
	public void setMaxSubscribers(int maxSubscribers) {
		this.maxSubscribers = maxSubscribers;
		this.dispatcher.setMaxSubscribers(maxSubscribers);
	}

	@Override
	protected UnicastingDispatcher getDispatcher() {
		return this.dispatcher;
	}

	@Override
	public final void onInit() {
		if (!(this.executor instanceof ErrorHandlingTaskExecutor)) {
			ErrorHandler errorHandler = new MessagePublishingErrorHandler(
					new BeanFactoryChannelResolver(this.getBeanFactory()));
			this.executor = new ErrorHandlingTaskExecutor(this.executor, errorHandler);
		}
		this.dispatcher = new UnicastingDispatcher(this.executor);
		this.dispatcher.setFailover(this.failover);
		if (this.maxSubscribers == null) {
			this.maxSubscribers = this.getIntegrationProperty(IntegrationProperties.CHANNELS_MAX_UNICAST_SUBSCRIBERS, Integer.class);
		}
		this.dispatcher.setMaxSubscribers(this.maxSubscribers);
		if (this.loadBalancingStrategy != null) {
			this.dispatcher.setLoadBalancingStrategy(this.loadBalancingStrategy);
		}
	}

}
