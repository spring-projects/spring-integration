/*
 * Copyright 2014-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

import org.aopalliance.aop.Advice;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.integration.amqp.channel.AbstractAmqpChannel;
import org.springframework.integration.amqp.config.AmqpChannelFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.ErrorHandler;

/**
 * An {@link AmqpPollableMessageChannelSpec} for a message-driven
 * {@link org.springframework.integration.amqp.channel.PointToPointSubscribableAmqpChannel}.
 *
 * @param <S> the target {@link AmqpMessageChannelSpec} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Vozhdayenko
 *
 * @since 5.0
 */
public class AmqpMessageChannelSpec<S extends AmqpMessageChannelSpec<S, T>, T extends AbstractAmqpChannel>
		extends AmqpPollableMessageChannelSpec<S, T> {

	protected final List<Advice> adviceChain = new LinkedList<>(); // NOSONAR

	protected AmqpMessageChannelSpec(ConnectionFactory connectionFactory) {
		super(new AmqpChannelFactoryBean(true), connectionFactory);
	}

	/**
	 * @param maxSubscribers the maxSubscribers.
	 * @return the spec.
	 * @see org.springframework.integration.amqp.channel.PointToPointSubscribableAmqpChannel#setMaxSubscribers(int)
	 */
	public S maxSubscribers(int maxSubscribers) {
		this.amqpChannelFactoryBean.setMaxSubscribers(maxSubscribers);
		return _this();
	}

	/**
	 * @param acknowledgeMode the acknowledgeMode.
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#setAcknowledgeMode(AcknowledgeMode)
	 */
	public S acknowledgeMode(AcknowledgeMode acknowledgeMode) {
		this.amqpChannelFactoryBean.setAcknowledgeMode(acknowledgeMode);
		return _this();
	}

	/**
	 * @param advice the advice.
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#setAdviceChain(Advice[])
	 */
	public S advice(Advice... advice) {
		this.adviceChain.addAll(Arrays.asList(advice));
		return _this();
	}

	/**
	 * @param autoStartup the autoStartup.
	 * @return the spec.
	 * @see org.springframework.context.SmartLifecycle
	 */
	public S autoStartup(boolean autoStartup) {
		this.amqpChannelFactoryBean.setAutoStartup(autoStartup);
		return _this();
	}

	/**
	 * @param concurrentConsumers the concurrentConsumers
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#setConcurrentConsumers(int)
	 */
	public S concurrentConsumers(int concurrentConsumers) {
		this.amqpChannelFactoryBean.setConcurrentConsumers(concurrentConsumers);
		return _this();
	}

	/**
	 * @param errorHandler the errorHandler.
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#setErrorHandler(ErrorHandler)
	 */
	public S errorHandler(ErrorHandler errorHandler) {
		this.amqpChannelFactoryBean.setErrorHandler(errorHandler);
		return _this();
	}

	/**
	 * @param exposeListenerChannel the exposeListenerChannel.
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#setExposeListenerChannel(boolean)
	 */
	public S exposeListenerChannel(boolean exposeListenerChannel) {
		this.amqpChannelFactoryBean.setExposeListenerChannel(exposeListenerChannel);
		return _this();
	}

	/**
	 * @param phase the phase.
	 * @return the spec.
	 * @see org.springframework.context.SmartLifecycle
	 */
	public S phase(int phase) {
		this.amqpChannelFactoryBean.setPhase(phase);
		return _this();
	}

	/**
	 * @param prefetchCount the prefetchCount.
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#setPrefetchCount(int)
	 */
	public S prefetchCount(int prefetchCount) {
		this.amqpChannelFactoryBean.setPrefetchCount(prefetchCount);
		return _this();
	}

	/**
	 * @param receiveTimeout the receiveTimeout
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#setReceiveTimeout(long)
	 */
	public S receiveTimeout(long receiveTimeout) {
		this.amqpChannelFactoryBean.setReceiveTimeout(receiveTimeout);
		return _this();
	}

	/**
	 * @param recoveryInterval the recoveryInterval
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#setRecoveryInterval(long)
	 */
	public S recoveryInterval(long recoveryInterval) {
		this.amqpChannelFactoryBean.setRecoveryInterval(recoveryInterval);
		return _this();
	}

	/**
	 * @param shutdownTimeout the shutdownTimeout.
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#setShutdownTimeout(long)
	 */
	public S shutdownTimeout(long shutdownTimeout) {
		this.amqpChannelFactoryBean.setShutdownTimeout(shutdownTimeout);
		return _this();
	}

	/**
	 * Configure an {@link Executor} used to invoke the message listener.
	 * @param taskExecutor the taskExecutor.
	 * @return the spec.
	 */
	public S taskExecutor(Executor taskExecutor) {
		this.amqpChannelFactoryBean.setTaskExecutor(taskExecutor);
		return _this();
	}

	/**
	 * Configure a {@link TransactionAttribute} to be used with the
	 * {@link #transactionManager(PlatformTransactionManager)}.
	 * @param transactionAttribute the transactionAttribute.
	 * @return the spec.
	 */
	public S transactionAttribute(TransactionAttribute transactionAttribute) {
		this.amqpChannelFactoryBean.setTransactionAttribute(transactionAttribute);
		return _this();
	}

	/**
	 * Configure a {@link PlatformTransactionManager}; used to synchronize the rabbit transaction
	 * with some other transaction(s).
	 * @param transactionManager the transactionManager.
	 * @return the spec.
	 */
	public S transactionManager(PlatformTransactionManager transactionManager) {
		this.amqpChannelFactoryBean.setTransactionManager(transactionManager);
		return _this();
	}

	/**
	 * Configure the batch size.
	 * @param batchSize the batchSize.
	 * @return the spec.
	 * @since 5.2
	 * @see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#setBatchSize(int)
	 */
	public S batchSize(int batchSize) {
		this.amqpChannelFactoryBean.setBatchSize(batchSize);
		return _this();
	}

	@Override
	protected T doGet() {
		this.amqpChannelFactoryBean.setAdviceChain(this.adviceChain.toArray(new Advice[0]));
		return super.doGet();
	}

}
