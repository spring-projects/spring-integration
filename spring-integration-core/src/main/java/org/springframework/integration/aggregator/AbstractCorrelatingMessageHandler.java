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

package org.springframework.integration.aggregator;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.function.BiFunction;

import org.aopalliance.aop.Advice;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.Lifecycle;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.DiscardingMessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.store.UniqueExpiryCallback;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Abstract Message handler that holds a buffer of correlated messages in a
 * {@link org.springframework.integration.store.MessageStore}.
 * This class takes care of correlated groups of messages
 * that can be completed in batches. It is useful for custom implementation of
 * MessageHandlers that require correlation and is used as a base class for Aggregator -
 * {@link AggregatingMessageHandler} and Resequencer - {@link ResequencingMessageHandler},
 * or custom implementations requiring correlation.
 * <p>
 * To customize this handler inject {@link CorrelationStrategy},
 * {@link ReleaseStrategy}, and {@link MessageGroupProcessor} implementations as
 * you require.
 * <p>
 * By default the {@link CorrelationStrategy} will be a
 * {@link HeaderAttributeCorrelationStrategy} and the {@link ReleaseStrategy} will be a
 * {@link SequenceSizeReleaseStrategy}.
 * <p>
 * Use proper {@link CorrelationStrategy} for cases when same
 * {@link org.springframework.integration.store.MessageStore} is used
 * for multiple handlers to ensure uniqueness of message groups across handlers.
 * <p>
 * When the {@link #expireTimeout} is greater than 0, groups which are older than this timeout
 * are purged from the store on start up (or when {@link #purgeOrphanedGroups()} is called).
 * If {@link #expireDuration} is provided, the task is scheduled to perform
 * {@link #purgeOrphanedGroups()} periodically.
 *
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author David Liu
 * @author Enrique Rodriguez
 * @author Meherzad Lahewala
 * @author Jayadev Sirimamilla
 *
 * @since 2.0
 */
public abstract class AbstractCorrelatingMessageHandler extends AbstractMessageProducingHandler
		implements DiscardingMessageHandler, ApplicationEventPublisherAware, ManageableLifecycle {

	private final Comparator<Message<?>> sequenceNumberComparator = new MessageSequenceComparator();

	private final Map<UUID, ScheduledFuture<?>> expireGroupScheduledFutures = new ConcurrentHashMap<>();

	private MessageGroupProcessor outputProcessor;

	private MessageGroupStore messageStore;

	private CorrelationStrategy correlationStrategy;

	private ReleaseStrategy releaseStrategy;

	private boolean releaseStrategySet;

	private MessageChannel discardChannel;

	private String discardChannelName;

	private boolean sendPartialResultOnExpiry;

	private boolean sequenceAware;

	private LockRegistry lockRegistry = new DefaultLockRegistry();

	private boolean lockRegistrySet = false;

	private long minimumTimeoutForEmptyGroups;

	private boolean releasePartialSequences;

	private Expression groupTimeoutExpression;

	private List<Advice> forceReleaseAdviceChain;

	private long expireTimeout;

	private Duration expireDuration;

	private MessageGroupProcessor forceReleaseProcessor = new ForceReleaseMessageGroupProcessor();

	private EvaluationContext evaluationContext;

	private ApplicationEventPublisher applicationEventPublisher;

	private boolean expireGroupsUponTimeout = true;

	private boolean popSequence = true;

	private boolean releaseLockBeforeSend;

	private volatile boolean running;

	private BiFunction<Message<?>, String, String> groupConditionSupplier;

	public AbstractCorrelatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store,
			CorrelationStrategy correlationStrategy, ReleaseStrategy releaseStrategy) {

		Assert.notNull(processor, "'processor' must not be null");
		Assert.notNull(store, "'store' must not be null");

		setMessageStore(store);
		this.outputProcessor = processor;

		this.correlationStrategy =
				correlationStrategy == null
						? new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID)
						: correlationStrategy;

		this.releaseStrategy =
				releaseStrategy == null
						? new SimpleSequenceSizeReleaseStrategy()
						: releaseStrategy;

		this.releaseStrategySet = releaseStrategy != null;
		this.sequenceAware = this.releaseStrategy instanceof SequenceSizeReleaseStrategy;
	}

	public AbstractCorrelatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store) {
		this(processor, store, null, null);
	}

	public AbstractCorrelatingMessageHandler(MessageGroupProcessor processor) {
		this(processor, new SimpleMessageStore(0), null, null);
	}

	public void setLockRegistry(LockRegistry lockRegistry) {
		Assert.isTrue(!this.lockRegistrySet, "'this.lockRegistry' can not be reset once its been set");
		Assert.notNull(lockRegistry, "'lockRegistry' must not be null");
		this.lockRegistry = lockRegistry;
		this.lockRegistrySet = true;
	}

	public final void setMessageStore(MessageGroupStore store) {
		this.messageStore = store;
		UniqueExpiryCallback expiryCallback =
				(messageGroupStore, group) -> this.forceReleaseProcessor.processMessageGroup(group);
		store.registerMessageGroupExpiryCallback(expiryCallback);
	}

	public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
		Assert.notNull(correlationStrategy, "'correlationStrategy' must not be null");
		this.correlationStrategy = correlationStrategy;
	}

	public void setReleaseStrategy(ReleaseStrategy releaseStrategy) {
		Assert.notNull(releaseStrategy, "'releaseStrategy' must not be null");
		this.releaseStrategy = releaseStrategy;
		this.sequenceAware = this.releaseStrategy instanceof SequenceSizeReleaseStrategy;
		this.releaseStrategySet = true;
	}

	public void setGroupTimeoutExpression(Expression groupTimeoutExpression) {
		this.groupTimeoutExpression = groupTimeoutExpression;
	}

	public void setForceReleaseAdviceChain(List<Advice> forceReleaseAdviceChain) {
		Assert.notNull(forceReleaseAdviceChain, "'forceReleaseAdviceChain' must not be null");
		this.forceReleaseAdviceChain = forceReleaseAdviceChain;
	}

	/**
	 * Specify a {@link MessageGroupProcessor} for the output function.
	 * @param outputProcessor the {@link MessageGroupProcessor} to use
	 * @since 5.0
	 */
	public void setOutputProcessor(MessageGroupProcessor outputProcessor) {
		Assert.notNull(outputProcessor, "'processor' must not be null");
		this.outputProcessor = outputProcessor;
	}

	/**
	 * Return a configured {@link MessageGroupProcessor}.
	 * @return the configured {@link MessageGroupProcessor}
	 * @since 5.2
	 */
	public MessageGroupProcessor getOutputProcessor() {
		return this.outputProcessor;
	}

	public void setDiscardChannel(MessageChannel discardChannel) {
		Assert.notNull(discardChannel, "'discardChannel' cannot be null");
		this.discardChannel = discardChannel;
	}

	public void setDiscardChannelName(String discardChannelName) {
		Assert.hasText(discardChannelName, "'discardChannelName' must not be empty");
		this.discardChannelName = discardChannelName;
	}


	public void setSendPartialResultOnExpiry(boolean sendPartialResultOnExpiry) {
		this.sendPartialResultOnExpiry = sendPartialResultOnExpiry;
	}

	/**
	 * By default, when a MessageGroupStoreReaper is configured to expire partial
	 * groups, empty groups are also removed. Empty groups exist after a group
	 * is released normally. This is to enable the detection and discarding of
	 * late-arriving messages. If you wish to expire empty groups on a longer
	 * schedule than expiring partial groups, set this property. Empty groups will
	 * then not be removed from the MessageStore until they have not been modified
	 * for at least this number of milliseconds.
	 * @param minimumTimeoutForEmptyGroups The minimum timeout.
	 */
	public void setMinimumTimeoutForEmptyGroups(long minimumTimeoutForEmptyGroups) {
		this.minimumTimeoutForEmptyGroups = minimumTimeoutForEmptyGroups;
	}

	/**
	 * Set {@code releasePartialSequences} on an underlying default
	 * {@link SequenceSizeReleaseStrategy}. Ignored for other release strategies.
	 * @param releasePartialSequences true to allow release.
	 */
	public void setReleasePartialSequences(boolean releasePartialSequences) {
		if (!this.releaseStrategySet && releasePartialSequences) {
			setReleaseStrategy(new SequenceSizeReleaseStrategy());
		}
		this.releasePartialSequences = releasePartialSequences;
	}

	/**
	 * Expire (completely remove) a group if it is completed due to timeout.
	 * Default true
	 * @param expireGroupsUponTimeout the expireGroupsUponTimeout to set
	 * @since 4.1
	 */
	public void setExpireGroupsUponTimeout(boolean expireGroupsUponTimeout) {
		this.expireGroupsUponTimeout = expireGroupsUponTimeout;
	}

	/**
	 * Perform a
	 * {@link org.springframework.integration.support.MessageBuilder#popSequenceDetails()}
	 * for output message or not. Default to true. This option removes the sequence
	 * information added by the nearest upstream component with {@code applySequence=true}
	 * (for example splitter).
	 * @param popSequence the boolean flag to use.
	 * @since 5.1
	 */
	public void setPopSequence(boolean popSequence) {
		this.popSequence = popSequence;
	}

	protected boolean isReleaseLockBeforeSend() {
		return this.releaseLockBeforeSend;
	}

	/**
	 * Set to true to release the message group lock before sending any output. See
	 * "Avoiding Deadlocks" in the Aggregator section of the reference manual for more
	 * information as to why this might be needed.
	 * @param releaseLockBeforeSend true to release the lock.
	 * @since 5.1.1
	 */
	public void setReleaseLockBeforeSend(boolean releaseLockBeforeSend) {
		this.releaseLockBeforeSend = releaseLockBeforeSend;
	}

	/**
	 * Configure a timeout in milliseconds for purging old orphaned groups from the store.
	 * Used on startup and when an {@link #expireDuration} is provided, the task for running
	 * {@link #purgeOrphanedGroups()} is scheduled with that period.
	 * The {@link #forceReleaseProcessor} is used to process those expired groups according
	 * the "force complete" options. A group can be orphaned if a persistent message group
	 * store is used and no new messages arrive for that group after a restart.
	 * @param expireTimeout the number of milliseconds to determine old orphaned groups in the store to purge.
	 * @since 5.4
	 * @see #purgeOrphanedGroups()
	 */
	public void setExpireTimeout(long expireTimeout) {
		Assert.isTrue(expireTimeout > 0, "'expireTimeout' must be more than 0.");
		this.expireTimeout = expireTimeout;
	}

	/**
	 * Configure a {@link Duration} (in millis) how often to clean up old orphaned groups from the store.
	 * @param expireDuration the delay how often to call {@link #purgeOrphanedGroups()}.
	 * @since 5.4
	 * @see #purgeOrphanedGroups()
	 * @see #setExpireDuration(Duration)
	 * @see #setExpireTimeout(long)
	 */
	public void setExpireDurationMillis(long expireDuration) {
		setExpireDuration(Duration.ofMillis(expireDuration));
	}

	/**
	 * Configure a {@link Duration} how often to clean up old orphaned groups from the store.
	 * @param expireDuration the delay how often to call {@link #purgeOrphanedGroups()}.
	 * @since 5.4
	 * @see #purgeOrphanedGroups()
	 * @see #setExpireTimeout(long)
	 */
	public void setExpireDuration(@Nullable Duration expireDuration) {
		this.expireDuration = expireDuration;
	}

	/**
	 * Configure a {@link BiFunction} to supply a group condition from a message to be added to the group.
	 * The {@code null} result from the function will reset a condition set before.
	 * @param conditionSupplier the function to supply a group condition from a message to be added to the group.
	 * @since 5.5
	 * @see GroupConditionProvider
	 */
	public void setGroupConditionSupplier(BiFunction<Message<?>, String, String> conditionSupplier) {
		this.groupConditionSupplier = conditionSupplier;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.state(!(this.discardChannelName != null && this.discardChannel != null),
				"'discardChannelName' and 'discardChannel' are mutually exclusive.");
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			if (this.outputProcessor instanceof BeanFactoryAware) {
				((BeanFactoryAware) this.outputProcessor).setBeanFactory(beanFactory);
			}
			if (this.correlationStrategy instanceof BeanFactoryAware) {
				((BeanFactoryAware) this.correlationStrategy).setBeanFactory(beanFactory);
			}
			if (this.releaseStrategy instanceof BeanFactoryAware) {
				((BeanFactoryAware) this.releaseStrategy).setBeanFactory(beanFactory);
			}
		}

		if (this.releasePartialSequences) {
			Assert.isInstanceOf(SequenceSizeReleaseStrategy.class, this.releaseStrategy, () ->
					"Release strategy of type [" + this.releaseStrategy.getClass().getSimpleName() +
							"] cannot release partial sequences. Use a SequenceSizeReleaseStrategy instead.");
			((SequenceSizeReleaseStrategy) this.releaseStrategy)
					.setReleasePartialSequences(this.releasePartialSequences);
		}

		if (this.evaluationContext == null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		}

		if (this.sequenceAware) {
			this.logger.warn("Using a SequenceSizeReleaseStrategy with large groups may not perform well, consider "
					+ "using a SimpleSequenceSizeReleaseStrategy");
		}

		/*
		 * Disallow any further changes to the lock registry
		 * (checked in the setter).
		 */
		this.lockRegistrySet = true;
		this.forceReleaseProcessor = createGroupTimeoutProcessor();

		if (this.releaseStrategy instanceof GroupConditionProvider) {
			this.groupConditionSupplier = ((GroupConditionProvider) this.releaseStrategy).getGroupConditionSupplier();
		}
	}

	private MessageGroupProcessor createGroupTimeoutProcessor() {
		MessageGroupProcessor processor = new ForceReleaseMessageGroupProcessor();

		if (this.groupTimeoutExpression != null && !CollectionUtils.isEmpty(this.forceReleaseAdviceChain)) {
			ProxyFactory proxyFactory = new ProxyFactory(processor);
			this.forceReleaseAdviceChain.forEach(proxyFactory::addAdvice);
			return (MessageGroupProcessor) proxyFactory.getProxy(getApplicationContext().getClassLoader());
		}
		return processor;
	}

	@Override
	public String getComponentType() {
		return "aggregator";
	}

	public MessageGroupStore getMessageStore() {
		return this.messageStore;
	}

	protected Map<UUID, ScheduledFuture<?>> getExpireGroupScheduledFutures() {
		return this.expireGroupScheduledFutures;
	}

	protected CorrelationStrategy getCorrelationStrategy() {
		return this.correlationStrategy;
	}

	protected ReleaseStrategy getReleaseStrategy() {
		return this.releaseStrategy;
	}

	@Nullable
	protected BiFunction<Message<?>, String, String> getGroupConditionSupplier() {
		return this.groupConditionSupplier;
	}

	@Override
	public MessageChannel getDiscardChannel() {
		String channelName = this.discardChannelName;
		if (channelName == null && this.discardChannel == null) {
			channelName = IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME;
		}
		if (channelName != null) {
			try {
				this.discardChannel = getChannelResolver().resolveDestination(channelName);
			}
			catch (DestinationResolutionException ex) {
				if (channelName.equals(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME)) {
					this.discardChannel = new NullChannel();
				}
				else {
					throw ex;
				}
			}
			this.discardChannelName = null;
		}
		return this.discardChannel;
	}

	protected String getDiscardChannelName() {
		return this.discardChannelName;
	}

	protected boolean isSendPartialResultOnExpiry() {
		return this.sendPartialResultOnExpiry;
	}

	protected boolean isSequenceAware() {
		return this.sequenceAware;
	}

	protected LockRegistry getLockRegistry() {
		return this.lockRegistry;
	}

	protected boolean isLockRegistrySet() {
		return this.lockRegistrySet;
	}

	protected long getMinimumTimeoutForEmptyGroups() {
		return this.minimumTimeoutForEmptyGroups;
	}

	protected boolean isReleasePartialSequences() {
		return this.releasePartialSequences;
	}

	protected Expression getGroupTimeoutExpression() {
		return this.groupTimeoutExpression;
	}

	protected EvaluationContext getEvaluationContext() {
		return this.evaluationContext;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Object correlationKey = this.correlationStrategy.getCorrelationKey(message);
		Assert.state(correlationKey != null,
				"Null correlation not allowed.  Maybe the CorrelationStrategy is failing?");

		this.logger.debug(() -> "Handling message with correlationKey [" + correlationKey + "]: " + message);

		UUID groupIdUuid = UUIDConverter.getUUID(correlationKey);
		Lock lock = this.lockRegistry.obtain(groupIdUuid.toString());

		boolean noOutput = true;
		try {
			lock.lockInterruptibly();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessageHandlingException(message, "Interrupted getting lock in the [" + this + ']', e);
		}
		try {
			noOutput = processMessageForGroup(message, correlationKey, groupIdUuid, lock);
		}
		finally {
			if (noOutput || !this.releaseLockBeforeSend) {
				lock.unlock();
			}
		}
	}

	private boolean processMessageForGroup(Message<?> message, Object correlationKey, UUID groupIdUuid, Lock lock) {
		boolean noOutput = true;
		cancelScheduledFutureIfAny(correlationKey, groupIdUuid, true);
		MessageGroup messageGroup = this.messageStore.getMessageGroup(correlationKey);
		if (this.sequenceAware) {
			messageGroup = new SequenceAwareMessageGroup(messageGroup);
		}

		if (!messageGroup.isComplete() && messageGroup.canAdd(message)) {
			MessageGroup messageGroupToLog = messageGroup;
			this.logger.trace(() -> "Adding message to group [ " + messageGroupToLog + "]");
			messageGroup = store(correlationKey, message);

			setGroupConditionIfAny(message, messageGroup);

			if (this.releaseStrategy.canRelease(messageGroup)) {
				Collection<Message<?>> completedMessages = null;
				try {
					noOutput = false;
					completedMessages = completeGroup(message, correlationKey, messageGroup, lock);
				}
				finally {
					// Possible clean (implementation dependency) up
					// even if there was an exception processing messages
					afterRelease(messageGroup, completedMessages);
				}
				if (!isExpireGroupsUponCompletion() && this.minimumTimeoutForEmptyGroups > 0) {
					removeEmptyGroupAfterTimeout(messageGroup, this.minimumTimeoutForEmptyGroups);
				}
			}
			else {
				scheduleGroupToForceComplete(messageGroup);
			}
		}
		else {
			noOutput = false;
			discardMessage(message, lock);
		}
		return noOutput;
	}

	private void cancelScheduledFutureIfAny(Object correlationKey, UUID groupIdUuid, boolean mayInterruptIfRunning) {
		ScheduledFuture<?> scheduledFuture = this.expireGroupScheduledFutures.remove(groupIdUuid);
		if (scheduledFuture != null) {
			boolean canceled = scheduledFuture.cancel(mayInterruptIfRunning);
			if (canceled) {
				this.logger.debug(() ->
						"Cancel 'ScheduledFuture' for MessageGroup with Correlation Key [ " + correlationKey + "].");
			}
		}
	}

	private void setGroupConditionIfAny(Message<?> message, MessageGroup messageGroup) {
		if (this.groupConditionSupplier != null) {
			String condition = this.groupConditionSupplier.apply(message, messageGroup.getCondition());
			this.messageStore.setGroupCondition(messageGroup.getGroupId(), condition);
			messageGroup.setCondition(condition);
		}
	}

	protected boolean isExpireGroupsUponCompletion() {
		return false;
	}

	private void removeEmptyGroupAfterTimeout(MessageGroup messageGroup, long timeout) {
		Object groupId = messageGroup.getGroupId();
		UUID groupUuid = UUIDConverter.getUUID(groupId);
		ScheduledFuture<?> scheduledFuture =
				getTaskScheduler()
						.schedule(() -> {
							Lock lock = this.lockRegistry.obtain(groupUuid.toString());

							try {
								lock.lockInterruptibly();
								try {
									this.expireGroupScheduledFutures.remove(groupUuid);
									/*
									 * Obtain a fresh state for group from the MessageStore,
									 * since it could be changed while we have waited for lock.
									 */
									MessageGroup groupNow = this.messageStore.getMessageGroup(groupUuid);
									boolean removeGroup = groupNow.size() == 0 &&
											groupNow.getLastModified()
													<= (System.currentTimeMillis() - this.minimumTimeoutForEmptyGroups);
									if (removeGroup) {
										this.logger.debug(() -> "Removing empty group: " + groupUuid);
										remove(messageGroup);
									}
								}
								finally {
									lock.unlock();
								}
							}
							catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								this.logger.debug(() -> "Thread was interrupted while trying to obtain lock."
										+ "Rescheduling empty MessageGroup [ " + groupId + "] for removal.");
								removeEmptyGroupAfterTimeout(messageGroup, timeout);
							}

						}, new Date(System.currentTimeMillis() + timeout));

		this.logger.debug(() -> "Schedule empty MessageGroup [ " + groupId + "] for removal.");
		this.expireGroupScheduledFutures.put(groupUuid, scheduledFuture);
	}

	private void scheduleGroupToForceComplete(MessageGroup messageGroup) {
		Object groupTimeout = obtainGroupTimeout(messageGroup);
		/*
		 * When 'groupTimeout' is evaluated to 'null' we do nothing.
		 * The 'MessageGroupStoreReaper' can be used to 'forceComplete' message groups.
		 */
		if (groupTimeout != null) {
			Date startTime = null;
			if (groupTimeout instanceof Date) {
				startTime = (Date) groupTimeout;
			}
			else if ((Long) groupTimeout > 0) {
				startTime = new Date(System.currentTimeMillis() + (Long) groupTimeout);
			}

			if (startTime != null) {
				Object groupId = messageGroup.getGroupId();
				long timestamp = messageGroup.getTimestamp();
				long lastModified = messageGroup.getLastModified();
				ScheduledFuture<?> scheduledFuture =
						getTaskScheduler()
								.schedule(() -> {
									try {
										processForceRelease(groupId, timestamp, lastModified);
									}
									catch (MessageDeliveryException ex) {
											logger.warn(ex, () ->
													"The MessageGroup [" + groupId +
															"] is rescheduled by the reason of: ");
										scheduleGroupToForceComplete(groupId);
									}
								}, startTime);

				this.logger.debug(() -> "Schedule MessageGroup [ " + messageGroup + "] to 'forceComplete'.");
				this.expireGroupScheduledFutures.put(UUIDConverter.getUUID(groupId), scheduledFuture);
			}
			else {
				this.forceReleaseProcessor.processMessageGroup(messageGroup);
			}
		}
	}

	private void scheduleGroupToForceComplete(Object groupId) {
		MessageGroup messageGroup = this.messageStore.getMessageGroup(groupId);
		scheduleGroupToForceComplete(messageGroup);
	}

	private void processForceRelease(Object groupId, long timestamp, long lastModified) {
		MessageGroup messageGroup = this.messageStore.getMessageGroup(groupId);
		if (messageGroup.getTimestamp() == timestamp && messageGroup.getLastModified() == lastModified) {
			this.forceReleaseProcessor.processMessageGroup(messageGroup);
		}
	}

	private void discardMessage(Message<?> message, Lock lock) {
		if (this.releaseLockBeforeSend) {
			lock.unlock();
		}
		discardMessage(message);
	}

	private void discardMessage(Message<?> message) {
		MessageChannel messageChannel = getDiscardChannel();
		if (messageChannel != null) {
			this.messagingTemplate.send(messageChannel, message);
		}
	}

	/**
	 * Allows you to provide additional logic that needs to be performed after the MessageGroup was released.
	 * @param group The group.
	 * @param completedMessages The completed messages.
	 */
	protected abstract void afterRelease(MessageGroup group, Collection<Message<?>> completedMessages);

	/**
	 * Subclasses may override if special action is needed because the group was released or discarded
	 * due to a timeout. By default, {@link #afterRelease(MessageGroup, Collection)} is invoked.
	 * @param group The group.
	 * @param completedMessages The completed messages.
	 * @param timeout True if the release/discard was due to a timeout.
	 */
	protected void afterRelease(MessageGroup group, Collection<Message<?>> completedMessages, boolean timeout) {
		afterRelease(group, completedMessages);
	}

	protected void forceComplete(MessageGroup group) { // NOSONAR Complexity
		Object correlationKey = group.getGroupId();
		// UUIDConverter is no-op if already converted
		UUID groupId = UUIDConverter.getUUID(correlationKey);
		Lock lock = this.lockRegistry.obtain(groupId.toString());
		boolean removeGroup = true;
		boolean noOutput = true;
		try {
			lock.lockInterruptibly();
			try {
				cancelScheduledFutureIfAny(correlationKey, groupId, false);
				MessageGroup groupNow = group;
				/*
				 * If the group argument is not already complete,
				 * re-fetch it because it might have changed while we were waiting on
				 * its lock. If the last modified timestamp changed, defer the completion
				 * because the selection condition may have changed such that the group
				 * would no longer be eligible. If the timestamp changed, it's a completely new
				 * group and should not be reaped on this cycle.
				 *
				 * If the group argument is already complete, do not re-fetch.
				 * Note: not all message stores provide a direct reference to its internal
				 * group so the initial 'isComplete()` will only return true for those stores if
				 * the group was already complete at the time of its selection as a candidate.
				 *
				 * If the group is marked complete, only consider it
				 * for reaping if it's empty (and both timestamps are unaltered).
				 */
				if (!group.isComplete()) {
					groupNow = this.messageStore.getMessageGroup(correlationKey);
				}
				long lastModifiedNow = groupNow.getLastModified();
				int groupSize = groupNow.size();

				if ((!groupNow.isComplete() || groupSize == 0)
						&& group.getLastModified() == lastModifiedNow
						&& group.getTimestamp() == groupNow.getTimestamp()) {

					if (groupSize > 0) {
						noOutput = false;
						if (this.releaseStrategy.canRelease(groupNow)) {
							completeGroup(correlationKey, groupNow, lock);
						}
						else {
							expireGroup(correlationKey, groupNow, lock);
						}
						if (!this.expireGroupsUponTimeout) {
							afterRelease(groupNow, groupNow.getMessages(), true);
							removeGroup = false;
						}
					}
					else {
						/*
						 * By default empty groups are removed on the same schedule as non-empty
						 * groups. A longer timeout for empty groups can be enabled by
						 * setting minimumTimeoutForEmptyGroups.
						 */
						removeGroup =
								lastModifiedNow <= (System.currentTimeMillis() - this.minimumTimeoutForEmptyGroups);
						if (removeGroup) {
							this.logger.debug(() -> "Removing empty group: " + correlationKey);
						}
					}
				}
				else {
					removeGroup = false;
					this.logger.debug(() -> "Group expiry candidate (" + correlationKey +
							") has changed - it may be reconsidered for a future expiration");
				}
			}
			catch (MessageDeliveryException e) {
				removeGroup = false;
				this.logger.debug(() -> "Group expiry candidate (" + correlationKey +
						") has been affected by MessageDeliveryException - " +
						"it may be reconsidered for a future expiration one more time");
				throw e;
			}
			finally {
				try {
					if (removeGroup) {
						remove(group);
					}
				}
				finally {
					if (noOutput || !this.releaseLockBeforeSend) {
						lock.unlock();
					}
				}
			}
		}
		catch (@SuppressWarnings("unused") InterruptedException ie) {
			Thread.currentThread().interrupt();
			this.logger.debug("Thread was interrupted while trying to obtain lock");
		}
	}

	protected void remove(MessageGroup group) {
		Object correlationKey = group.getGroupId();
		this.messageStore.removeMessageGroup(correlationKey);
	}

	protected int findLastReleasedSequenceNumber(@SuppressWarnings("unused") Object groupId,
			Collection<Message<?>> partialSequence) {

		Message<?> lastReleasedMessage = Collections.max(partialSequence, this.sequenceNumberComparator);
		return StaticMessageHeaderAccessor.getSequenceNumber(lastReleasedMessage);
	}

	protected MessageGroup store(Object correlationKey, Message<?> message) {
		return this.messageStore.addMessageToGroup(correlationKey, message);
	}

	protected void expireGroup(Object correlationKey, MessageGroup group, Lock lock) {
		this.logger.info(() -> "Expiring MessageGroup with correlationKey[" + correlationKey + "]");
		if (this.sendPartialResultOnExpiry) {
			this.logger.debug(() -> "Prematurely releasing partially complete group with key ["
					+ correlationKey + "] to: " + getOutputChannel());
			completeGroup(correlationKey, group, lock);
		}
		else {
			this.logger.debug(() -> "Discarding messages of partially complete group with key ["
					+ correlationKey + "] to: "
					+ (this.discardChannelName != null ? this.discardChannelName : this.discardChannel));
			if (this.releaseLockBeforeSend) {
				lock.unlock();
			}
			group.getMessages()
					.forEach(this::discardMessage);
		}
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(
					new MessageGroupExpiredEvent(this, correlationKey, group.size(),
							new Date(group.getLastModified()), new Date(), !this.sendPartialResultOnExpiry));
		}
	}

	protected void completeGroup(Object correlationKey, MessageGroup group, Lock lock) {
		Message<?> first = null;
		if (group != null) {
			first = group.getOne();
		}
		completeGroup(first, correlationKey, group, lock);
	}

	@SuppressWarnings("unchecked")
	protected Collection<Message<?>> completeGroup(Message<?> message, Object correlationKey, MessageGroup group,
			Lock lock) {

		Collection<Message<?>> partialSequence = null;
		Object result;
		try {
			this.logger.debug(() -> "Completing group with correlationKey [" + correlationKey + "]");

			result = this.outputProcessor.processMessageGroup(group);
			if (result instanceof Collection<?>) {
				verifyResultCollectionConsistsOfMessages((Collection<?>) result);
				partialSequence = (Collection<Message<?>>) result;
			}

			if (this.popSequence && partialSequence == null) {
				AbstractIntegrationMessageBuilder<?> messageBuilder = null;
				if (result instanceof AbstractIntegrationMessageBuilder<?>) {
					messageBuilder = (AbstractIntegrationMessageBuilder<?>) result;
				}
				else if (!(result instanceof Message<?>)) {
					messageBuilder =
							getMessageBuilderFactory()
									.withPayload(result)
									.copyHeaders(message.getHeaders());
				}
				else if (compareSequences((Message<?>) result, message)) {
					messageBuilder =
							getMessageBuilderFactory()
									.fromMessage((Message<?>) result);
				}
				result = messageBuilder != null ? messageBuilder.popSequenceDetails() : result;
			}
		}
		finally {
			if (this.releaseLockBeforeSend) {
				lock.unlock();
			}
		}
		sendOutputs(result, message);
		return partialSequence;
	}

	private static boolean compareSequences(Message<?> msg1, Message<?> msg2) {
		Object sequence1 = msg1.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS);
		Object sequence2 = msg2.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS);
		return ObjectUtils.nullSafeEquals(sequence1, sequence2);

	}

	protected void verifyResultCollectionConsistsOfMessages(Collection<?> elements) {
		Class<?> commonElementType = CollectionUtils.findCommonElementType(elements);
		Assert.isAssignable(Message.class, commonElementType, () ->
				"The expected collection of Messages contains non-Message element: " + commonElementType);
	}

	protected Object obtainGroupTimeout(MessageGroup group) {
		if (this.groupTimeoutExpression != null) {
			Object timeout = this.groupTimeoutExpression.getValue(this.evaluationContext, group);
			if (timeout instanceof Date) {
				return timeout;
			}
			else if (timeout != null) {
				try {
					return Long.parseLong(timeout.toString());
				}
				catch (NumberFormatException ex) {
					throw new IllegalStateException("Error evaluating 'groupTimeoutExpression'", ex);
				}
			}
		}
		return null;
	}

	@Override
	public void destroy() {
		this.expireGroupScheduledFutures.values().forEach(future -> future.cancel(true));
	}

	@Override
	public void start() {
		if (!this.running) {
			this.running = true;
			if (this.outputProcessor instanceof Lifecycle) {
				((Lifecycle) this.outputProcessor).start();
			}
			if (this.releaseStrategy instanceof Lifecycle) {
				((Lifecycle) this.releaseStrategy).start();
			}
			if (this.expireTimeout > 0) {
				purgeOrphanedGroups();
				if (this.expireDuration != null) {
					getTaskScheduler()
							.scheduleWithFixedDelay(this::purgeOrphanedGroups, this.expireDuration);
				}
			}
		}
	}

	@Override
	public void stop() {
		if (this.running) {
			this.running = false;
			if (this.outputProcessor instanceof Lifecycle) {
				((Lifecycle) this.outputProcessor).stop();
			}
			if (this.releaseStrategy instanceof Lifecycle) {
				((Lifecycle) this.releaseStrategy).stop();
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Perform a {@link MessageGroupStore#expireMessageGroups(long)} with the provided {@link #expireTimeout}.
	 * Can be called externally at any time.
	 * Internally it is called from the scheduled task with the configured {@link #expireDuration}.
	 * @since 5.4
	 */
	public void purgeOrphanedGroups() {
		Assert.isTrue(this.expireTimeout > 0, "'expireTimeout' must be more than 0.");
		this.messageStore.expireMessageGroups(this.expireTimeout);
	}

	protected static class SequenceAwareMessageGroup extends SimpleMessageGroup {

		private final SimpleMessageGroup sourceGroup;

		public SequenceAwareMessageGroup(MessageGroup messageGroup) {
			/*
			 * Since this group is temporary, and never added to, we simply use the
			 * supplied group's message collection for the lookup rather than creating a
			 * new group.
			 */
			super(messageGroup.getMessages(), null, messageGroup.getGroupId(), messageGroup.getTimestamp(),
					messageGroup.isComplete(), true);
			if (messageGroup instanceof SimpleMessageGroup) {
				this.sourceGroup = (SimpleMessageGroup) messageGroup;
			}
			else {
				this.sourceGroup = null;
			}
		}

		/**
		 * This method determines whether messages have been added to this group that
		 * supersede the given message based on its sequence id. This can be helpful to
		 * avoid ending up with sequences larger than their required sequence size or
		 * sequences that are missing certain sequence numbers.
		 */
		@Override
		public boolean canAdd(Message<?> message) {
			if (this.size() == 0) {
				return true;
			}
			Integer messageSequenceNumber = message.getHeaders()
					.get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Integer.class);
			if (messageSequenceNumber != null && messageSequenceNumber > 0) {
				Integer messageSequenceSize = message.getHeaders()
						.get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, Integer.class);
				if (messageSequenceSize == null) {
					messageSequenceSize = 0;
				}
				return messageSequenceSize.equals(getSequenceSize())
						&& !(this.sourceGroup != null ? this.sourceGroup.containsSequence(messageSequenceNumber)
						: containsSequenceNumber(this.getMessages(), messageSequenceNumber));
			}
			return true;
		}

		private boolean containsSequenceNumber(Collection<Message<?>> messages, Integer messageSequenceNumber) {
			for (Message<?> member : messages) {
				if (messageSequenceNumber.equals(member.getHeaders().get(
						IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Integer.class))) {
					return true;
				}
			}
			return false;
		}

	}

	private class ForceReleaseMessageGroupProcessor implements MessageGroupProcessor {

		ForceReleaseMessageGroupProcessor() {
		}

		@Override
		public Object processMessageGroup(MessageGroup group) {
			forceComplete(group);
			return null;
		}

	}

}
