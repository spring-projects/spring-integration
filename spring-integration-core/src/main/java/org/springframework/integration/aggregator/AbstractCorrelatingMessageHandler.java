/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupCallback;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Abstract Message handler that holds a buffer of correlated messages in a
 * {@link MessageStore}. This class takes care of correlated groups of messages
 * that can be completed in batches. It is useful for custom implementation of MessageHandlers that require correlation 
 * and is used as a base class for Aggregator - {@link AggregatingMessageHandler} and
 * Resequencer - {@link ResequencingMessageHandler},
 * or custom implementations requiring correlation. 
 * <p/>
 * To customize this handler inject {@link CorrelationStrategy},
 * {@link ReleaseStrategy}, and {@link MessageGroupProcessor} implementations as
 * you require.
 * <p/>
 * By default the {@link CorrelationStrategy} will be a
 * {@link HeaderAttributeCorrelationStrategy} and the {@link ReleaseStrategy} will be a
 * {@link SequenceSizeReleaseStrategy}.
 *
 * @author Iwein Fuld
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public abstract class AbstractCorrelatingMessageHandler extends AbstractMessageHandler implements MessageProducer {

	private static final Log logger = LogFactory.getLog(AbstractCorrelatingMessageHandler.class);

	public static final long DEFAULT_SEND_TIMEOUT = 1000L;

	protected volatile MessageGroupStore messageStore;

	private final MessageGroupProcessor outputProcessor;

	private volatile CorrelationStrategy correlationStrategy;

	private volatile ReleaseStrategy releaseStrategy;

	private MessageChannel outputChannel;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private volatile MessageChannel discardChannel = new NullChannel();

	private boolean sendPartialResultOnExpiry = false;
	
	private final Object correlationLocksMonitor = new Object();

	private final ConcurrentMap<Object, Object> locks = new ConcurrentHashMap<Object, Object>();

	private volatile boolean sequenceAware = false;

	public AbstractCorrelatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store,
									 CorrelationStrategy correlationStrategy, ReleaseStrategy releaseStrategy) {
		Assert.notNull(processor);
		Assert.notNull(store);
		setMessageStore(store);
		this.outputProcessor = processor;
		this.correlationStrategy = correlationStrategy == null ?
				new HeaderAttributeCorrelationStrategy(MessageHeaders.CORRELATION_ID) : correlationStrategy;
		this.releaseStrategy = releaseStrategy == null ? new SequenceSizeReleaseStrategy() : releaseStrategy;
		this.messagingTemplate.setSendTimeout(DEFAULT_SEND_TIMEOUT);
		if (this.releaseStrategy instanceof SequenceSizeReleaseStrategy){
			sequenceAware = true;
		}
	}

	public AbstractCorrelatingMessageHandler(MessageGroupProcessor processor, MessageGroupStore store) {
		this(processor, store, null, null);
	}

	public AbstractCorrelatingMessageHandler(MessageGroupProcessor processor) {
		this(processor, new SimpleMessageStore(0), null, null);
	}


	public void setMessageStore(MessageGroupStore store) {
		this.messageStore = store;
		store.registerMessageGroupExpiryCallback(new MessageGroupCallback() {
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
		if (this.releaseStrategy instanceof SequenceSizeReleaseStrategy){
			sequenceAware = true;
		}
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		Assert.notNull(outputChannel, "'outputChannel' must not be null");
		this.outputChannel = outputChannel;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		BeanFactory beanFactory = this.getBeanFactory();
		if (beanFactory != null) {
			this.messagingTemplate.setBeanFactory(beanFactory);
		}
	}

	public void setDiscardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	public void setSendPartialResultOnExpiry(boolean sendPartialResultOnExpiry) {
		this.sendPartialResultOnExpiry = sendPartialResultOnExpiry;
	}

	public void setReleasePartialSequences(boolean releasePartialSequences){
		Assert.isInstanceOf(SequenceSizeReleaseStrategy.class, this.releaseStrategy,
				"Release strategy of type [" + this.releaseStrategy.getClass().getSimpleName()
						+ "] cannot release partial sequences. Use the default SequenceSizeReleaseStrategy instead.");
		((SequenceSizeReleaseStrategy)this.releaseStrategy).setReleasePartialSequences(releasePartialSequences);
	}

	@Override
	public String getComponentType() {
		return "aggregator";
	}
	
	protected MessageGroupStore getMessageStore() {
		return messageStore;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object correlationKey = correlationStrategy.getCorrelationKey(message);
		Assert.state(correlationKey!=null, "Null correlation not allowed.  Maybe the CorrelationStrategy is failing?");

		if (logger.isDebugEnabled()) {
			logger.debug("Handling message with correlationKey [" + correlationKey + "]: " + message);
		}

		// TODO: INT-1117 - make the lock global?
		Object lock = getLock(correlationKey);

		synchronized (lock) {
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
		
						synchronized(correlationLocksMonitor){
							locks.remove(messageGroup.getGroupId());
						}
					}
				} 				
			} 
			else {
				discardChannel.send(message);
			}
		}
	}


	/**
	 * Allows you to provide additional logic that needs to be performed after the MessageGroup was released. 
	 * @param group
	 * @param completedMessages
	 */
	protected abstract void afterRelease(MessageGroup group, Collection<Message<?>> completedMessages);

	private final boolean forceComplete(MessageGroup group) {

		Object correlationKey = group.getGroupId();
		Object lock = getLock(correlationKey);
		synchronized (lock) {

			if (group.size() > 0) {
				try {
					if (releaseStrategy.canRelease(group)) {
						this.completeGroup(correlationKey, group);
					} 
					else {
						this.expireGroup(correlationKey, group);
					}
				}
				finally {
					this.remove(group);
				}
				return true;
			}
			return false;
		}
	}

	private Object getLock(Object correlationKey) {
		synchronized(correlationLocksMonitor){
			locks.putIfAbsent(correlationKey, new Object());
			return locks.get(correlationKey);
		}
	}

	void remove(MessageGroup group) {
		Object correlationKey = group.getGroupId();
		messageStore.removeMessageGroup(correlationKey);
	}

	protected int findLastReleasedSequenceNumber(Object groupId, Collection<Message<?>> partialSequence){
		List<Message<?>> sorted = new ArrayList<Message<?>>((Collection<? extends Message<?>>)partialSequence);
		Collections.sort(sorted, new SequenceNumberComparator());
		
		Message<?> lastReleasedMessage = sorted.get(partialSequence.size()-1);
		
		return lastReleasedMessage.getHeaders().getSequenceNumber();
	}

	private MessageGroup store(Object correlationKey, Message<?> message) {
		return messageStore.addMessageToGroup(correlationKey, message);
	}

	private void expireGroup(Object correlationKey, MessageGroup group) {
		if (logger.isInfoEnabled()) {
			logger.info("Expiring MessageGroup with correlationKey[" + correlationKey + "]");
		}
		if (sendPartialResultOnExpiry) {
			if (logger.isDebugEnabled()) {
				logger.debug("Prematurely releasing partially complete group with key ["
						+ correlationKey + "] to: " + outputChannel);
			}
			completeGroup(correlationKey, group);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Discarding messages of partially complete group with key ["
						+ correlationKey + "] to: " + discardChannel);
			}
			for (Message<?> message : group.getMessages()) {
				discardChannel.send(message);
			}
		}
	}

	private void completeGroup(Object correlationKey, MessageGroup group) {
		Message<?> first = null;
		if (group != null) {
			first = group.getOne();
		}
		completeGroup(first, correlationKey, group);
	}

	@SuppressWarnings("unchecked")
	private Collection<Message<?>> completeGroup(Message<?> message, Object correlationKey, MessageGroup group) {
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
	
	private void verifyResultCollectionConsistsOfMessages(Collection<?> elements){
		Class<?> commonElementType = CollectionUtils.findCommonElementType(elements);
		Assert.isAssignable(Message.class, commonElementType, "The expected collection of Messages contains non-Message element: " + commonElementType);
	}

	@SuppressWarnings("rawtypes")
	private void sendReplies(Object processorResult, Message message) {
		Object replyChannelHeader = null;
		if (message != null) {
			replyChannelHeader = message.getHeaders().getReplyChannel();
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

	private void sendReplyMessage(Object reply, Object replyChannel) {
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

	private boolean shouldSendMultipleReplies(Iterable<?> iter) {
		for (Object next : iter) {
			if (next instanceof Message<?>) {
				return true;
			}
		}
		return false;
	}

	private static class SequenceAwareMessageGroup extends SimpleMessageGroup {

		public SequenceAwareMessageGroup(MessageGroup messageGroup) {
			super(messageGroup);
		}

		/**
		 * This method determines whether messages have been added to this group that supersede the given message based on
		 * its sequence id. This can be helpful to avoid ending up with sequences larger than their required sequence size
		 * or sequences that are missing certain sequence numbers.
		 */
		public boolean canAdd(Message<?> message) {
			if (this.size() == 0) {
				return true;
			}
			Integer messageSequenceNumber = message.getHeaders().getSequenceNumber();
			if (messageSequenceNumber != null && messageSequenceNumber > 0) {
				Integer messageSequenceSize = message.getHeaders().getSequenceSize();
				if (!messageSequenceSize.equals(this.getSequenceSize())) {
					return false;
				}
				else {
					return !this.containsSequenceNumber(this.getMessages(), messageSequenceNumber);
				}
			}
			return true;
		}

		private boolean containsSequenceNumber(Collection<Message<?>> messages, Integer messageSequenceNumber) {
			for (Message<?> member : messages) {
				Integer memberSequenceNumber = member.getHeaders().getSequenceNumber();
				if (messageSequenceNumber.equals(memberSequenceNumber)) {
					return true;
				}
			}
			return false;
		}
	}

}
