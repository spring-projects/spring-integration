/*
 * Copyright 2015-2021 the original author or authors.
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.aopalliance.aop.Advice;
import org.jetbrains.annotations.Nullable;

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
import org.springframework.messaging.Message;
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

	private Long expireTimeout;

	private Long expireDuration;

	private Function<MessageGroup, Map<String, Object>> headersFunction;

	private BiFunction<Message<?>, String, String> groupConditionSupplier;

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

	public void setExpireTimeout(Long expireTimeout) {
		this.expireTimeout = expireTimeout;
	}

	public void setExpireDurationMillis(Long expireDuration) {
		this.expireDuration = expireDuration;
	}

	public void setGroupConditionSupplier(BiFunction<Message<?>, String, String> groupConditionSupplier) {
		this.groupConditionSupplier = groupConditionSupplier;
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
				.acceptIfNotNull(this.lockRegistry, aggregator::setLockRegistry)
				.acceptIfNotNull(this.messageStore, aggregator::setMessageStore)
				.acceptIfNotNull(obtainCorrelationStrategy(), aggregator::setCorrelationStrategy)
				.acceptIfNotNull(obtainReleaseStrategy(), aggregator::setReleaseStrategy)
				.acceptIfNotNull(this.groupTimeoutExpression, aggregator::setGroupTimeoutExpression)
				.acceptIfNotNull(this.forceReleaseAdviceChain, aggregator::setForceReleaseAdviceChain)
				.acceptIfNotNull(this.taskScheduler, aggregator::setTaskScheduler)
				.acceptIfNotNull(this.discardChannel, aggregator::setDiscardChannel)
				.acceptIfNotNull(this.discardChannelName, aggregator::setDiscardChannelName)
				.acceptIfNotNull(this.sendPartialResultOnExpiry, aggregator::setSendPartialResultOnExpiry)
				.acceptIfNotNull(this.minimumTimeoutForEmptyGroups, aggregator::setMinimumTimeoutForEmptyGroups)
				.acceptIfNotNull(this.expireGroupsUponTimeout, aggregator::setExpireGroupsUponTimeout)
				.acceptIfNotNull(this.popSequence, aggregator::setPopSequence)
				.acceptIfNotNull(this.releaseLockBeforeSend, aggregator::setReleaseLockBeforeSend)
				.acceptIfNotNull(this.expireDuration,
						(duration) -> aggregator.setExpireDuration(Duration.ofMillis(duration)))
				.acceptIfNotNull(this.groupConditionSupplier, aggregator::setGroupConditionSupplier)
				.acceptIfNotNull(this.expireTimeout, aggregator::setExpireTimeout);

		return aggregator;
	}

	@Nullable
	private CorrelationStrategy obtainCorrelationStrategy() {
		if (this.correlationStrategy == null && this.processorBean != null) {
			CorrelationStrategyFactoryBean correlationStrategyFactoryBean = new CorrelationStrategyFactoryBean();
			correlationStrategyFactoryBean.setTarget(this.processorBean);
			correlationStrategyFactoryBean.afterPropertiesSet();
			return correlationStrategyFactoryBean.getObject();
		}
		return this.correlationStrategy;
	}

	@Nullable
	private ReleaseStrategy obtainReleaseStrategy() {
		if (this.releaseStrategy == null && this.processorBean != null) {
			ReleaseStrategyFactoryBean releaseStrategyFactoryBean = new ReleaseStrategyFactoryBean();
			releaseStrategyFactoryBean.setTarget(this.processorBean);
			releaseStrategyFactoryBean.afterPropertiesSet();
			return releaseStrategyFactoryBean.getObject();
		}
		return this.releaseStrategy;
	}

	@Override
	protected Class<? extends MessageHandler> getPreCreationHandlerType() {
		return AggregatingMessageHandler.class;
	}

}
