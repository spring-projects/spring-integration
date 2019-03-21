/*
 * Copyright 2017 the original author or authors.
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

import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

/**
 * Spec for a {@link SimpleMessageListenerContainer}.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class SimpleMessageListenerContainerSpec extends
		AbstractMessageListenerContainerSpec<SimpleMessageListenerContainerSpec, SimpleMessageListenerContainer> {

	private final SimpleMessageListenerContainer listenerContainer;

	public SimpleMessageListenerContainerSpec(SimpleMessageListenerContainer listenerContainer) {
		super(listenerContainer);
		this.listenerContainer = listenerContainer;
	}

	/**
	 * @param concurrentConsumers the concurrentConsumers
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setConcurrentConsumers(int)
	 */
	public SimpleMessageListenerContainerSpec concurrentConsumers(int concurrentConsumers) {
		this.listenerContainer.setConcurrentConsumers(concurrentConsumers);
		return this;
	}

	/**
	 * @param maxConcurrentConsumers the maxConcurrentConsumers.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setMaxConcurrentConsumers(int)
	 */
	public SimpleMessageListenerContainerSpec maxConcurrentConsumers(int maxConcurrentConsumers) {
		this.listenerContainer.setMaxConcurrentConsumers(maxConcurrentConsumers);
		return this;
	}

	/**
	 * @param startConsumerMinInterval the startConsumerMinInterval
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setStartConsumerMinInterval(long)
	 */
	public SimpleMessageListenerContainerSpec startConsumerMinInterval(long startConsumerMinInterval) {
		this.listenerContainer.setStartConsumerMinInterval(startConsumerMinInterval);
		return this;
	}

	/**
	 * @param stopConsumerMinInterval the stopConsumerMinInterval.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setStopConsumerMinInterval(long)
	 */
	public SimpleMessageListenerContainerSpec stopConsumerMinInterval(long stopConsumerMinInterval) {
		this.listenerContainer.setStopConsumerMinInterval(stopConsumerMinInterval);
		return this;
	}

	/**
	 * @param consecutiveActiveTrigger the consecutiveActiveTrigger.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setConsecutiveActiveTrigger(int)
	 */
	public SimpleMessageListenerContainerSpec consecutiveActiveTrigger(int consecutiveActiveTrigger) {
		this.listenerContainer.setConsecutiveActiveTrigger(consecutiveActiveTrigger);
		return this;
	}

	/**
	 * @param consecutiveIdleTrigger the consecutiveIdleTrigger.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setConsecutiveIdleTrigger(int)
	 */
	public SimpleMessageListenerContainerSpec consecutiveIdleTrigger(int consecutiveIdleTrigger) {
		this.listenerContainer.setConsecutiveIdleTrigger(consecutiveIdleTrigger);
		return this;
	}

	/**
	 * @param receiveTimeout the receiveTimeout
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setReceiveTimeout(long)
	 */
	public SimpleMessageListenerContainerSpec receiveTimeout(long receiveTimeout) {
		this.listenerContainer.setReceiveTimeout(receiveTimeout);
		return this;
	}

	/**
	 * @param txSize the txSize.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setTxSize(int)
	 */
	public SimpleMessageListenerContainerSpec txSize(int txSize) {
		this.listenerContainer.setTxSize(txSize);
		return this;
	}

}
