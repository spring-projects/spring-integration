/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.graph;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.endpoint.MessageProducerSupport;

/**
 * Represents an inbound message producer.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class MessageProducerNode extends ErrorCapableEndpointNode implements SendTimersAware {

	private @Nullable Supplier<SendTimers> sendTimers;

	public MessageProducerNode(int nodeId, String name, MessageProducerSupport producer, String output,
			@Nullable String errors) {

		super(nodeId, name, producer, output, errors);
	}

	@Override
	public void sendTimers(Supplier<SendTimers> timers) {
		this.sendTimers = timers;
	}

	public @Nullable SendTimers getSendTimers() {
		return this.sendTimers != null ? this.sendTimers.get() : null;
	}

}
