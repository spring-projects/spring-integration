/*
 * Copyright 2016-2021 the original author or authors.
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

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.aopalliance.aop.Advice;

import org.springframework.integration.aggregator.AbstractCorrelatingMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingCorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingReleaseStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.config.CorrelationStrategyFactoryBean;
import org.springframework.integration.config.ReleaseStrategyFactoryBean;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandlerSpec} for an {@link AbstractCorrelatingMessageHandler}.
 *
 * @param <S> the target {@link CorrelationHandlerSpec} implementation type.
 * @param <H> the {@link AbstractCorrelatingMessageHandler} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public abstract class CorrelationHandlerSpec<S extends CorrelationHandlerSpec<S, H>,
		H extends AbstractCorrelatingMessageHandler>
		extends ConsumerEndpointSpec<S, H> {

	private final List<Advice> forceReleaseAdviceChain = new LinkedList<>();

	protected CorrelationHandlerSpec(H messageHandler) {
		super(messageHandler);
		messageHandler.setForceReleaseAdviceChain(this.forceReleaseAdviceChain);
	}

	/**
	 * @param messageStore the message group store.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setMessageStore(MessageGroupStore)
	 */
	public S messageStore(MessageGroupStore messageStore) {
		Assert.notNull(messageStore, "'messageStore' must not be null.");
		this.handler.setMessageStore(messageStore);
		return _this();
	}

	/**
	 * @param sendPartialResultOnExpiry the sendPartialResultOnExpiry.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setSendPartialResultOnExpiry(boolean)
	 */
	public S sendPartialResultOnExpiry(boolean sendPartialResultOnExpiry) {
		this.handler.setSendPartialResultOnExpiry(sendPartialResultOnExpiry);
		return _this();
	}

	/**
	 * @param minimumTimeoutForEmptyGroups the minimumTimeoutForEmptyGroups
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setMinimumTimeoutForEmptyGroups(long)
	 */
	public S minimumTimeoutForEmptyGroups(long minimumTimeoutForEmptyGroups) {
		this.handler.setMinimumTimeoutForEmptyGroups(minimumTimeoutForEmptyGroups);
		return _this();
	}

	/**
	 * Configure the handler with a group timeout expression that evaluates to
	 * this constant value.
	 * @param groupTimeout the group timeout in milliseconds.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setGroupTimeoutExpression
	 * @see ValueExpression
	 */
	public S groupTimeout(long groupTimeout) {
		this.handler.setGroupTimeoutExpression(new ValueExpression<>(groupTimeout));
		return _this();
	}

	/**
	 * Specify a SpEL expression to evaluate the group timeout for scheduled expiration.
	 * Must return {@link java.util.Date}, {@link java.lang.Long} or {@link String} as a long.
	 * @param groupTimeoutExpression the group timeout expression string.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setGroupTimeoutExpression
	 */
	public S groupTimeoutExpression(String groupTimeoutExpression) {
		Assert.hasText(groupTimeoutExpression, "'groupTimeoutExpression' must not be empty string.");
		this.handler.setGroupTimeoutExpression(PARSER.parseExpression(groupTimeoutExpression));
		return _this();
	}

	/**
	 * Configure the handler with a function that will be invoked to resolve the group timeout,
	 * based on the message group.
	 * Usually used with a JDK8 lambda:
	 * <p>{@code .groupTimeout(g -> g.size() * 2000L)}.
	 * Must return {@link java.util.Date}, {@link java.lang.Long} or {@link String} a long.
	 * @param groupTimeoutFunction a function invoked to resolve the group timeout in milliseconds.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setGroupTimeoutExpression
	 */
	public S groupTimeout(Function<MessageGroup, ?> groupTimeoutFunction) {
		this.handler.setGroupTimeoutExpression(new FunctionExpression<>(groupTimeoutFunction));
		return _this();
	}

	/**
	 * @param taskScheduler the task scheduler.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setTaskScheduler(TaskScheduler)
	 */
	@Override
	public S taskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "'taskScheduler' must not be null");
		super.taskScheduler(taskScheduler);
		this.handler.setTaskScheduler(taskScheduler);
		return _this();
	}

	/**
	 * @param discardChannel the discard channel.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setDiscardChannel(MessageChannel)
	 */
	public S discardChannel(MessageChannel discardChannel) {
		Assert.notNull(discardChannel, "'discardChannel' must not be null.");
		this.handler.setDiscardChannel(discardChannel);
		return _this();
	}

	/**
	 * @param discardChannelName the discard channel.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setDiscardChannelName(String)
	 */
	public S discardChannel(String discardChannelName) {
		Assert.hasText(discardChannelName, "'discardChannelName' must not be empty.");
		this.handler.setDiscardChannelName(discardChannelName);
		return _this();
	}

	/**
	 * Configure the handler with {@link org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy}
	 * and {@link org.springframework.integration.aggregator.MethodInvokingReleaseStrategy} using the target
	 * object which should have methods annotated appropriately for each function.
	 * @param target the target object
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setCorrelationStrategy(CorrelationStrategy)
	 * @see AbstractCorrelatingMessageHandler#setReleaseStrategy(ReleaseStrategy)
	 */
	public S processor(Object target) {
		try {
			CorrelationStrategyFactoryBean correlationStrategyFactoryBean = new CorrelationStrategyFactoryBean();
			correlationStrategyFactoryBean.setTarget(target);
			correlationStrategyFactoryBean.afterPropertiesSet();
			ReleaseStrategyFactoryBean releaseStrategyFactoryBean = new ReleaseStrategyFactoryBean();
			releaseStrategyFactoryBean.setTarget(target);
			releaseStrategyFactoryBean.afterPropertiesSet();
			return correlationStrategy(correlationStrategyFactoryBean.getObject())
					.releaseStrategy(releaseStrategyFactoryBean.getObject());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Configure the handler with an {@link ExpressionEvaluatingCorrelationStrategy}
	 * for the given expression.
	 * @param correlationExpression the correlation expression.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setCorrelationStrategy(CorrelationStrategy)
	 */
	public S correlationExpression(String correlationExpression) {
		return correlationStrategy(new ExpressionEvaluatingCorrelationStrategy(correlationExpression));
	}

	/**
	 * Configure the handler with an
	 * {@link org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy}
	 * for the target object and method name.
	 * @param target the target object.
	 * @param methodName the method name.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setCorrelationStrategy(CorrelationStrategy)
	 */
	public S correlationStrategy(Object target, String methodName) {
		try {
			CorrelationStrategyFactoryBean correlationStrategyFactoryBean = new CorrelationStrategyFactoryBean();
			correlationStrategyFactoryBean.setTarget(target);
			correlationStrategyFactoryBean.setMethodName(methodName);
			correlationStrategyFactoryBean.afterPropertiesSet();
			return correlationStrategy(correlationStrategyFactoryBean.getObject());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @param correlationStrategy the correlation strategy.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setCorrelationStrategy(CorrelationStrategy)
	 */
	public S correlationStrategy(CorrelationStrategy correlationStrategy) {
		this.handler.setCorrelationStrategy(correlationStrategy);
		return _this();
	}

	/**
	 * Configure the handler with an {@link ExpressionEvaluatingReleaseStrategy} for the
	 * given expression.
	 *
	 * @param releaseExpression the correlation expression.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setReleaseStrategy(ReleaseStrategy)
	 */
	public S releaseExpression(String releaseExpression) {
		return releaseStrategy(new ExpressionEvaluatingReleaseStrategy(releaseExpression));
	}

	/**
	 * Configure the handler with an
	 * {@link org.springframework.integration.aggregator.MethodInvokingReleaseStrategy}
	 * for the target object and method name.
	 * @param target the target object.
	 * @param methodName the method name.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setReleaseStrategy(ReleaseStrategy)
	 */
	public S releaseStrategy(Object target, String methodName) {
		try {
			ReleaseStrategyFactoryBean releaseStrategyFactoryBean = new ReleaseStrategyFactoryBean();
			releaseStrategyFactoryBean.setTarget(target);
			releaseStrategyFactoryBean.setMethodName(methodName);
			releaseStrategyFactoryBean.afterPropertiesSet();
			return releaseStrategy(releaseStrategyFactoryBean.getObject());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @param releaseStrategy the release strategy.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setReleaseStrategy(ReleaseStrategy)
	 */
	public S releaseStrategy(ReleaseStrategy releaseStrategy) {
		this.handler.setReleaseStrategy(releaseStrategy);
		return _this();
	}

	/**
	 * Expire (completely remove) a group if it is completed due to timeout.
	 * Default {@code true} for aggregator and {@code false} for resequencer.
	 * @param expireGroupsUponTimeout the expireGroupsUponTimeout to set
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setExpireGroupsUponTimeout
	 */
	public S expireGroupsUponTimeout(boolean expireGroupsUponTimeout) {
		this.handler.setExpireGroupsUponTimeout(expireGroupsUponTimeout);
		return _this();
	}

	/**
	 * Configure a list of {@link Advice} objects to be applied to the
	 * {@code forceComplete()} operation.
	 * @param advice the advice chain.
	 * @return the endpoint spec.
	 */
	public S forceReleaseAdvice(Advice... advice) {
		this.forceReleaseAdviceChain.addAll(Arrays.asList(advice));
		return _this();
	}

	/**
	 * Used to obtain a {@code Lock} based on the {@code groupId} for concurrent operations
	 * on the {@code MessageGroup}.
	 * By default, an internal {@code DefaultLockRegistry} is used.
	 * Use of a distributed {@link LockRegistry}, such as the {@code RedisLockRegistry},
	 * ensures only one instance of the aggregator will operate on a group concurrently.
	 * @param lockRegistry the {@link LockRegistry} to use.
	 * @return the endpoint spec.
	 */
	public S lockRegistry(LockRegistry lockRegistry) {
		Assert.notNull(lockRegistry, "'lockRegistry' must not be null.");
		this.handler.setLockRegistry(lockRegistry);
		return _this();
	}

	/**
	 * Perform a
	 * {@link org.springframework.integration.support.MessageBuilder#popSequenceDetails()}
	 * for output message or not.
	 * @param popSequence the boolean flag to use.
	 * @return the endpoint spec.
	 * @since 5.1
	 * @see AbstractCorrelatingMessageHandler#setPopSequence(boolean)
	 */
	public S popSequence(boolean popSequence) {
		this.handler.setPopSequence(popSequence);
		return _this();
	}

	/**
	 * Configure a timeout for old groups in the store to purge.
	 * @param expireTimeout the timeout in milliseconds to use.
	 * @return the endpoint spec.
	 * @since 5.4
	 * @see AbstractCorrelatingMessageHandler#setExpireTimeout(long)
	 * @deprecated since 5.5 in favor of {@link #expireTimeout(long)}
	 */
	@Deprecated
	public S setExpireTimeout(long expireTimeout) {
		return expireTimeout(expireTimeout);
	}

	/**
	 * Configure a timeout for old groups in the store to purge.
	 * @param expireTimeout the timeout in milliseconds to use.
	 * @return the endpoint spec.
	 * @since 5.5
	 * @see AbstractCorrelatingMessageHandler#setExpireTimeout(long)
	 */
	public S expireTimeout(long expireTimeout) {
		this.handler.setExpireTimeout(expireTimeout);
		return _this();
	}

	/**
	 * Configure a {@link Duration} how often to run a scheduled purge task.
	 * @param expireDuration the duration for scheduled purge task.
	 * @return the endpoint spec.
	 * @since 5.4
	 * @see AbstractCorrelatingMessageHandler#setExpireDuration(Duration)
	 * @deprecated since 5.5 in favor of {@link #expireDuration(Duration)}
	 */
	@Deprecated
	public S setExpireDuration(Duration expireDuration) {
		return expireDuration(expireDuration);
	}

	/**
	 * Configure a {@link Duration} how often to run a scheduled purge task.
	 * @param expireDuration the duration for scheduled purge task.
	 * @return the endpoint spec.
	 * @since 5.5
	 * @see AbstractCorrelatingMessageHandler#setExpireDuration(Duration)
	 */
	public S expireDuration(Duration expireDuration) {
		this.handler.setExpireDuration(expireDuration);
		return _this();
	}

	/**
	 * Set to true to release the message group lock before sending any output. See
	 * "Avoiding Deadlocks" in the Aggregator section of the reference manual for more
	 * information as to why this might be needed.
	 * @param releaseLockBeforeSend true to release the lock.
	 * @return the endpoint spec.
	 * @since 5.5
	 * @see AbstractCorrelatingMessageHandler#setReleaseLockBeforeSend(boolean)
	 */
	public S releaseLockBeforeSend(boolean releaseLockBeforeSend) {
		this.handler.setReleaseLockBeforeSend(releaseLockBeforeSend);
		return _this();
	}

	/**
	 * Configure a {@link BiFunction} to supply a group condition from a message to be added to the group.
	 * The {@code null} result from the function will reset a condition set before.
	 * @param conditionSupplier the function to supply a group condition from a message to be added to the group.
	 * @return the endpoint spec.
	 * @since 5.5
	 */
	public S groupConditionSupplier(BiFunction<Message<?>, String, String> conditionSupplier) {
		this.handler.setGroupConditionSupplier(conditionSupplier);
		return _this();
	}

}
