/*
 * Copyright 2017 the original author or authors.
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

import java.util.concurrent.Executor;

import org.aopalliance.aop.Advice;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ErrorHandler;

/**
 * Base class for container specs.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public abstract class AbstractMessageListenerContainerSpec<S extends AbstractMessageListenerContainerSpec<S, C>,
		C extends AbstractMessageListenerContainer> {

	private final C listenerContainer;

	public AbstractMessageListenerContainerSpec(C listenerContainer) {
		this.listenerContainer = listenerContainer;
	}

	/**
	 * @param acknowledgeMode the acknowledgeMode.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setAcknowledgeMode(AcknowledgeMode)
	 */
	public S acknowledgeMode(AcknowledgeMode acknowledgeMode) {
		this.listenerContainer.setAcknowledgeMode(acknowledgeMode);
		return _this();
	}

	/**
	 * @param queueName a vararg list of queue names to add.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#addQueueNames(String...)
	 */
	public S addQueueNames(String... queueName) {
		this.listenerContainer.addQueueNames(queueName);
		return _this();
	}

	/**
	 * @param queues a vararg list of queues to add.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#addQueueNames(String...)
	 */
	public S addQueues(Queue... queues) {
		this.listenerContainer.addQueues(queues);
		return _this();
	}

	/**
	 * @param errorHandler the errorHandler.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setErrorHandler(ErrorHandler)
	 */
	public S errorHandler(ErrorHandler errorHandler) {
		this.listenerContainer.setErrorHandler(errorHandler);
		return _this();
	}

	/**
	 * @param transactional true for transactional channels.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setChannelTransacted(boolean)
	 */
	public S channelTransacted(boolean transactional) {
		this.listenerContainer.setChannelTransacted(transactional);
		return _this();
	}

	/**
	 * @param adviceChain the adviceChain.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setAdviceChain(Advice[])
	 */
	public S adviceChain(Advice... adviceChain) {
		this.listenerContainer.setAdviceChain(adviceChain);
		return _this();
	}

	/**
	 * @param recoveryInterval the recoveryInterval
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setRecoveryInterval(long)
	 */
	public S recoveryInterval(long recoveryInterval) {
		this.listenerContainer.setRecoveryInterval(recoveryInterval);
		return _this();
	}

	/**
	 * @param exclusive true for exclusive.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setExclusive(boolean)
	 */
	public S exclusive(boolean exclusive) {
		this.listenerContainer.setExclusive(exclusive);
		return _this();
	}

	/**
	 * @param shutdownTimeout the shutdownTimeout.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setShutdownTimeout(long)
	 */
	public S shutdownTimeout(long shutdownTimeout) {
		this.listenerContainer.setShutdownTimeout(shutdownTimeout);
		return _this();
	}

	/**
	 * Configure an {@link Executor} used to invoke the message listener.
	 * @param taskExecutor the taskExecutor.
	 * @return the spec.
	 */
	public S taskExecutor(Executor taskExecutor) {
		this.listenerContainer.setTaskExecutor(taskExecutor);
		return _this();
	}

	/**
	 * @param prefetchCount the prefetchCount.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setPrefetchCount(int)
	 */
	public S prefetchCount(int prefetchCount) {
		this.listenerContainer.setPrefetchCount(prefetchCount);
		return _this();
	}

	/**
	 * Configure a {@link PlatformTransactionManager}; used to synchronize the rabbit transaction
	 * with some other transaction(s).
	 * @param transactionManager the transactionManager.
	 * @return the spec.
	 */
	public S transactionManager(PlatformTransactionManager transactionManager) {
		this.listenerContainer.setTransactionManager(transactionManager);
		return _this();
	}

	/**
	 * @param defaultRequeueRejected the defaultRequeueRejected.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setDefaultRequeueRejected(boolean)
	 */
	public S defaultRequeueRejected(boolean defaultRequeueRejected) {
		this.listenerContainer.setDefaultRequeueRejected(defaultRequeueRejected);
		return _this();
	}

	@SuppressWarnings("unchecked")
	protected final S _this() {
		return (S) this;
	}

}
