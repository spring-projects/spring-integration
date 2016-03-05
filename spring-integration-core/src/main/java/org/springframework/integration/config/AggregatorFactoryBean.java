/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.config;

import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.expression.Expression;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.aggregator.MethodInvokingMessageGroupProcessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.management.AbstractMessageHandlerMetrics;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.StringUtils;


/**
 * {@link FactoryBean} to create an {@link AggregatingMessageHandler}.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class AggregatorFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<AggregatingMessageHandler> {

	private Object processorBean;

	private String methodName;

	private Boolean expireGroupsUponCompletion;

	private Long sendTimeout;

	private String outputChannelName;

	private AbstractMessageHandlerMetrics metrics;

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

	public void setOutputChannelName(String outputChannelName) {
		this.outputChannelName = outputChannelName;
	}

	public void setMetrics(AbstractMessageHandlerMetrics metrics) {
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
		AggregatingMessageHandler aggregator = new AggregatingMessageHandler(outputProcessor);

		if (this.expireGroupsUponCompletion != null) {
			aggregator.setExpireGroupsUponCompletion(this.expireGroupsUponCompletion);
		}

		if (this.sendTimeout != null) {
			aggregator.setSendTimeout(this.sendTimeout);
		}

		if (this.outputChannelName != null) {
			aggregator.setOutputChannelName(this.outputChannelName);
		}

		if (this.metrics != null) {
			aggregator.configureMetrics(this.metrics);
		}

		if (this.statsEnabled != null) {
			aggregator.setStatsEnabled(this.statsEnabled);
		}

		if (this.countsEnabled != null) {
			aggregator.setCountsEnabled(this.countsEnabled);
		}

		if (this.lockRegistry != null) {
			aggregator.setLockRegistry(this.lockRegistry);
		}

		if (this.messageStore != null) {
			aggregator.setMessageStore(this.messageStore);
		}

		if (this.correlationStrategy != null) {
			aggregator.setCorrelationStrategy(this.correlationStrategy);
		}

		if (this.releaseStrategy != null) {
			aggregator.setReleaseStrategy(this.releaseStrategy);
		}

		if (this.groupTimeoutExpression != null) {
			aggregator.setGroupTimeoutExpression(this.groupTimeoutExpression);
		}

		if (this.forceReleaseAdviceChain != null) {
			aggregator.setForceReleaseAdviceChain(this.forceReleaseAdviceChain);
		}

		if (this.taskScheduler != null) {
			aggregator.setTaskScheduler(this.taskScheduler);
		}

		if (this.discardChannel != null) {
			aggregator.setDiscardChannel(this.discardChannel);
		}

		if (this.discardChannelName != null) {
			aggregator.setDiscardChannelName(this.discardChannelName);
		}

		if (this.sendPartialResultOnExpiry != null) {
			aggregator.setSendPartialResultOnExpiry(this.sendPartialResultOnExpiry);
		}

		if (this.minimumTimeoutForEmptyGroups != null) {
			aggregator.setMinimumTimeoutForEmptyGroups(this.minimumTimeoutForEmptyGroups);
		}

		if (this.expireGroupsUponTimeout != null) {
			aggregator.setExpireGroupsUponTimeout(this.expireGroupsUponTimeout);
		}

		return aggregator;
	}

	@Override
	protected Class<? extends MessageHandler> getPreCreationHandlerType() {
		return AggregatingMessageHandler.class;
	}

}
