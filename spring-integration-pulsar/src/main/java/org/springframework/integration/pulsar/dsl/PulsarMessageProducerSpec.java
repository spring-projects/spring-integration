/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.pulsar.dsl;

import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.pulsar.inbound.PulsarMessageProducer;
import org.springframework.pulsar.listener.PulsarMessageListenerContainer;
import org.springframework.pulsar.support.header.PulsarHeaderMapper;

/**
 * A {@link MessageProducerSpec} implementation for the {@link PulsarMessageProducer}.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 */
public class PulsarMessageProducerSpec extends MessageProducerSpec<PulsarMessageProducerSpec, PulsarMessageProducer> {

	PulsarMessageProducerSpec(PulsarMessageListenerContainer container) {
		super(new PulsarMessageProducer(container));
	}

	/**
	 * Configure the mapper of the Pulsar message properties and metadata to the message
	 * headers.
	 * @param headerMapper the mapper.
	 * @return the spec.
	 */
	public PulsarMessageProducerSpec headerMapper(PulsarHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return this;
	}

}
