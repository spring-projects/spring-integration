/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
