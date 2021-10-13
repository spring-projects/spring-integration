/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.scheduling;

import java.util.List;
import java.util.concurrent.Executor;

import org.aopalliance.aop.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class PollerMetadata {

	/**
	 * The constant for unlimited number of message to poll in one cycle.
	 */
	public static final int MAX_MESSAGES_UNBOUNDED = Integer.MIN_VALUE;

	/**
	 * The default receive timeout as one second.
	 */
	public static final long DEFAULT_RECEIVE_TIMEOUT = 1000;

	/**
	 * The bean name for global default poller.
	 */
	public static final String DEFAULT_POLLER_METADATA_BEAN_NAME =
			"org.springframework.integration.context.defaultPollerMetadata";

	/**
	 *  A convenient short alias for the global default poller bean name.
	 */
	public static final String DEFAULT_POLLER = DEFAULT_POLLER_METADATA_BEAN_NAME;

	private Trigger trigger;

	private long maxMessagesPerPoll = MAX_MESSAGES_UNBOUNDED;

	private long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;

	private ErrorHandler errorHandler;

	private List<Advice> adviceChain;

	private Executor taskExecutor;

	private long sendTimeout;

	private TransactionSynchronizationFactory transactionSynchronizationFactory;


	public void setTransactionSynchronizationFactory(
			TransactionSynchronizationFactory transactionSynchronizationFactory) {
		Assert.notNull(transactionSynchronizationFactory, "'transactionSynchronizationFactory' must not be null");
		this.transactionSynchronizationFactory = transactionSynchronizationFactory;
	}

	public TransactionSynchronizationFactory getTransactionSynchronizationFactory() {
		return this.transactionSynchronizationFactory;
	}

	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}

	public Trigger getTrigger() {
		return this.trigger;
	}

	public ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Set the maximum number of messages to receive for each poll.
	 * A non-positive value indicates that polling should repeat as long
	 * as non-null messages are being received and successfully sent.
	 * <p>The default is unbounded.
	 * @param maxMessagesPerPoll The maxMessagesPerPoll to set.
	 * @see #MAX_MESSAGES_UNBOUNDED
	 */
	public void setMaxMessagesPerPoll(long maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
	}

	public long getMaxMessagesPerPoll() {
		return this.maxMessagesPerPoll;
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public long getReceiveTimeout() {
		return this.receiveTimeout;
	}

	public void setAdviceChain(List<Advice> adviceChain) {
		this.adviceChain = adviceChain;
	}

	public List<Advice> getAdviceChain() {
		return this.adviceChain;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public Executor getTaskExecutor() {
		return this.taskExecutor;
	}

	public long getSendTimeout() {
		return this.sendTimeout;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}


	/**
	 * Return the default {@link PollerMetadata} bean if available.
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 * @return The poller metadata.
	 */
	public static PollerMetadata getDefaultPollerMetadata(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		if (!beanFactory.containsBean(DEFAULT_POLLER_METADATA_BEAN_NAME)) {
			return null;
		}
		return beanFactory.getBean(DEFAULT_POLLER_METADATA_BEAN_NAME, PollerMetadata.class);
	}

}
