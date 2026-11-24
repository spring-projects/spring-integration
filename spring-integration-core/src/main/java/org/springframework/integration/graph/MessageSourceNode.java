/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
