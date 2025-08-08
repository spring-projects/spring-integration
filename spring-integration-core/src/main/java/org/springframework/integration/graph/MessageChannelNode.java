/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;

/**
 * Represents a message channel.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class MessageChannelNode extends IntegrationNode implements SendTimersAware {

	private Supplier<SendTimers> sendTimers;

	public MessageChannelNode(int nodeId, String name, MessageChannel channel) {
		super(nodeId, name, channel);
	}

	@Nullable
	public SendTimers getSendTimers() {
		return this.sendTimers != null ? this.sendTimers.get() : null;
	}

	@Override
	public void sendTimers(Supplier<SendTimers> timers) {
		this.sendTimers = timers;
	}

}
