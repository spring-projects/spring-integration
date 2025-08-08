/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHandler;

/**
 * Represents a message handler.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class MessageHandlerNode extends EndpointNode implements SendTimersAware {

	private final String input;

	private Supplier<SendTimers> sendTimers;

	public MessageHandlerNode(int nodeId, String name, MessageHandler handler, String input, String output) {
		super(nodeId, name, handler, output);
		this.input = input;
	}

	public String getInput() {
		return this.input;
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

