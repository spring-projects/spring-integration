/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.Lifecycle;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.DiscardingMessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Abstract Message handler that holds a buffer of correlated messages in a
 * {@link MessageStore}. This class takes care of correlated groups of messages
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
 * Use proper {@link CorrelationStrategy} for cases when same {@link MessageStore} is used
 * for multiple handlers to ensure uniqueness of message groups across handlers.
 *
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author David Liu
 * @author Enrique Rodriguez
 * @author Meherzad Lahewala
 *
 * @since 2.0
 */
public abstract class AbstractCorrelatingMessageHandler extends AbstractMessageProducingHandler
		implements DiscardingMessageHandler, DisposableBean, ApplicationEventPublisherAware, Lifecycle {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Comparator<Message<?>> sequenceNumberComparator = new MessageSequenceComparator();

	private final Map<UUID, ScheduledFuture<?>> expireGroupScheduledFutures = new ConcurrentHashMap<>();

	private final Set<Object> groupIds = ConcurrentHashMap.newKeySet();

	private MessageGroupProcessor outputProcessor;

	private MessageGroupStore messageStore;

	private CorrelationStrategy correlationStrategy;

	private ReleaseStrategy releaseStrategy;

	private boolean releaseStrategySet;

	private MessageChannel discardChannel;

	private String discardChannelName;

	private boolean sendPartialResultOnExpiry = false;

	private boolean sequenceAware = false;

	private LockRegistry lockRegistry = new DefaultLockRegistry();

	private boolean lockRegistrySet = false;

	private long minimumTimeoutForEmptyGroups;

	private boolean releasePartialSequences;

	private Expression groupTimeoutExpression;

	private List<Advice> forceReleaseAdviceChain;

	private MessageGroupProcessor forceReleaseProcessor = new ForceReleaseMessageGroupProcessor();

	private EvaluationContext evaluationContext;

	private ApplicationEventPublisher applicationEventPublisher;

	private boolean expireGroupsUponTimeout = true;

	private volatile boolean running;

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
		store.registerMessageGroupExpiryCallback(
				(messageGroupStore, group) -> this.forceReleaseProcessor.processMessageGroup(group));
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

	@Override
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		super.setTaskScheduler(taskScheduler);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		Assert.state(!(this.discardChannelName != null && this.discardChannel != null),
				"'discardChannelName' and 'discardChannel' are mutually exclusive.");
		BeanFactory beanFactory = this.getBeanFactory();
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

		if (this.discardChannel == null) {
			this.discardChannel = new NullChannel();
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

	protected MessageGroupProcessor getOutputProcessor() {
		return this.outputProcessor;
	}

	protected CorrelationStrategy getCorrelationStrategy() {
		return this.correlationStrategy;
	}

	protected ReleaseStrategy getReleaseStrategy() {
		return this.releaseStrategy;
	}

	@Override
	public MessageChannel getDiscardChannel() {
		if (this.discardChannelName != null) {
			synchronized (this) {
				if (this.discardChannelName != null) {
					this.discardChannel = getChannelResolver().resolveDestination(this.discardChannelName);
					this.discardChannelName = null;
				}
			}
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
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object correlationKey = this.correlationStrategy.getCorrelationKey(message);
		Assert.state(correlationKey != null, "Null correlation not allowed.  Maybe the CorrelationStrategy is failing?");

		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Handling message with correlationKey [" + correlationKey + "]: " + message);
		}

		UUID groupIdUuid = UUIDConverter.getUUID(correlationKey);
		Lock lock = this.lockRegistry.obtain(groupIdUuid.toString());

		lock.lockInterruptibly();
		try {
			ScheduledFuture<?> scheduledFuture = this.expireGroupScheduledFutures.remove(groupIdUuid);
			if (scheduledFuture != null) {
				boolean canceled = scheduledFuture.cancel(true);
				if (canceled && this.logger.isDebugEnabled()) {
					this.logger.debug("Cancel 'ScheduledFuture' for MessageGroup with Correlation Key [ "
							+ correlationKey + "].");
				}
			}
			MessageGroup messageGroup = this.messageStore.getMessageGroup(correlationKey);
			if (this.sequenceAware) {
				messageGroup = new SequenceAwareMessageGroup(messageGroup);
			}

			if (!messageGroup.isComplete() && messageGroup.canAdd(message)) {
				if (this.logger.isTraceEnabled()) {
					this.logger.trace("Adding message to group [ " + messageGroup + "]");
				}
				messageGroup = this.store(correlationKey, message);

				if (this.releaseStrategy.canRelease(messageGroup)) {
					Collection<Message<?>> completedMessages = null;
					try {
						completedMessages = completeGroup(message, correlationKey, messageGroup);
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
				discardMessage(message);
			}
		}
		finally {
			lock.unlock();
		}
	}

	protected boolean isExpireGroupsUponCompletion() {
		return false;
	}

	private void removeEmptyGroupAfterTimeout(MessageGroup messageGroup, long timeout) {
		Object groupId = messageGroup.getGroupId();
		UUID groupUuid = UUIDConverter.getUUID(groupId);
		ScheduledFuture<?> scheduledFuture = getTaskScheduler()
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
								if (this.logger.isDebugEnabled()) {
									this.logger.debug("Removing empty group: " + groupUuid);
								}
								remove(messageGroup);
							}
						}
						finally {
							lock.unlock();
						}
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						if (this.logger.isDebugEnabled()) {
							this.logger.debug("Thread was interrupted while trying to obtain lock."
									+ "Rescheduling empty MessageGroup [ " + groupId + "] for removal.");
						}
						removeEmptyGroupAfterTimeout(messageGroup, timeout);
					}

				}, new Date(System.currentTimeMillis() + timeout));

		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Schedule empty MessageGroup [ " + groupId + "] for removal.");
		}
		this.expireGroupScheduledFutures.put(groupUuid, scheduledFuture);
	}

	private void scheduleGroupToForceComplete(MessageGroup messageGroup) {
		final Long groupTimeout = obtainGroupTimeout(messageGroup);
		/*
		 * When 'groupTimeout' is evaluated to 'null' we do nothing.
		 * The 'MessageGroupStoreReaper' can be used to 'forceComplete' message groups.
		 */
		if (groupTimeout != null) {
			if (groupTimeout > 0) {
				final Object groupId = messageGroup.getGroupId();
				final long timestamp = messageGroup.getTimestamp();
				final long lastModified = messageGroup.getLastModified();
				ScheduledFuture<?> scheduledFuture = getTaskScheduler()
						.schedule(() -> {
							try {
								processForceRelease(groupId, timestamp, lastModified);
							}
							catch (MessageDeliveryException e) {
								if (AbstractCorrelatingMessageHandler.this.logger.isWarnEnabled()) {
									AbstractCorrelatingMessageHandler.this.logger.warn("The MessageGroup ["
											+ groupId + "] is rescheduled by the reason of:", e);
								}
								scheduleGroupToForceComplete(groupId);
							}
						}, new Date(System.currentTimeMillis() + groupTimeout));

				if (this.logger.isDebugEnabled()) {
					this.logger.debug("Schedule MessageGroup [ " + messageGroup + "] to 'forceComplete'.");
				}
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

	private void discardMessage(Message<?> message) {
		this.messagingTemplate.send(getDiscardChannel(), message);
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

	protected void forceComplete(MessageGroup group) {

		Object correlationKey = group.getGroupId();
		// UUIDConverter is no-op if already converted
		Lock lock = this.lockRegistry.obtain(UUIDConverter.getUUID(correlationKey).toString());
		boolean removeGroup = true;
		try {
			lock.lockInterruptibly();
			try {
				ScheduledFuture<?> scheduledFuture =
						this.expireGroupScheduledFutures.remove(UUIDConverter.getUUID(correlationKey));
				if (scheduledFuture != null) {
					boolean canceled = scheduledFuture.cancel(false);
					if (canceled && this.logger.isDebugEnabled()) {
						this.logger.debug("Cancel 'forceComplete' scheduling for MessageGroup [ " + group + "].");
					}
				}
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
						if (this.releaseStrategy.canRelease(groupNow)) {
							completeGroup(correlationKey, groupNow);
						}
						else {
							expireGroup(correlationKey, groupNow);
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
						removeGroup = lastModifiedNow <= (System.currentTimeMillis() - this.minimumTimeoutForEmptyGroups);
						if (removeGroup && this.logger.isDebugEnabled()) {
							this.logger.debug("Removing empty group: " + correlationKey);
						}
					}
				}
				else {
					removeGroup = false;
					if (this.logger.isDebugEnabled()) {
						this.logger.debug("Group expiry candidate (" + correlationKey +
								") has changed - it may be reconsidered for a future expiration");
					}
				}
			}
			catch (MessageDeliveryException e) {
				removeGroup = false;
				if (this.logger.isDebugEnabled()) {
					this.logger.debug("Group expiry candidate (" + correlationKey +
							") has been affected by MessageDeliveryException - " +
							"it may be reconsidered for a future expiration one more time");
				}
				throw e;
			}
			finally {
				try {
					if (removeGroup) {
						this.remove(group);
					}
				}
				finally {
					lock.unlock();
				}
			}
		}
		catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			this.logger.debug("Thread was interrupted while trying to obtain lock");
		}
	}

	protected void remove(MessageGroup group) {
		Object correlationKey = group.getGroupId();
		this.messageStore.removeMessageGroup(correlationKey);
		this.groupIds.remove(group.getGroupId());
	}

	protected int findLastReleasedSequenceNumber(Object groupId, Collection<Message<?>> partialSequence) {
		Message<?> lastReleasedMessage = Collections.max(partialSequence, this.sequenceNumberComparator);
		return new IntegrationMessageHeaderAccessor(lastReleasedMessage).getSequenceNumber();
	}

	protected MessageGroup store(Object correlationKey, Message<?> message) {
		this.groupIds.add(correlationKey);
		return this.messageStore.addMessageToGroup(correlationKey, message);
	}

	protected void expireGroup(Object correlationKey, MessageGroup group) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Expiring MessageGroup with correlationKey[" + correlationKey + "]");
		}
		if (this.sendPartialResultOnExpiry) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Prematurely releasing partially complete group with key ["
						+ correlationKey + "] to: " + getOutputChannel());
			}
			completeGroup(correlationKey, group);
		}
		else {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Discarding messages of partially complete group with key ["
						+ correlationKey + "] to: "
						+ (this.discardChannelName != null ? this.discardChannelName : this.discardChannel));
			}
			for (Message<?> message : group.getMessages()) {
				discardMessage(message);
			}
		}
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(new MessageGroupExpiredEvent(this, correlationKey, group
					.size(), new Date(group.getLastModified()), new Date(), !this.sendPartialResultOnExpiry));
		}
	}

	protected void completeGroup(Object correlationKey, MessageGroup group) {
		Message<?> first = null;
		if (group != null) {
			first = group.getOne();
		}
		completeGroup(first, correlationKey, group);
	}

	@SuppressWarnings("unchecked")
	protected Collection<Message<?>> completeGroup(Message<?> message, Object correlationKey, MessageGroup group) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Completing group with correlationKey [" + correlationKey + "]");
		}

		Object result = this.outputProcessor.processMessageGroup(group);
		Collection<Message<?>> partialSequence = null;
		if (result instanceof Collection<?>) {
			this.verifyResultCollectionConsistsOfMessages((Collection<?>) result);
			partialSequence = (Collection<Message<?>>) result;
		}
		this.sendOutputs(result, message);
		return partialSequence;
	}

	protected void verifyResultCollectionConsistsOfMessages(Collection<?> elements) {
		Class<?> commonElementType = CollectionUtils.findCommonElementType(elements);
		Assert.isAssignable(Message.class, commonElementType,
				"The expected collection of Messages contains non-Message element: " + commonElementType);
	}

	protected Long obtainGroupTimeout(MessageGroup group) {
		return this.groupTimeoutExpression != null
				? this.groupTimeoutExpression.getValue(this.evaluationContext, group, Long.class) : null;
	}

	@Override
	public void destroy() throws Exception {
		for (ScheduledFuture<?> future : this.expireGroupScheduledFutures.values()) {
			future.cancel(true);
		}
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
			Integer messageSequenceNumber = message.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER,
					Integer.class);
			if (messageSequenceNumber != null && messageSequenceNumber > 0) {
				Integer messageSequenceSize = message.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE,
						Integer.class);
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
			super();
		}

		@Override
		public Object processMessageGroup(MessageGroup group) {
			if (AbstractCorrelatingMessageHandler.this.groupIds.contains(group.getGroupId())) {
				forceComplete(group);
			}
			return null;
		}

	}

}
