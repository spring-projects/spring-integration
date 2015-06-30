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
import org.springframework.integration.handler.management.AbstractMessageHandlerMetrics;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.MessageChannel;
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

	private final AggregatingMessageHandler aggregator;

	public AggregatorFactoryBean(Object processor) {
		this(processor, null);
	}

	public AggregatorFactoryBean(Object processor, String methodName) {
		MessageGroupProcessor outputProcessor;
		if (processor instanceof MessageGroupProcessor) {
			outputProcessor = (MessageGroupProcessor) processor;
		}
		else {
			if (methodName == null) {
				outputProcessor = new MethodInvokingMessageGroupProcessor(processor);
			}
			else {
				outputProcessor = new MethodInvokingMessageGroupProcessor(processor, methodName);
			}
		}
		this.aggregator = new AggregatingMessageHandler(outputProcessor);
	}

	public void setExpireGroupsUponCompletion(boolean expireGroupsUponCompletion) {
		this.aggregator.setExpireGroupsUponCompletion(expireGroupsUponCompletion);
	}

	public void setSendTimeout(long sendTimeout) {
		this.aggregator.setSendTimeout(sendTimeout);
	}

	public void setOutputChannelName(String outputChannelName) {
		this.aggregator.setOutputChannelName(outputChannelName);
	}

	public void configureMetrics(AbstractMessageHandlerMetrics metrics) {
		this.aggregator.configureMetrics(metrics);
	}

	@Override
	public final void setBeanName(String beanName) {
		this.aggregator.setBeanName(beanName);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.aggregator.setApplicationContext(applicationContext);
	}

	public void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
		this.aggregator.setChannelResolver(channelResolver);
	}

	public void enableStats(boolean statsEnabled) {
		this.aggregator.enableStats(statsEnabled);
	}

	public void enableCounts(boolean countsEnabled) {
		this.aggregator.enableCounts(countsEnabled);
	}

	public void setLockRegistry(LockRegistry lockRegistry) {
		this.aggregator.setLockRegistry(lockRegistry);
	}

	public void setMessageStore(MessageGroupStore store) {
		this.aggregator.setMessageStore(store);
	}

	public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
		this.aggregator.setCorrelationStrategy(correlationStrategy);
	}

	public void setReleaseStrategy(ReleaseStrategy releaseStrategy) {
		this.aggregator.setReleaseStrategy(releaseStrategy);
	}

	public void setGroupTimeoutExpression(Expression groupTimeoutExpression) {
		this.aggregator.setGroupTimeoutExpression(groupTimeoutExpression);
	}

	public void setForceReleaseAdviceChain(List<Advice> forceReleaseAdviceChain) {
		this.aggregator.setForceReleaseAdviceChain(forceReleaseAdviceChain);
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.aggregator.setTaskScheduler(taskScheduler);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.aggregator.setApplicationEventPublisher(applicationEventPublisher);
	}

	public void setDiscardChannel(MessageChannel discardChannel) {
		this.aggregator.setDiscardChannel(discardChannel);
	}

	public void setDiscardChannelName(String discardChannelName) {
		this.aggregator.setDiscardChannelName(discardChannelName);
	}

	public void setSendPartialResultOnExpiry(boolean sendPartialResultOnExpiry) {
		this.aggregator.setSendPartialResultOnExpiry(sendPartialResultOnExpiry);
	}

	public void setMinimumTimeoutForEmptyGroups(long minimumTimeoutForEmptyGroups) {
		this.aggregator.setMinimumTimeoutForEmptyGroups(minimumTimeoutForEmptyGroups);
	}

	public void setReleasePartialSequences(boolean releasePartialSequences) {
		this.aggregator.setReleasePartialSequences(releasePartialSequences);
	}

	public void setExpireGroupsUponTimeout(boolean expireGroupsUponTimeout) {
		this.aggregator.setExpireGroupsUponTimeout(expireGroupsUponTimeout);
	}

	@Override
	protected AggregatingMessageHandler createHandler() {
		return this.aggregator;
	}

}
