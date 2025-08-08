/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel;

import java.util.function.Predicate;

import org.springframework.integration.dispatcher.LoadBalancingStrategy;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.lang.Nullable;

/**
 * A channel that invokes a single subscriber for each sent Message.
 * The invocation will occur in the sender's thread.
 *
 * @author Dave Syer
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class DirectChannel extends AbstractSubscribableChannel {

	private final UnicastingDispatcher dispatcher = new UnicastingDispatcher();

	private volatile Integer maxSubscribers;

	/**
	 * Create a channel with default {@link RoundRobinLoadBalancingStrategy}.
	 */
	public DirectChannel() {
		this(new RoundRobinLoadBalancingStrategy());
	}

	/**
	 * Create a DirectChannel with a {@link LoadBalancingStrategy}.
	 * Can be {@code null} meaning that no balancing is applied;
	 * every message is always going to be handled by the first subscriber.
	 * @param loadBalancingStrategy The load balancing strategy implementation.
	 * @see #setFailover(boolean)
	 */
	public DirectChannel(@Nullable LoadBalancingStrategy loadBalancingStrategy) {
		this.dispatcher.setLoadBalancingStrategy(loadBalancingStrategy);
	}

	/**
	 * Specify whether the channel's dispatcher should have failover enabled.
	 * By default, it will. Set this value to 'false' to disable it.
	 * Overrides {@link #setFailoverStrategy(Predicate)} option.
	 * In other words: or this, or that option has to be set.
	 * @param failover The failover boolean.
	 */
	public void setFailover(boolean failover) {
		this.dispatcher.setFailover(failover);
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
		this.dispatcher.setFailoverStrategy(failoverStrategy);
	}

	/**
	 * Specify the maximum number of subscribers supported by the
	 * channel's dispatcher.
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
	protected void onInit() {
		super.onInit();
		if (this.maxSubscribers == null) {
			setMaxSubscribers(getIntegrationProperties().getChannelsMaxUnicastSubscribers());
		}
	}

}
