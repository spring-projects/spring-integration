/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import java.util.concurrent.Executor;

import org.springframework.integration.channel.ExecutorChannel;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ExecutorChannelSpec extends LoadBalancingChannelSpec<ExecutorChannelSpec, ExecutorChannel> {

	private final Executor executor;

	protected ExecutorChannelSpec(Executor executor) {
		this.executor = executor;
	}

	@Override
	protected ExecutorChannel doGet() {
		this.channel = new ExecutorChannel(this.executor, this.loadBalancingStrategy);
		if (this.failoverStrategy != null) {
			this.channel.setFailoverStrategy(this.failoverStrategy);
		}
		if (this.maxSubscribers != null) {
			this.channel.setMaxSubscribers(this.maxSubscribers);
		}
		return super.doGet();
	}

}
