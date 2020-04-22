/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.config;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.aopalliance.aop.Advice;

import org.springframework.expression.Expression;
import org.springframework.integration.aggregator.AbstractAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.DelegatingMessageGroupProcessor;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.aggregator.MethodInvokingMessageGroupProcessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.util.JavaUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.StringUtils;


/**
 * {@link org.springframework.beans.factory.FactoryBean} to create an
 * {@link AggregatingMessageHandler}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class AggregatorFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<AggregatingMessageHandler> {

	private Object processorBean;

	private String methodName;

	private Boolean expireGroupsUponCompletion;

	private Long sendTimeout;

	private String outputChannelName;

	@SuppressWarnings("deprecation")
	private org.springframework.integration.support.management.AbstractMessageHandlerMetrics metrics;

	private Boolean statsEnabled;

	private Boolean countsEnabled;

	private LockRegistry lockRegistry;

	private MessageGroupStore messageStore;

	private CorrelationStrategy correlationStrategy;

	private ReleaseStrategy releaseStrategy;

	private Expression groupTimeoutExpression;

	private List<Advice> forceReleaseAdviceChain;

	private TaskScheduler taskScheduler;

	private MessageChannel discardChannel;

	private String discardChannelName;

	private Boolean sendPartialResultOnExpiry;

	private Long minimumTimeoutForEmptyGroups;

	private Boolean expireGroupsUponTimeout;

	private Boolean popSequence;

	private Boolean releaseLockBeforeSend;

	private Function<MessageGroup, Map<String, Object>> headersFunction;

	public void setProcessorBean(Object processorBean) {
		this.processorBean = processorBean;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public void setExpireGroupsUponCompletion(Boolean expireGroupsUponCompletion) {
		this.expireGroupsUponCompletion = expireGroupsUponCompletion;
	}

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	public void setOutputChannelName(String outputChannelName) {
		this.outputChannelName = outputChannelName;
	}

	/**
	 * Deprecated.
	 * @param metrics the metrics.
	 * @deprecated in favor of Micrometer metrics.
	 */
	@Deprecated
	@SuppressWarnings("deprecation")
	public void setMetrics(org.springframework.integration.support.management.AbstractMessageHandlerMetrics metrics) {
		this.metrics = metrics;
	}

	public void setStatsEnabled(Boolean statsEnabled) {
		this.statsEnabled = statsEnabled;
	}

	public void setCountsEnabled(Boolean countsEnabled) {
		this.countsEnabled = countsEnabled;
	}

	public void setLockRegistry(LockRegistry lockRegistry) {
		this.lockRegistry = lockRegistry;
	}

	public void setMessageStore(MessageGroupStore messageStore) {
		this.messageStore = messageStore;
	}

	public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
		this.correlationStrategy = correlationStrategy;
	}

	public void setReleaseStrategy(ReleaseStrategy releaseStrategy) {
		this.releaseStrategy = releaseStrategy;
	}

	public void setGroupTimeoutExpression(Expression groupTimeoutExpression) {
		this.groupTimeoutExpression = groupTimeoutExpression;
	}

	public void setForceReleaseAdviceChain(List<Advice> forceReleaseAdviceChain) {
		this.forceReleaseAdviceChain = forceReleaseAdviceChain;
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	public void setDiscardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	public void setDiscardChannelName(String discardChannelName) {
		this.discardChannelName = discardChannelName;
	}

	public void setSendPartialResultOnExpiry(Boolean sendPartialResultOnExpiry) {
		this.sendPartialResultOnExpiry = sendPartialResultOnExpiry;
	}

	public void setMinimumTimeoutForEmptyGroups(Long minimumTimeoutForEmptyGroups) {
		this.minimumTimeoutForEmptyGroups = minimumTimeoutForEmptyGroups;
	}

	public void setExpireGroupsUponTimeout(Boolean expireGroupsUponTimeout) {
		this.expireGroupsUponTimeout = expireGroupsUponTimeout;
	}

	public void setPopSequence(Boolean popSequence) {
		this.popSequence = popSequence;
	}

	public void setReleaseLockBeforeSend(Boolean releaseLockBeforeSend) {
		this.releaseLockBeforeSend = releaseLockBeforeSend;
	}

	public void setHeadersFunction(Function<MessageGroup, Map<String, Object>> headersFunction) {
		this.headersFunction = headersFunction;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected AggregatingMessageHandler createHandler() {
		MessageGroupProcessor outputProcessor;
		if (this.processorBean instanceof MessageGroupProcessor) {
			outputProcessor = (MessageGroupProcessor) this.processorBean;
		}
		else {
			if (!StringUtils.hasText(this.methodName)) {
				outputProcessor = new MethodInvokingMessageGroupProcessor(this.processorBean);
			}
			else {
				outputProcessor = new MethodInvokingMessageGroupProcessor(this.processorBean, this.methodName);
			}
		}

		if (this.headersFunction != null) {
			if (outputProcessor instanceof AbstractAggregatingMessageGroupProcessor) {
				((AbstractAggregatingMessageGroupProcessor) outputProcessor).setHeadersFunction(this.headersFunction);
			}
			else {
				outputProcessor = new DelegatingMessageGroupProcessor(outputProcessor, this.headersFunction);
			}
		}

		AggregatingMessageHandler aggregator = new AggregatingMessageHandler(outputProcessor);
		JavaUtils.INSTANCE
				.acceptIfNotNull(this.expireGroupsUponCompletion, aggregator::setExpireGroupsUponCompletion)
				.acceptIfNotNull(this.sendTimeout, aggregator::setSendTimeout)
				.acceptIfNotNull(this.outputChannelName, aggregator::setOutputChannelName)
				.acceptIfNotNull(this.metrics, aggregator::configureMetrics)
				.acceptIfNotNull(this.statsEnabled, aggregator::setStatsEnabled)
				.acceptIfNotNull(this.countsEnabled, aggregator::setCountsEnabled)
				.acceptIfNotNull(this.lockRegistry, aggregator::setLockRegistry)
				.acceptIfNotNull(this.messageStore, aggregator::setMessageStore)
				.acceptIfNotNull(this.correlationStrategy, aggregator::setCorrelationStrategy)
				.acceptIfNotNull(this.releaseStrategy, aggregator::setReleaseStrategy)
				.acceptIfNotNull(this.groupTimeoutExpression, aggregator::setGroupTimeoutExpression)
				.acceptIfNotNull(this.forceReleaseAdviceChain, aggregator::setForceReleaseAdviceChain)
				.acceptIfNotNull(this.taskScheduler, aggregator::setTaskScheduler)
				.acceptIfNotNull(this.discardChannel, aggregator::setDiscardChannel)
				.acceptIfNotNull(this.discardChannelName, aggregator::setDiscardChannelName)
				.acceptIfNotNull(this.sendPartialResultOnExpiry, aggregator::setSendPartialResultOnExpiry)
				.acceptIfNotNull(this.minimumTimeoutForEmptyGroups, aggregator::setMinimumTimeoutForEmptyGroups)
				.acceptIfNotNull(this.expireGroupsUponTimeout, aggregator::setExpireGroupsUponTimeout)
				.acceptIfNotNull(this.popSequence, aggregator::setPopSequence)
				.acceptIfNotNull(this.releaseLockBeforeSend, aggregator::setReleaseLockBeforeSend);

		return aggregator;
	}

	@Override
	protected Class<? extends MessageHandler> getPreCreationHandlerType() {
		return AggregatingMessageHandler.class;
	}

}
