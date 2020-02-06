/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
