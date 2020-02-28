/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.aopalliance.aop.Advice;

import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.transaction.TransactionInterceptorBuilder;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.Trigger;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.ErrorHandler;

/**
 * An {@link IntegrationComponentSpec} for {@link PollerMetadata}s.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public final class PollerSpec extends IntegrationComponentSpec<PollerSpec, PollerMetadata>
		implements ComponentsRegistration {

	private final List<Advice> adviceChain = new LinkedList<>();

	private final Map<Object, String> componentsToRegister = new LinkedHashMap<>();

	PollerSpec(Trigger trigger) {
		this.target = new PollerMetadata();
		this.target.setAdviceChain(this.adviceChain);
		this.target.setTrigger(trigger);
	}

	/**
	 * Specify the {@link TransactionSynchronizationFactory} to attach a
	 * {@link org.springframework.transaction.support.TransactionSynchronization}
	 * to the transaction around {@code poll} operation.
	 * @param transactionSynchronizationFactory the TransactionSynchronizationFactory to use.
	 * @return the spec.
	 */
	public PollerSpec transactionSynchronizationFactory(
			TransactionSynchronizationFactory transactionSynchronizationFactory) {
		this.target.setTransactionSynchronizationFactory(transactionSynchronizationFactory);
		return this;
	}

	/**
	 * Specify the {@link ErrorHandler} to wrap a {@code taskExecutor}
	 * to the {@link org.springframework.integration.util.ErrorHandlingTaskExecutor}.
	 * @param errorHandler the {@link ErrorHandler} to use.
	 * @return the spec.
	 * @see #taskExecutor(Executor)
	 */
	public PollerSpec errorHandler(ErrorHandler errorHandler) {
		this.target.setErrorHandler(errorHandler);
		return this;
	}

	/**
	 * Specify a {@link MessageChannel} to use for sending error message in case
	 * of polling failures.
	 * @param errorChannel the {@link MessageChannel} to use.
	 * @return the spec.
	 * @see MessagePublishingErrorHandler
	 */
	public PollerSpec errorChannel(MessageChannel errorChannel) {
		MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
		errorHandler.setDefaultErrorChannel(errorChannel);
		this.componentsToRegister.put(errorHandler, null);
		return errorHandler(errorHandler);
	}

	/**
	 * Specify a bean name for the {@link MessageChannel} to use for sending error message in case
	 * of polling failures.
	 * @param errorChannelName the bean name for {@link MessageChannel} to use.
	 * @return the spec.
	 * @see MessagePublishingErrorHandler
	 */
	public PollerSpec errorChannel(String errorChannelName) {
		MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
		errorHandler.setDefaultErrorChannelName(errorChannelName);
		this.componentsToRegister.put(errorHandler, null);
		return errorHandler(errorHandler);
	}

	/**
	 * @param maxMessagesPerPoll the maxMessagesPerPoll to set.
	 * @return the spec.
	 * @see PollerMetadata#setMaxMessagesPerPoll
	 */
	public PollerSpec maxMessagesPerPoll(long maxMessagesPerPoll) {
		this.target.setMaxMessagesPerPoll(maxMessagesPerPoll);
		return this;
	}

	/**
	 * Specify a timeout in milliseconds to wait for a message in the
	 * {@link org.springframework.messaging.MessageChannel}.
	 * Defaults to {@code 1000}.
	 * @param receiveTimeout the timeout to use.
	 * @return the spec.
	 * @see org.springframework.messaging.PollableChannel#receive(long)
	 */
	public PollerSpec receiveTimeout(long receiveTimeout) {
		this.target.setReceiveTimeout(receiveTimeout);
		return this;
	}

	/**
	 * Specify AOP {@link Advice}s for the {@code pollingTask}.
	 * @param advice the {@link Advice}s to use.
	 * @return the spec.
	 */
	public PollerSpec advice(Advice... advice) {
		this.adviceChain.addAll(Arrays.asList(advice));
		return this;
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with the
	 * provided {@code PlatformTransactionManager} and default
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute}
	 * for the {@code pollingTask}.
	 * @param transactionManager the {@link TransactionManager} to use.
	 * @return the spec.
	 */
	public PollerSpec transactional(TransactionManager transactionManager) {
		return transactional(new TransactionInterceptorBuilder()
				.transactionManager(transactionManager)
				.build());
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with default
	 * {@code PlatformTransactionManager} and
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute} for
	 * the {@code pollingTask}.
	 * @return the spec.
	 */
	public PollerSpec transactional() {
		TransactionInterceptor transactionInterceptor = new TransactionInterceptorBuilder().build();
		this.componentsToRegister.put(transactionInterceptor, null);
		return transactional(transactionInterceptor);
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} for the {@code pollingTask}.
	 * @param transactionInterceptor the {@link TransactionInterceptor} to use.
	 * @return the spec.
	 * @see TransactionInterceptorBuilder
	 */
	public PollerSpec transactional(TransactionInterceptor transactionInterceptor) {
		return advice(transactionInterceptor);
	}

	/**
	 * Specify an {@link Executor} to perform the {@code pollingTask}.
	 * @param taskExecutor the {@link Executor} to use.
	 * @return the spec.
	 */
	public PollerSpec taskExecutor(Executor taskExecutor) {
		this.target.setTaskExecutor(taskExecutor);
		return this;
	}

	public PollerSpec sendTimeout(long sendTimeout) {
		this.target.setSendTimeout(sendTimeout);
		return this;
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return this.componentsToRegister;
	}

}
