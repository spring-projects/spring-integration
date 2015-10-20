/*
 * Copyright 2015 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
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
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.scheduling.TaskScheduler;


/**
 * {@link FactoryBean} to create an {@link AggregatingMessageHandler}.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class AggregatorFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<AggregatingMessageHandler>
		implements ApplicationContextAware, BeanNameAware, ApplicationEventPublisherAware {

	private Object processorBean;

	private String methodName;

	private ApplicationContext applicationContext;

	private String beanName;

	private ApplicationEventPublisher applicationEventPublisher;

	private AggregatingMessageHandler aggregator;

	private Boolean expireGroupsUponCompletion;

	private Long sendTimeout;

	private String outputChannelName;

	private AbstractMessageHandlerMetrics metrics;

	private DestinationResolver<MessageChannel> channelResolver;

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

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

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

	public void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
		this.channelResolver = channelResolver;
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
			if (this.methodName == null) {
				outputProcessor = new MethodInvokingMessageGroupProcessor(this.processorBean);
			}
			else {
				outputProcessor = new MethodInvokingMessageGroupProcessor(this.processorBean, this.methodName);
			}
		}
		this.aggregator = new AggregatingMessageHandler(outputProcessor);

		if (this.applicationContext != null) {
			this.aggregator.setApplicationContext(this.applicationContext);
		}

		if (this.applicationEventPublisher != null) {
			this.aggregator.setApplicationEventPublisher(applicationEventPublisher);
		}

		if (this.beanName != null) {
			this.aggregator.setBeanName(this.beanName);
		}

		if (this.expireGroupsUponCompletion != null) {
			this.aggregator.setExpireGroupsUponCompletion(this.expireGroupsUponCompletion);
		}

		if (this.sendTimeout != null) {
			this.aggregator.setSendTimeout(this.sendTimeout);
		}

		if (this.outputChannelName != null) {
			this.aggregator.setOutputChannelName(this.outputChannelName);
		}

		if (this.metrics != null) {
			this.aggregator.configureMetrics(this.metrics);
		}

		if (this.channelResolver != null) {
			this.aggregator.setChannelResolver(this.channelResolver);
		}

		if (this.statsEnabled != null) {
			this.aggregator.setStatsEnabled(this.statsEnabled);
		}

		if (this.countsEnabled != null) {
			this.aggregator.setCountsEnabled(this.countsEnabled);
		}

		if (this.lockRegistry != null) {
			this.aggregator.setLockRegistry(this.lockRegistry);
		}

		if (this.messageStore != null) {
			this.aggregator.setMessageStore(this.messageStore);
		}

		if (this.correlationStrategy != null) {
			this.aggregator.setCorrelationStrategy(this.correlationStrategy);
		}

		if (this.releaseStrategy != null) {
			this.aggregator.setReleaseStrategy(this.releaseStrategy);
		}

		if (this.groupTimeoutExpression != null) {
			this.aggregator.setGroupTimeoutExpression(this.groupTimeoutExpression);
		}

		if (this.forceReleaseAdviceChain != null) {
			this.aggregator.setForceReleaseAdviceChain(this.forceReleaseAdviceChain);
		}

		if (this.taskScheduler != null) {
			this.aggregator.setTaskScheduler(this.taskScheduler);
		}

		if (this.discardChannel != null) {
			this.aggregator.setDiscardChannel(this.discardChannel);
		}

		if (this.discardChannelName != null) {
			this.aggregator.setDiscardChannelName(this.discardChannelName);
		}

		if (this.sendPartialResultOnExpiry != null) {
			this.aggregator.setSendPartialResultOnExpiry(this.sendPartialResultOnExpiry);
		}

		if (this.minimumTimeoutForEmptyGroups != null) {
			this.aggregator.setMinimumTimeoutForEmptyGroups(this.minimumTimeoutForEmptyGroups);
		}

		if (this.expireGroupsUponTimeout != null) {
			this.aggregator.setExpireGroupsUponTimeout(this.expireGroupsUponTimeout);
		}

		return this.aggregator;
	}

	@Override
	public Class<? extends MessageHandler> getObjectType() {
		return AggregatingMessageHandler.class;
	}

}
