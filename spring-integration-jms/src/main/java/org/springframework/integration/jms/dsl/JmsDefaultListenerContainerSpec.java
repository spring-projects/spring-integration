/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.jms.dsl;

import java.util.concurrent.Executor;

import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.backoff.BackOff;

/**
 * A {@link DefaultMessageListenerContainer} specific {@link JmsListenerContainerSpec} extension.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public class JmsDefaultListenerContainerSpec
		extends JmsListenerContainerSpec<JmsDefaultListenerContainerSpec, DefaultMessageListenerContainer> {

	protected JmsDefaultListenerContainerSpec() {
		super(DefaultMessageListenerContainer.class);
	}

	/**
	 * Specify an {@link Executor}.
	 * @param taskExecutor the {@link Executor} to use.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setTaskExecutor(Executor)
	 */
	public JmsDefaultListenerContainerSpec taskExecutor(Executor taskExecutor) {
		this.target.setTaskExecutor(taskExecutor);
		return this;
	}

	/**
	 * Specify a {@link BackOff}.
	 * @param backOff the {@link BackOff} to use.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setBackOff(BackOff)
	 */
	public JmsDefaultListenerContainerSpec backOff(BackOff backOff) {
		this.target.setBackOff(backOff);
		return this;
	}

	/**
	 * Specify a recovery interval.
	 * @param recoveryInterval the recovery interval to use.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setRecoveryInterval(long)
	 */
	public JmsDefaultListenerContainerSpec recoveryInterval(long recoveryInterval) {
		this.target.setRecoveryInterval(recoveryInterval);
		return this;
	}

	/**
	 * Specify the level of caching that this listener container is allowed to apply,
	 * in the form of the name of the corresponding constant: e.g. "CACHE_CONNECTION".
	 * @param constantName the cache level constant name.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see #cacheLevel(int)
	 * @see DefaultMessageListenerContainer#setCacheLevelName(String)
	 */
	public JmsDefaultListenerContainerSpec cacheLevelName(String constantName) {
		this.target.setCacheLevelName(constantName);
		return this;
	}

	/**
	 * Specify the level of caching that this listener container is allowed to apply.
	 * @param cacheLevel the level of caching.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setCacheLevel(int)
	 */
	public JmsDefaultListenerContainerSpec cacheLevel(int cacheLevel) {
		this.target.setCacheLevel(cacheLevel);
		return this;
	}

	/**
	 * The concurrency to use.
	 * @param concurrency the concurrency.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setConcurrency(String)
	 */
	public JmsDefaultListenerContainerSpec concurrency(String concurrency) {
		this.target.setConcurrency(concurrency);
		return this;
	}

	/**
	 * The concurrent consumers number to use.
	 * @param concurrentConsumers the concurrent consumers count.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setConcurrentConsumers(int)
	 */
	public JmsDefaultListenerContainerSpec concurrentConsumers(int concurrentConsumers) {
		this.target.setConcurrentConsumers(concurrentConsumers);
		return this;
	}

	/**
	 * The max for concurrent consumers number to use.
	 * @param maxConcurrentConsumers the max concurrent consumers count.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setMaxConcurrentConsumers(int)
	 */
	public JmsDefaultListenerContainerSpec maxConcurrentConsumers(int maxConcurrentConsumers) {
		this.target.setMaxConcurrentConsumers(maxConcurrentConsumers);
		return this;
	}

	/**
	 * The max messages per task.
	 * @param maxMessagesPerTask the max messages per task.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setMaxMessagesPerTask(int)
	 */
	public JmsDefaultListenerContainerSpec maxMessagesPerTask(int maxMessagesPerTask) {
		this.target.setMaxMessagesPerTask(maxMessagesPerTask);
		return this;
	}

	/**
	 * The max for concurrent consumers number to use.
	 * @param idleConsumerLimit the limit for idle consumer.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setMaxConcurrentConsumers(int)
	 */
	public JmsDefaultListenerContainerSpec idleConsumerLimit(int idleConsumerLimit) {
		this.target.setIdleConsumerLimit(idleConsumerLimit);
		return this;
	}

	/**
	 * The limit for idle task.
	 * @param idleTaskExecutionLimit the limit for idle task.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setIdleTaskExecutionLimit(int)
	 */
	public JmsDefaultListenerContainerSpec idleTaskExecutionLimit(int idleTaskExecutionLimit) {
		this.target.setIdleTaskExecutionLimit(idleTaskExecutionLimit);
		return this;
	}

	/**
	 * A {@link PlatformTransactionManager} reference.
	 * @param transactionManager the {@link PlatformTransactionManager} to use.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setTransactionManager(PlatformTransactionManager)
	 */
	public JmsDefaultListenerContainerSpec transactionManager(PlatformTransactionManager transactionManager) {
		this.target.setTransactionManager(transactionManager);
		return this;
	}

	/**
	 * A name for transaction.
	 * @param transactionName the name for transaction.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setTransactionName(String)
	 */
	public JmsDefaultListenerContainerSpec transactionName(String transactionName) {
		this.target.setTransactionName(transactionName);
		return this;
	}

	/**
	 * A transaction timeout.
	 * @param transactionTimeout the transaction timeout.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setTransactionTimeout(int)
	 */
	public JmsDefaultListenerContainerSpec transactionTimeout(int transactionTimeout) {
		this.target.setTransactionTimeout(transactionTimeout);
		return this;
	}

	/**
	 * A receive timeout.
	 * @param receiveTimeout the receive timeout.
	 * @return current {@link JmsDefaultListenerContainerSpec}.
	 * @see DefaultMessageListenerContainer#setReceiveTimeout(long)
	 */
	public JmsDefaultListenerContainerSpec receiveTimeout(long receiveTimeout) {
		this.target.setReceiveTimeout(receiveTimeout);
		return this;
	}

}
