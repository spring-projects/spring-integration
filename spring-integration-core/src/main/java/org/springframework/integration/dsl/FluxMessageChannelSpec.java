/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.dsl;

import org.springframework.integration.channel.FluxMessageChannel;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class FluxMessageChannelSpec extends MessageChannelSpec<FluxMessageChannelSpec, FluxMessageChannel> {

	protected FluxMessageChannelSpec() {
		this.channel = new FluxMessageChannel();
	}

}
