/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.amqp.dsl;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;

import org.aopalliance.aop.Advice;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ErrorHandler;

/**
 * A {@link MessageProducerSpec} for {@link AmqpInboundChannelAdapter}s.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public class AmqpInboundChannelAdapterSpec extends AmqpBaseInboundChannelAdapterSpec<AmqpInboundChannelAdapterSpec>
		implements ComponentsRegistration {

	private final SimpleMessageListenerContainer listenerContainer;

	AmqpInboundChannelAdapterSpec(SimpleMessageListenerContainer listenerContainer) {
		super(new AmqpInboundChannelAdapter(listenerContainer));
		this.listenerContainer = listenerContainer;
	}

	/**
	 * @param acknowledgeMode the acknowledgeMode.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setAcknowledgeMode(AcknowledgeMode)
	 */
	public AmqpInboundChannelAdapterSpec acknowledgeMode(AcknowledgeMode acknowledgeMode) {
		this.listenerContainer.setAcknowledgeMode(acknowledgeMode);
		return this;
	}

	/**
	 * @param queueName a vararg list of queue names to add.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#addQueueNames(String...)
	 */
	public AmqpInboundChannelAdapterSpec addQueueNames(String... queueName) {
		this.listenerContainer.addQueueNames(queueName);
		return this;
	}

	/**
	 * @param queues a vararg list of queues to add.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#addQueueNames(String...)
	 */
	public AmqpInboundChannelAdapterSpec addQueues(Queue... queues) {
		this.listenerContainer.addQueues(queues);
		return this;
	}

	/**
	 * @param errorHandler the errorHandler.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setErrorHandler(ErrorHandler)
	 */
	public AmqpInboundChannelAdapterSpec errorHandler(ErrorHandler errorHandler) {
		this.listenerContainer.setErrorHandler(errorHandler);
		return this;
	}

	/**
	 * @param transactional true for transactional channels.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setChannelTransacted(boolean)
	 */
	public AmqpInboundChannelAdapterSpec channelTransacted(boolean transactional) {
		this.listenerContainer.setChannelTransacted(transactional);
		return this;
	}

	/**
	 * @param adviceChain the adviceChain.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setAdviceChain(Advice[])
	 */
	public AmqpInboundChannelAdapterSpec adviceChain(Advice... adviceChain) {
		this.listenerContainer.setAdviceChain(adviceChain);
		return this;
	}

	/**
	 * @param recoveryInterval the recoveryInterval
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setRecoveryInterval(long)
	 */
	public AmqpInboundChannelAdapterSpec recoveryInterval(long recoveryInterval) {
		this.listenerContainer.setRecoveryInterval(recoveryInterval);
		return this;
	}

	/**
	 * @param concurrentConsumers the concurrentConsumers
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setConcurrentConsumers(int)
	 */
	public AmqpInboundChannelAdapterSpec concurrentConsumers(int concurrentConsumers) {
		this.listenerContainer.setConcurrentConsumers(concurrentConsumers);
		return this;
	}

	/**
	 * @param maxConcurrentConsumers the maxConcurrentConsumers.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setMaxConcurrentConsumers(int)
	 */
	public AmqpInboundChannelAdapterSpec maxConcurrentConsumers(int maxConcurrentConsumers) {
		this.listenerContainer.setMaxConcurrentConsumers(maxConcurrentConsumers);
		return this;
	}

	/**
	 * @param exclusive true for exclusive.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setExclusive(boolean)
	 */
	public AmqpInboundChannelAdapterSpec exclusive(boolean exclusive) {
		this.listenerContainer.setExclusive(exclusive);
		return this;
	}

	/**
	 * @param startConsumerMinInterval the startConsumerMinInterval
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setStartConsumerMinInterval(long)
	 */
	public AmqpInboundChannelAdapterSpec startConsumerMinInterval(long startConsumerMinInterval) {
		this.listenerContainer.setStartConsumerMinInterval(startConsumerMinInterval);
		return this;
	}

	/**
	 * @param stopConsumerMinInterval the stopConsumerMinInterval.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setStopConsumerMinInterval(long)
	 */
	public AmqpInboundChannelAdapterSpec stopConsumerMinInterval(long stopConsumerMinInterval) {
		this.listenerContainer.setStopConsumerMinInterval(stopConsumerMinInterval);
		return this;
	}

	/**
	 * @param consecutiveActiveTrigger the consecutiveActiveTrigger.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setConsecutiveActiveTrigger(int)
	 */
	public AmqpInboundChannelAdapterSpec consecutiveActiveTrigger(int consecutiveActiveTrigger) {
		this.listenerContainer.setConsecutiveActiveTrigger(consecutiveActiveTrigger);
		return this;
	}

	/**
	 * @param consecutiveIdleTrigger the consecutiveIdleTrigger.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setConsecutiveIdleTrigger(int)
	 */
	public AmqpInboundChannelAdapterSpec consecutiveIdleTrigger(int consecutiveIdleTrigger) {
		this.listenerContainer.setConsecutiveIdleTrigger(consecutiveIdleTrigger);
		return this;
	}

	/**
	 * @param receiveTimeout the receiveTimeout
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setReceiveTimeout(long)
	 */
	public AmqpInboundChannelAdapterSpec receiveTimeout(long receiveTimeout) {
		this.listenerContainer.setReceiveTimeout(receiveTimeout);
		return this;
	}

	/**
	 * @param shutdownTimeout the shutdownTimeout.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setShutdownTimeout(long)
	 */
	public AmqpInboundChannelAdapterSpec shutdownTimeout(long shutdownTimeout) {
		this.listenerContainer.setShutdownTimeout(shutdownTimeout);
		return this;
	}

	/**
	 * Configure an {@link Executor} used to invoke the message listener.
	 * @param taskExecutor the taskExecutor.
	 * @return the spec.
	 */
	public AmqpInboundChannelAdapterSpec taskExecutor(Executor taskExecutor) {
		this.listenerContainer.setTaskExecutor(taskExecutor);
		return this;
	}

	/**
	 * @param prefetchCount the prefetchCount.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setPrefetchCount(int)
	 */
	public AmqpInboundChannelAdapterSpec prefetchCount(int prefetchCount) {
		this.listenerContainer.setPrefetchCount(prefetchCount);
		return this;
	}

	/**
	 * @param txSize the txSize.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setTxSize(int)
	 */
	public AmqpInboundChannelAdapterSpec txSize(int txSize) {
		this.listenerContainer.setTxSize(txSize);
		return this;
	}

	/**
	 * Configure a {@link PlatformTransactionManager}; used to synchronize the rabbit transaction
	 * with some other transaction(s).
	 * @param transactionManager the transactionManager.
	 * @return the spec.
	 */
	public AmqpInboundChannelAdapterSpec transactionManager(PlatformTransactionManager transactionManager) {
		this.listenerContainer.setTransactionManager(transactionManager);
		return this;
	}

	/**
	 * @param defaultRequeueRejected the defaultRequeueRejected.
	 * @return the spec.
	 * @see SimpleMessageListenerContainer#setDefaultRequeueRejected(boolean)
	 */
	public AmqpInboundChannelAdapterSpec defaultRequeueRejected(boolean defaultRequeueRejected) {
		this.listenerContainer.setDefaultRequeueRejected(defaultRequeueRejected);
		return this;
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Collections.<Object>singleton(this.listenerContainer);
	}

}
