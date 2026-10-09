/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.pulsar.dsl;

import org.springframework.pulsar.core.PulsarOperations;
import org.springframework.pulsar.listener.PulsarMessageListenerContainer;

/**
 * Factory class for Apache Pulsar components.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 */
public final class Pulsar {

	/**
	 * Create a {@link PulsarMessageHandlerSpec} to send messages to Apache Pulsar.
	 * @param pulsarOperations the operations to send the messages with, such as a
	 * {@link org.springframework.pulsar.core.PulsarTemplate}.
	 * @param <T> the type of the values that are sent.
	 * @return the spec.
	 */
	public static <T> PulsarMessageHandlerSpec<T> outboundAdapter(PulsarOperations<T> pulsarOperations) {
		return new PulsarMessageHandlerSpec<>(pulsarOperations);
	}

	/**
	 * Create a {@link PulsarMessageProducerSpec} for a message-driven channel adapter
	 * that sends the messages received from Apache Pulsar to the channel.
	 * @param container the container that receives the messages.
	 * @return the spec.
	 */
	public static PulsarMessageProducerSpec messageDrivenChannelAdapter(PulsarMessageListenerContainer container) {
		return new PulsarMessageProducerSpec(container);
	}

	private Pulsar() {
	}

}
