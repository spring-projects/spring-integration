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

import java.util.Map;
import java.util.concurrent.Executor;

import org.aopalliance.aop.Advice;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.ListenerContainerIdleEvent;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.support.ConditionalExceptionLogger;
import org.springframework.amqp.support.ConsumerTagStrategy;
import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.ErrorHandler;
import org.springframework.util.backoff.BackOff;

/**
 * Base class for container specs.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public abstract class AbstractMessageListenerContainerSpec<S extends AbstractMessageListenerContainerSpec<S, C>,
		C extends AbstractMessageListenerContainer>
		extends IntegrationComponentSpec<S, C> {

	public AbstractMessageListenerContainerSpec(C listenerContainer) {
		this.target = listenerContainer;
	}

	@Override
	public S id(String id) {
		return super.id(id);
	}

	/**
	 * @param acknowledgeMode the acknowledgeMode.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setAcknowledgeMode(AcknowledgeMode)
	 */
	public S acknowledgeMode(AcknowledgeMode acknowledgeMode) {
		this.target.setAcknowledgeMode(acknowledgeMode);
		return _this();
	}

	/**
	 * @param queueName a vararg list of queue names to add.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#addQueueNames(String...)
	 */
	public S addQueueNames(String... queueName) {
		this.target.addQueueNames(queueName);
		return _this();
	}

	/**
	 * @param queues a vararg list of queues to add.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#addQueueNames(String...)
	 */
	public S addQueues(Queue... queues) {
		this.target.addQueues(queues);
		return _this();
	}

	/**
	 * @param errorHandler the errorHandler.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setErrorHandler(ErrorHandler)
	 */
	public S errorHandler(ErrorHandler errorHandler) {
		this.target.setErrorHandler(errorHandler);
		return _this();
	}

	/**
	 * @param transactional true for transactional channels.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setChannelTransacted(boolean)
	 */
	public S channelTransacted(boolean transactional) {
		this.target.setChannelTransacted(transactional);
		return _this();
	}

	/**
	 * @param adviceChain the adviceChain.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setAdviceChain(Advice[])
	 */
	public S adviceChain(Advice... adviceChain) {
		this.target.setAdviceChain(adviceChain);
		return _this();
	}

	/**
	 * @param recoveryInterval the recoveryInterval
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setRecoveryInterval(long)
	 */
	public S recoveryInterval(long recoveryInterval) {
		this.target.setRecoveryInterval(recoveryInterval);
		return _this();
	}

	/**
	 * @param exclusive true for exclusive.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setExclusive(boolean)
	 */
	public S exclusive(boolean exclusive) {
		this.target.setExclusive(exclusive);
		return _this();
	}

	/**
	 * @param shutdownTimeout the shutdownTimeout.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setShutdownTimeout(long)
	 */
	public S shutdownTimeout(long shutdownTimeout) {
		this.target.setShutdownTimeout(shutdownTimeout);
		return _this();
	}

	/**
	 * Configure an {@link Executor} used to invoke the message listener.
	 * @param taskExecutor the taskExecutor.
	 * @return the spec.
	 */
	public S taskExecutor(Executor taskExecutor) {
		this.target.setTaskExecutor(taskExecutor);
		return _this();
	}

	/**
	 * @param prefetchCount the prefetchCount.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setPrefetchCount(int)
	 */
	public S prefetchCount(int prefetchCount) {
		this.target.setPrefetchCount(prefetchCount);
		return _this();
	}

	/**
	 * Configure a {@link PlatformTransactionManager}; used to synchronize the rabbit transaction
	 * with some other transaction(s).
	 * @param transactionManager the transactionManager.
	 * @return the spec.
	 */
	public S transactionManager(PlatformTransactionManager transactionManager) {
		this.target.setTransactionManager(transactionManager);
		return _this();
	}

	/**
	 * @param defaultRequeueRejected the defaultRequeueRejected.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setDefaultRequeueRejected(boolean)
	 */
	public S defaultRequeueRejected(boolean defaultRequeueRejected) {
		this.target.setDefaultRequeueRejected(defaultRequeueRejected);
		return _this();
	}

	/**
	 * Determine whether or not the container should de-batch batched
	 * messages (true) or call the listener with the batch (false). Default: true.
	 * @param deBatchingEnabled the deBatchingEnabled to set.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setDeBatchingEnabled(boolean)
	 */
	public S deBatchingEnabled(boolean deBatchingEnabled) {
		this.target.setDeBatchingEnabled(deBatchingEnabled);
		return _this();
	}

	/**
	 * Set {@link MessagePostProcessor}s that will be applied after message reception, before
	 * invoking the {@link MessageListener}. Often used to decompress data.  Processors are invoked in order,
	 * depending on {@code PriorityOrder}, {@code Order} and finally unordered.
	 * @param afterReceivePostProcessors the post processor.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setAfterReceivePostProcessors(MessagePostProcessor...)
	 */
	public S afterReceivePostProcessors(MessagePostProcessor... afterReceivePostProcessors) {
		this.target.setAfterReceivePostProcessors(afterReceivePostProcessors);
		return _this();
	}

	/**
	 * Set a qualifier that will prefix the connection factory lookup key; default none.
	 * @param lookupKeyQualifier the qualifier
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setLookupKeyQualifier(String)
	 */
	public S lookupKeyQualifier(String lookupKeyQualifier) {
		this.target.setLookupKeyQualifier(lookupKeyQualifier);
		return _this();
	}

	/**
	 * Set the implementation of {@link ConsumerTagStrategy} to generate consumer tags.
	 * By default, the RabbitMQ server generates consumer tags.
	 * @param consumerTagStrategy the consumerTagStrategy to set.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setConsumerTagStrategy(ConsumerTagStrategy)
	 */
	public S consumerTagStrategy(ConsumerTagStrategy consumerTagStrategy) {
		this.target.setConsumerTagStrategy(consumerTagStrategy);
		return _this();
	}

	/**
	 * Set consumer arguments.
	 * @param args the arguments.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setConsumerArguments(Map)
	 */
	public S consumerArguments(Map<String, Object> args) {
		this.target.setConsumerArguments(args);
		return _this();
	}

	/**
	 * How often to emit {@link ListenerContainerIdleEvent}s in milliseconds.
	 * @param idleEventInterval the interval.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setIdleEventInterval(long)
	 */
	public S idleEventInterval(long idleEventInterval) {
		this.target.setIdleEventInterval(idleEventInterval);
		return _this();
	}

	/**
	 * Set the transaction attribute to use when using an external transaction manager.
	 * @param transactionAttribute the transaction attribute to set
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setTransactionAttribute(TransactionAttribute)
	 */
	public S transactionAttribute(TransactionAttribute transactionAttribute) {
		this.target.setTransactionAttribute(transactionAttribute);
		return _this();
	}

	/**
	 * Specify the {@link BackOff} for interval between recovery attempts.
	 * The default is 5000 ms, that is, 5 seconds.
	 * With the {@link BackOff} you can supply the {@code maxAttempts} for recovery before
	 * the {@code stop()} will be performed.
	 * @param recoveryBackOff The BackOff to recover.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setRecoveryBackOff(BackOff)
	 */
	public S recoveryBackOff(BackOff recoveryBackOff) {
		this.target.setRecoveryBackOff(recoveryBackOff);
		return _this();
	}

	/**
	 * Set the {@link MessagePropertiesConverter} for this listener container.
	 * @param messagePropertiesConverter The properties converter.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setMessagePropertiesConverter(MessagePropertiesConverter)
	 */
	public S messagePropertiesConverter(MessagePropertiesConverter messagePropertiesConverter) {
		this.target.setMessagePropertiesConverter(messagePropertiesConverter);
		return _this();
	}

	/**
	 * If all of the configured queue(s) are not available on the broker, this setting
	 * determines whether the condition is fatal. When true, and
	 * the queues are missing during startup, the context refresh() will fail.
	 * <p> When false, the condition is not considered fatal and the container will
	 * continue to attempt to start the consumers.
	 * @param missingQueuesFatal the missingQueuesFatal to set.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setMissingQueuesFatal(boolean)
	 */
	public S missingQueuesFatal(boolean missingQueuesFatal) {
		this.target.setMissingQueuesFatal(missingQueuesFatal);
		return _this();
	}

	/**
	 * Prevent the container from starting if any of the queues defined in the context have
	 * mismatched arguments (TTL etc). Default false.
	 * @param mismatchedQueuesFatal true to fail initialization when this condition occurs.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setMismatchedQueuesFatal(boolean)
	 */
	public S mismatchedQueuesFatal(boolean mismatchedQueuesFatal) {
		this.target.setMismatchedQueuesFatal(mismatchedQueuesFatal);
		return _this();
	}

	/**
	 * Set to true to automatically declare elements (queues, exchanges, bindings)
	 * in the application context during container start().
	 * @param autoDeclare the boolean flag to indicate an declaration operation.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setAutoDeclare(boolean)
	 */
	public S autoDeclare(boolean autoDeclare) {
		this.target.setAutoDeclare(autoDeclare);
		return _this();
	}

	/**
	 * Set the interval between passive queue declaration attempts in milliseconds.
	 * @param failedDeclarationRetryInterval the interval, default 5000.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setFailedDeclarationRetryInterval(long)
	 */
	public S failedDeclarationRetryInterval(long failedDeclarationRetryInterval) {
		this.target.setFailedDeclarationRetryInterval(failedDeclarationRetryInterval);
		return _this();
	}

	/**
	 * Set whether a message with a null messageId is fatal for the consumer
	 * when using stateful retry. When false, instead of stopping the consumer,
	 * the message is rejected and not requeued - it will be discarded or routed
	 * to the dead letter queue, if so configured. Default true.
	 * @param statefulRetryFatalWithNullMessageId true for fatal.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setStatefulRetryFatalWithNullMessageId(boolean)
	 */
	public S statefulRetryFatalWithNullMessageId(boolean statefulRetryFatalWithNullMessageId) {
		this.target.setStatefulRetryFatalWithNullMessageId(statefulRetryFatalWithNullMessageId);
		return _this();
	}

	/**
	 * Set a {@link ConditionalExceptionLogger} for logging exclusive consumer failures. The
	 * default is to log such failures at WARN level.
	 * @param exclusiveConsumerExceptionLogger the conditional exception logger.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setExclusiveConsumerExceptionLogger(ConditionalExceptionLogger)
	 */
	public S exclusiveConsumerExceptionLogger(ConditionalExceptionLogger exclusiveConsumerExceptionLogger) {
		this.target.setExclusiveConsumerExceptionLogger(exclusiveConsumerExceptionLogger);
		return _this();
	}

	/**
	 * Set to true to always requeue on transaction rollback with an external TransactionManager.
	 * @param alwaysRequeueWithTxManagerRollback true to always requeue on rollback.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setAlwaysRequeueWithTxManagerRollback(boolean)
	 */
	public S alwaysRequeueWithTxManagerRollback(boolean alwaysRequeueWithTxManagerRollback) {
		this.target.setAlwaysRequeueWithTxManagerRollback(alwaysRequeueWithTxManagerRollback);
		return _this();
	}

}
