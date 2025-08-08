/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import org.springframework.integration.channel.RendezvousChannel;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class RendezvousChannelSpec extends MessageChannelSpec<RendezvousChannelSpec, RendezvousChannel> {

	protected RendezvousChannelSpec() {
		this.channel = new RendezvousChannel();
	}

}
