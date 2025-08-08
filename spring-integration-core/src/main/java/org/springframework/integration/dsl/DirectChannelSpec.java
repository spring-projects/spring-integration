/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import org.springframework.integration.channel.DirectChannel;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class DirectChannelSpec extends LoadBalancingChannelSpec<DirectChannelSpec, DirectChannel> {

	@Override
	protected DirectChannel doGet() {
		this.channel = new DirectChannel(this.loadBalancingStrategy);
		if (this.failoverStrategy != null) {
			this.channel.setFailoverStrategy(this.failoverStrategy);
		}
		if (this.maxSubscribers != null) {
			this.channel.setMaxSubscribers(this.maxSubscribers);
		}
		return super.doGet();
	}

}
