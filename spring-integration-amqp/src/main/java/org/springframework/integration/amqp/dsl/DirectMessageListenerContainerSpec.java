/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;

/**
 * Spec for a {@link DirectMessageListenerContainer}.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class DirectMessageListenerContainerSpec
		extends AbstractMessageListenerContainerSpec<DirectMessageListenerContainerSpec, DirectMessageListenerContainer> {

	protected final DirectMessageListenerContainer listenerContainer; // NOSONAR

	public DirectMessageListenerContainerSpec(DirectMessageListenerContainer listenerContainer) {
		super(listenerContainer);
		this.listenerContainer = listenerContainer;
	}

	/**
	 * @param consumersPerQueue the consumersPerQueue.
	 * @return the spec.
	 * @see DirectMessageListenerContainer#setConsumersPerQueue(int)
	 */
	public DirectMessageListenerContainerSpec consumersPerQueue(int consumersPerQueue) {
		this.listenerContainer.setConsumersPerQueue(consumersPerQueue);
		return this;
	}

	/**
	 * @param messagesPerAck the messages per ack.
	 * @return the spec.
	 * @see DirectMessageListenerContainer#setMessagesPerAck(int)
	 */
	public DirectMessageListenerContainerSpec messagesPerAck(int messagesPerAck) {
		this.listenerContainer.setMessagesPerAck(messagesPerAck);
		return this;
	}

	/**
	 * @param ackTimeout the ack timeout.
	 * @return the spec.
	 * @see DirectMessageListenerContainer#setAckTimeout(long)
	 */
	public DirectMessageListenerContainerSpec ackTimeout(long ackTimeout) {
		this.listenerContainer.setAckTimeout(ackTimeout);
		return this;
	}

}
