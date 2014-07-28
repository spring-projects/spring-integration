/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageGroupStore.MessageGroupCallback;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Abstract Message handler that holds a buffer of correlated messages in a
 * {@link MessageStore}. This class takes care of correlated groups of messages
 * that can be completed in batches. It is useful for custom implementation of MessageHandlers that require correlation
 * and is used as a base class for Aggregator - {@link AggregatingMessageHandler} and
 * Resequencer - {@link ResequencingMessageHandler},
 * or custom implementations requiring correlation.
 * <p>
 * To customize this handler inject {@link CorrelationStrategy},
 * {@link ReleaseStrategy}, and {@link MessageGroupProcessor} implementations as
 * you require.
 * <p>
 * By default the {@link CorrelationStrategy} will be a
 * {@link HeaderAttributeCorrelationStrategy} and the {@link ReleaseStrategy} will be a
 * {@link SequenceSizeReleaseStrategy}.
 *
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public abstract class AbstractCorrelatingMessageHandler extends AbstractMessageHandler
		implements MessageProducer, DisposableBean, IntegrationEvaluationContextAware,
		ApplicationEventPublisherAware {

	private static final Log logger = LogFactory.getLog(AbstractCorrelatingMessageHandler.class);

	public static final long DEFAULT_SEND_TIMEOUT = 1000L;

	private final Map<UUID, ScheduledFuture<?>> expireGroupScheduledFutures = new HashMap<UUID, ScheduledFuture<?>>();

	protected volatile MessageGroupStore messageStore;

	private final MessageGroupProcessor outputProcessor;

	private volatile CorrelationStrategy correlationStrategy;

	private volatile ReleaseStrategy releaseStrategy;

	private MessageChannel outputChannel;

	private String outputChannelName;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private volatile MessageChannel discardChannel;

	private volatile String discardChannelName;

	private boolean sendPartialResultOnExpiry = false;

	private volatile boolean sequenceAware = false;

	private volatile LockRegistry lockRegistry = new DefaultLockRegistry();

	private boolean lockRegistrySet = false;

	private volatile long minimumTimeoutForEmptyGroups;

	private volatile boolean releasePartialSequences;

	private volatile Expression groupTimeoutExpression;

	private EvaluationContext evaluationContext;

	private volatile ApplicationEventPublisher applicationEventPublisher;

	private volatile boolean expireGroupsUponTimeout = true;

	public AbstractCorrelatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store,
									 CorrelationStrategy correlationStrategy, ReleaseStrategy releaseStrategy) {
		Assert.notNull(processor);

		Assert.notNull(store);
		setMessageStore(store);
		this.outputProcessor = processor;
		this.correlationStrategy = correlationStrategy == null ?
				new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID) : correlationStrategy;
		this.releaseStrategy = releaseStrategy == null ? new SequenceSizeReleaseStrategy() : releaseStrategy;
		this.messagingTemplate.setSendTimeout(DEFAULT_SEND_TIMEOUT);
		sequenceAware = this.releaseStrategy instanceof SequenceSizeReleaseStrategy;
	}

	public AbstractCorrelatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store) {
		this(processor, store, null, null);
	}

	public AbstractCorrelatingMessageHandler(MessageGroupProcessor processor) {
		this(processor, new SimpleMessageStore(0), null, null);
	}

	public void setLockRegistry(LockRegistry lockRegistry) {
		Assert.isTrue(!lockRegistrySet, "'this.lockRegistry' can not be reset once its been set");
		Assert.notNull("'lockRegistry' must not be null");
		this.lockRegistry = lockRegistry;
		this.lockRegistrySet = true;
	}

	public void setMessageStore(MessageGroupStore store) {
		this.messageStore = store;
		store.registerMessageGroupExpiryCallback(new MessageGroupCallback() {
			@Override
			public void execute(MessageGroupStore messageGroupStore, MessageGroup group) {
				forceComplete(group);
			}
		});
	}

	public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
		Assert.notNull(correlationStrategy);
		this.correlationStrategy = correlationStrategy;
	}

	public void setReleaseStrategy(ReleaseStrategy releaseStrategy) {
		Assert.notNull(releaseStrategy);
		this.releaseStrategy = releaseStrategy;
		sequenceAware = this.releaseStrategy instanceof SequenceSizeReleaseStrategy;
	}

	@Override
	public void setOutputChannel(MessageChannel outputChannel) {
		Assert.notNull(outputChannel, "'outputChannel' must not be null");
		this.outputChannel = outputChannel;
	}

	public void setOutputChannelName(String outputChannelName) {
		Assert.hasText(outputChannelName, "'outputChannelName' must not be empty");
		this.outputChannelName = outputChannelName;
	}

	public void setGroupTimeoutExpression(Expression groupTimeoutExpression) {
		this.groupTimeoutExpression = groupTimeoutExpression;
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
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
		BeanFactory beanFactory = this.getBeanFactory();
		if (beanFactory != null) {
			this.messagingTemplate.setBeanFactory(beanFactory);
			Assert.state(!(this.discardChannelName != null && this.discardChannel != null),
					"'discardChannelName' and 'discardChannel' are mutually exclusive.");

			Assert.state(!(this.outputChannelName != null && this.outputChannel != null),
					"'outputChannelName' and 'outputChannel' are mutually exclusive.");

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
			Assert.isInstanceOf(SequenceSizeReleaseStrategy.class, this.releaseStrategy,
					"Release strategy of type [" + this.releaseStrategy.getClass().getSimpleName()
							+ "] cannot release partial sequences. Use the default SequenceSizeReleaseStrategy instead.");
			((SequenceSizeReleaseStrategy)this.releaseStrategy).setReleasePartialSequences(releasePartialSequences);
		}

		/*
		 * Disallow any further changes to the lock registry
		 * (checked in the setter).
		 */
		this.lockRegistrySet = true;
	}

	public void setDiscardChannel(MessageChannel discardChannel) {
		Assert.notNull(discardChannel, "'discardChannel' cannot be null");
		this.discardChannel = discardChannel;
	}

	public void setDiscardChannelName(String discardChannelName) {
		Assert.hasText(discardChannelName, "'discardChannelName' must not be empty");
		this.discardChannelName = discardChannelName;
	}

	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
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

	public void setReleasePartialSequences(boolean releasePartialSequences) {
		this.releasePartialSequences = releasePartialSequences;
	}

	/**
	 * Expire (completely remove) a group if it is completed due to timeout.
	 * Subclasses setting this to false MUST handle null in the messages
	 * argument to {@link #afterRelease(MessageGroup, Collection)}.
	 * Default true.
	 * @param expireGroupsUponTimeout the expireGroupsOnTimeout to set
	 * @since 4.1
	 */
	protected void setExpireGroupsUponTimeout(boolean expireGroupsUponTimeout) {
		this.expireGroupsUponTimeout = expireGroupsUponTimeout;
	}

	@Override
	public String getComponentType() {
		return "aggregator";
	}

	protected MessageGroupStore getMessageStore() {
		return messageStore;
	}

	protected Map<UUID, ScheduledFuture<?>> getExpireGroupScheduledFutures() {
		return expireGroupScheduledFutures;
	}

	protected MessageGroupProcessor getOutputProcessor() {
		return outputProcessor;
	}

	protected CorrelationStrategy getCorrelationStrategy() {
		return correlationStrategy;
	}

	protected ReleaseStrategy getReleaseStrategy() {
		return releaseStrategy;
	}

	protected MessageChannel getOutputChannel() {
		return outputChannel;
	}

	protected String getOutputChannelName() {
		return outputChannelName;
	}

	protected MessagingTemplate getMessagingTemplate() {
		return messagingTemplate;
	}

	protected MessageChannel getDiscardChannel() {
		return discardChannel;
	}

	protected String getDiscardChannelName() {
		return discardChannelName;
	}

	protected boolean isSendPartialResultOnExpiry() {
		return sendPartialResultOnExpiry;
	}

	protected boolean isSequenceAware() {
		return sequenceAware;
	}

	protected LockRegistry getLockRegistry() {
		return lockRegistry;
	}

	protected boolean isLockRegistrySet() {
		return lockRegistrySet;
	}

	protected long getMinimumTimeoutForEmptyGroups() {
		return minimumTimeoutForEmptyGroups;
	}

	protected boolean isReleasePartialSequences() {
		return releasePartialSequences;
	}

	protected Expression getGroupTimeoutExpression() {
		return groupTimeoutExpression;
	}

	protected EvaluationContext getEvaluationContext() {
		return evaluationContext;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object correlationKey = correlationStrategy.getCorrelationKey(message);
		Assert.state(correlationKey!=null, "Null correlation not allowed.  Maybe the CorrelationStrategy is failing?");

		if (logger.isDebugEnabled()) {
			logger.debug("Handling message with correlationKey [" + correlationKey + "]: " + message);
		}

		UUID groupIdUuid = UUIDConverter.getUUID(correlationKey);
		Lock lock = this.lockRegistry.obtain(groupIdUuid.toString());

		lock.lockInterruptibly();
		try {
			ScheduledFuture<?> scheduledFuture = this.expireGroupScheduledFutures.remove(groupIdUuid);
			if (scheduledFuture != null) {
				boolean canceled = scheduledFuture.cancel(true);
				if (canceled && logger.isDebugEnabled()) {
					logger.debug("Cancel 'forceComplete' scheduling for MessageGroup with Correlation Key [ " + correlationKey + "].");
				}
			}
			MessageGroup messageGroup = messageStore.getMessageGroup(correlationKey);
			if (this.sequenceAware){
				messageGroup = new SequenceAwareMessageGroup(messageGroup);
			}

			if (!messageGroup.isComplete() && messageGroup.canAdd(message)) {
				if (logger.isTraceEnabled()) {
					logger.trace("Adding message to group [ " + messageGroup + "]");
				}
				messageGroup = this.store(correlationKey, message);

				if (releaseStrategy.canRelease(messageGroup)) {
					Collection<Message<?>> completedMessages = null;
					try {
						completedMessages = this.completeGroup(message, correlationKey, messageGroup);
					}
					finally {
						// Always clean up even if there was an exception
						// processing messages
						this.afterRelease(messageGroup, completedMessages);
					}
				}
				else {
					Long groupTimeout = this.obtainGroupTimeout(messageGroup);
					if (groupTimeout != null && groupTimeout >= 0) {
						if (groupTimeout > 0) {
							final MessageGroup messageGroupToSchedule = messageGroup;

							scheduledFuture = this.getTaskScheduler()
									.schedule(new Runnable() {

										@Override
										public void run() {
											AbstractCorrelatingMessageHandler.this.forceComplete(messageGroupToSchedule);
										}
									}, new Date(System.currentTimeMillis() + groupTimeout));

							if (logger.isDebugEnabled()) {
								logger.debug("Schedule MessageGroup [ " + messageGroup + "] to 'forceComplete'.");
							}
							this.expireGroupScheduledFutures.put(groupIdUuid, scheduledFuture);
						}
						else {
							this.forceComplete(messageGroup);
						}
					}
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

	private void discardMessage(Message<?> message) {
		if (this.discardChannelName != null) {
			synchronized (this) {
				if (this.discardChannelName != null) {
					try {
						this.discardChannel = getBeanFactory().getBean(this.discardChannelName, MessageChannel.class);
						this.discardChannelName = null;
					}
					catch (BeansException e) {
						throw new DestinationResolutionException("Failed to look up MessageChannel with name '"
								+ this.discardChannelName + "' in the BeanFactory.");
					}
				}
			}
		}
		this.discardChannel.send(message);
	}

	/**
	 * Allows you to provide additional logic that needs to be performed after the MessageGroup was released.
	 * @param group The group.
	 * @param completedMessages The completed messages.
	 */
	protected abstract void afterRelease(MessageGroup group, Collection<Message<?>> completedMessages);

	protected void forceComplete(MessageGroup group) {

		Object correlationKey = group.getGroupId();
		// UUIDConverter is no-op if already converted
		Lock lock = this.lockRegistry.obtain(UUIDConverter.getUUID(correlationKey).toString());
		boolean removeGroup = true;
		try {
			lock.lockInterruptibly();
			try {
				ScheduledFuture<?> scheduledFuture = this.expireGroupScheduledFutures.remove(UUIDConverter.getUUID(correlationKey));
				if (scheduledFuture != null) {
					boolean canceled = scheduledFuture.cancel(false);
					if (canceled && logger.isDebugEnabled()) {
						logger.debug("Cancel 'forceComplete' scheduling for MessageGroup [ " + group + "].");
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
						if (releaseStrategy.canRelease(groupNow)) {
							completeGroup(correlationKey, groupNow);
						}
						else {
							expireGroup(correlationKey, groupNow);
						}
						if (!this.expireGroupsUponTimeout) {
							afterRelease(groupNow, null);
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
						if (removeGroup && logger.isDebugEnabled()) {
							logger.debug("Removing empty group: " + correlationKey);
						}
					}
				}
				else {
					removeGroup = false;
					if (logger.isDebugEnabled()) {
						logger.debug("Group expiry candidate (" + correlationKey +
								") has changed - it may be reconsidered for a future expiration");
					}
				}
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
			logger.debug("Thread was interrupted while trying to obtain lock");
		}
	}

	void remove(MessageGroup group) {
		Object correlationKey = group.getGroupId();
		messageStore.removeMessageGroup(correlationKey);
	}

	protected int findLastReleasedSequenceNumber(Object groupId, Collection<Message<?>> partialSequence){
		List<Message<?>> sorted = new ArrayList<Message<?>>(partialSequence);
		Collections.sort(sorted, new SequenceNumberComparator());

		Message<?> lastReleasedMessage = sorted.get(partialSequence.size() - 1);

		return new IntegrationMessageHeaderAccessor(lastReleasedMessage).getSequenceNumber();
	}

	protected MessageGroup store(Object correlationKey, Message<?> message) {
		return messageStore.addMessageToGroup(correlationKey, message);
	}

	protected void expireGroup(Object correlationKey, MessageGroup group) {
		if (logger.isInfoEnabled()) {
			logger.info("Expiring MessageGroup with correlationKey[" + correlationKey + "]");
		}
		if (sendPartialResultOnExpiry) {
			if (logger.isDebugEnabled()) {
				logger.debug("Prematurely releasing partially complete group with key ["
						+ correlationKey + "] to: "
						+ (this.outputChannelName != null ? this.outputChannelName : this.outputChannel));
			}
			completeGroup(correlationKey, group);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Discarding messages of partially complete group with key ["
						+ correlationKey + "] to: "
						+ (this.discardChannelName != null ? this.discardChannelName : this.discardChannel));
			}
			for (Message<?> message : group.getMessages()) {
				discardMessage(message);
			}
		}
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(new MessageGroupExpiredEvent(this, correlationKey, group
					.size(), new Date(group.getLastModified()) , new Date(), !sendPartialResultOnExpiry));
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
		if (logger.isDebugEnabled()) {
			logger.debug("Completing group with correlationKey [" + correlationKey + "]");
		}

		Object result = outputProcessor.processMessageGroup(group);
		Collection<Message<?>> partialSequence = null;
		if (result instanceof Collection<?>) {
			this.verifyResultCollectionConsistsOfMessages((Collection<?>) result);
			partialSequence = (Collection<Message<?>>) result;
		}
		this.sendReplies(result, message);
		return partialSequence;
	}

	protected void verifyResultCollectionConsistsOfMessages(Collection<?> elements){
		Class<?> commonElementType = CollectionUtils.findCommonElementType(elements);
		Assert.isAssignable(Message.class, commonElementType, "The expected collection of Messages contains non-Message element: " + commonElementType);
	}

	@SuppressWarnings("rawtypes")
	protected void sendReplies(Object processorResult, Message message) {
		Object replyChannelHeader = null;
		if (message != null) {
			replyChannelHeader = message.getHeaders().getReplyChannel();
		}

		if (this.outputChannelName != null) {
			synchronized (this) {
				if (this.outputChannelName != null) {
					try {
						this.outputChannel = getBeanFactory().getBean(this.outputChannelName, MessageChannel.class);
						this.outputChannelName = null;
					}
					catch (BeansException e) {
						throw new DestinationResolutionException("Failed to look up MessageChannel with name '"
								+ this.outputChannelName + "' in the BeanFactory.");
					}
				}
			}
		}

		Object replyChannel = this.outputChannel;
		if (this.outputChannel == null) {
			replyChannel = replyChannelHeader;
		}
		Assert.notNull(replyChannel, "no outputChannel or replyChannel header available");
		if (processorResult instanceof Iterable<?> && shouldSendMultipleReplies((Iterable<?>) processorResult)) {
			for (Object next : (Iterable<?>) processorResult) {
				this.sendReplyMessage(next, replyChannel);
			}
		} else {
			this.sendReplyMessage(processorResult, replyChannel);
		}
	}

	protected void sendReplyMessage(Object reply, Object replyChannel) {
		if (replyChannel instanceof MessageChannel) {
			if (reply instanceof Message<?>) {
				this.messagingTemplate.send((MessageChannel) replyChannel, (Message<?>) reply);
			} else {
				this.messagingTemplate.convertAndSend((MessageChannel) replyChannel, reply);
			}
		} else if (replyChannel instanceof String) {
			if (reply instanceof Message<?>) {
				this.messagingTemplate.send((String) replyChannel, (Message<?>) reply);
			} else {
				this.messagingTemplate.convertAndSend((String) replyChannel, reply);
			}
		} else {
			throw new MessagingException("replyChannel must be a MessageChannel or String");
		}
	}

	protected boolean shouldSendMultipleReplies(Iterable<?> iter) {
		for (Object next : iter) {
			if (next instanceof Message<?>) {
				return true;
			}
		}
		return false;
	}

	protected Long obtainGroupTimeout(MessageGroup group) {
		return this.groupTimeoutExpression != null
				? this.groupTimeoutExpression.getValue(this.evaluationContext, group, Long.class) : null;
	}

	@Override
	public void destroy() throws Exception {
		for (ScheduledFuture<?> future : expireGroupScheduledFutures.values()) {
			future.cancel(true);
		}
	}

	protected static class SequenceAwareMessageGroup extends SimpleMessageGroup {

		public SequenceAwareMessageGroup(MessageGroup messageGroup) {
			super(messageGroup);
		}

		/**
		 * This method determines whether messages have been added to this group that supersede the given message based on
		 * its sequence id. This can be helpful to avoid ending up with sequences larger than their required sequence size
		 * or sequences that are missing certain sequence numbers.
		 */
		@Override
		public boolean canAdd(Message<?> message) {
			if (this.size() == 0) {
				return true;
			}
			IntegrationMessageHeaderAccessor messageHeaderAccessor = new IntegrationMessageHeaderAccessor(message);
			Integer messageSequenceNumber = messageHeaderAccessor.getSequenceNumber();
			if (messageSequenceNumber != null && messageSequenceNumber > 0) {
				Integer messageSequenceSize = messageHeaderAccessor.getSequenceSize();
				return messageSequenceSize.equals(this.getSequenceSize())
						&& !this.containsSequenceNumber(this.getMessages(), messageSequenceNumber);
			}
			return true;
		}

		private boolean containsSequenceNumber(Collection<Message<?>> messages, Integer messageSequenceNumber) {
			for (Message<?> member : messages) {
				Integer memberSequenceNumber = new IntegrationMessageHeaderAccessor(member).getSequenceNumber();
				if (messageSequenceNumber.equals(memberSequenceNumber)) {
					return true;
				}
			}
			return false;
		}
	}

}
