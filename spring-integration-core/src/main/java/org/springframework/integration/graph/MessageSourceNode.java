/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.core.MessageSource;

/**
 * Represents a message source.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class MessageSourceNode extends ErrorCapableEndpointNode implements ReceiveCountersAware {

	private @Nullable Supplier<ReceiveCounters> receiveCounters;

	public MessageSourceNode(int nodeId, String name, MessageSource<?> messageSource, String output,
			@Nullable String errors) {

		super(nodeId, name, messageSource, output, errors);
	}

	public @Nullable ReceiveCounters getReceiveCounters() {
		return this.receiveCounters != null ? this.receiveCounters.get() : null;
	}

	@Override
	public void receiveCounters(Supplier<ReceiveCounters> counters) {
		this.receiveCounters = counters;
	}

}
