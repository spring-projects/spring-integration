/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.function.Predicate;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.dispatcher.LoadBalancingStrategy;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;

/**
 *
 * @param <S> the target {@link LoadBalancingChannelSpec} implementation type.
 * @param <C> the target {@link AbstractMessageChannel} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class LoadBalancingChannelSpec<S extends MessageChannelSpec<S, C>, C extends AbstractMessageChannel>
		extends MessageChannelSpec<S, C> {

	protected LoadBalancingStrategy loadBalancingStrategy = new RoundRobinLoadBalancingStrategy(); // NOSONAR

	protected Predicate<Exception> failoverStrategy; // NOSONAR

	protected Integer maxSubscribers; // NOSONAR

	protected LoadBalancingChannelSpec() {
	}

	public S loadBalancer(LoadBalancingStrategy loadBalancingStrategyToSet) {
		this.loadBalancingStrategy = loadBalancingStrategyToSet;
		return _this();
	}

	public S failover(boolean failoverToSet) {
		return failoverStrategy((exception) -> failoverToSet);
	}

	/**
	 * Configure a strategy whether the channel's dispatcher should have failover enabled
	 * for the exception thrown.
	 * Overrides {@link #failover(boolean)} option.
	 * In other words: or this, or that option has to be set.
	 * @param failoverStrategy The failover boolean.
	 * @since 6.3
	 */
	public S failoverStrategy(Predicate<Exception> failoverStrategy) {
		this.failoverStrategy = failoverStrategy;
		return _this();
	}

	public S maxSubscribers(Integer maxSubscribersToSet) {
		this.maxSubscribers = maxSubscribersToSet;
		return _this();
	}

}
