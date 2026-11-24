/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
