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

package org.springframework.integration.channel;

import java.util.concurrent.Executor;
import java.util.function.Predicate;

import org.springframework.integration.dispatcher.LoadBalancingStrategy;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * An implementation of {@link org.springframework.messaging.MessageChannel}
 * that delegates to an instance of
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
 *
 * @since 1.0.3
 */
public class ExecutorChannel extends AbstractExecutorChannel {

	private final LoadBalancingStrategy loadBalancingStrategy;

	private Predicate<Exception> failoverStrategy = (exception) -> true;

	/**
	 * Create an ExecutorChannel that delegates to the provided
	 * {@link Executor} when dispatching Messages.
	 * <p>
	 * The Executor must not be null.
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
	 * @param executor The executor.
	 * @param loadBalancingStrategy The load balancing strategy implementation.
	 */
	public ExecutorChannel(Executor executor, @Nullable LoadBalancingStrategy loadBalancingStrategy) {
		super(executor);
		Assert.notNull(executor, "executor must not be null");
		UnicastingDispatcher unicastingDispatcher = new UnicastingDispatcher(executor);
		this.loadBalancingStrategy = loadBalancingStrategy;
		if (this.loadBalancingStrategy != null) {
			unicastingDispatcher.setLoadBalancingStrategy(this.loadBalancingStrategy);
		}
		this.dispatcher = unicastingDispatcher;
	}

	/**
	 * Specify whether the channel's dispatcher should have failover enabled.
	 * By default, it will. Set this value to 'false' to disable it.
	 * @param failover The failover boolean.
	 */
	public void setFailover(boolean failover) {
		setFailoverStrategy((exception) -> failover);
	}

	/**
	 * Configure a strategy whether the channel's dispatcher should have failover enabled
	 * for the exception thrown.
	 * Overrides {@link #setFailover(boolean)} option.
	 * In other words: or this, or that option has to be set.
	 * @param failoverStrategy The failover boolean.
	 * @since 6.3
	 */
	public void setFailoverStrategy(Predicate<Exception> failoverStrategy) {
		this.failoverStrategy = failoverStrategy;
		getDispatcher().setFailoverStrategy(failoverStrategy);
	}

	@Override
	protected UnicastingDispatcher getDispatcher() {
		return (UnicastingDispatcher) this.dispatcher;
	}

	@Override
	public final void onInit() {
		Assert.state(getDispatcher().getHandlerCount() == 0, "You cannot subscribe() until the channel "
				+ "bean is fully initialized by the framework. Do not subscribe in a @Bean definition");
		super.onInit();
		if (!(this.executor instanceof ErrorHandlingTaskExecutor)) {
			ErrorHandler errorHandler = ChannelUtils.getErrorHandler(getBeanFactory());
			this.executor = new ErrorHandlingTaskExecutor(this.executor, errorHandler);
		}
		UnicastingDispatcher unicastingDispatcher = new UnicastingDispatcher(this.executor);
		unicastingDispatcher.setFailoverStrategy(this.failoverStrategy);
		if (this.maxSubscribers == null) {
			this.maxSubscribers = getIntegrationProperties().getChannelsMaxUnicastSubscribers();
		}
		unicastingDispatcher.setMaxSubscribers(this.maxSubscribers);
		if (this.loadBalancingStrategy != null) {
			unicastingDispatcher.setLoadBalancingStrategy(this.loadBalancingStrategy);
		}

		unicastingDispatcher.setMessageHandlingTaskDecorator(task -> {
			if (ExecutorChannel.this.executorInterceptorsSize > 0) {
				return new MessageHandlingTask(task);
			}
			else {
				return task;
			}
		});

		this.dispatcher = unicastingDispatcher;
	}

}
