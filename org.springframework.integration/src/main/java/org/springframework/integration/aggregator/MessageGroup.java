/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.integration.core.Message;

import java.util.*;

/**
 * Represents a mutable group of correlated messages that is bound to a certain
 * {@link org.springframework.integration.store.MessageStore} and correlation
 * key. The group will grow during its lifetime, when messages are <code>add</code>ed to it.
 * <strong>This is not thread safe and should not be used for long running aggregations</strong>.
 * <p/>
 * According to its
 * {@link org.springframework.integration.aggregator.CompletionStrategy} it can
 * be <i>complete</i> depending on the messages in the group.
 * <p/>
 * Optionally MessageGroupListeners can be added to get callbacks when (parts
 * of) the group are processed or the whole group is completed.
 * 
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class MessageGroup {

	private final CompletionStrategy completionStrategy;

	private final Object correlationKey;

	private final ArrayList<Message<?>> messages = new ArrayList<Message<?>>();

	private final List<MessageGroupListener> listeners;


	public MessageGroup(Collection<? extends Message<?>> originalMessages, CompletionStrategy completionStrategy,
			Object correlationKey, MessageGroupListener... listeners) {
		this.completionStrategy = completionStrategy;
		this.correlationKey = correlationKey;
		this.messages.addAll(originalMessages);
		this.listeners = Collections.unmodifiableList(Arrays.asList(listeners));
	}


	/**
	 * This method determines whether messages have been added to this group
	 * that supersede the given message based on its sequence id. This can be
	 * helpful to avoid ending up with sequences larger than their required
	 * sequence size or sequences that are missing certain sequence numbers.
	 */
	public boolean hasNoMessageSuperseding(Message<?> message) {
		Integer messageSequenceNumber = message.getHeaders().getSequenceNumber();
		if (messageSequenceNumber != null && messageSequenceNumber > 0) {
			for (Message<?> member : messages) {
				Integer memberSequenceNumber = member.getHeaders().getSequenceNumber();
				if (messageSequenceNumber.equals(memberSequenceNumber)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Add a message to the internal list. This is needed to avoid hitting the
	 * underlying store or copying the internal list. Use with care.
	 */
	protected void add(Message<?> message) {
		messages.add(message);
	}

	public boolean isComplete() {
		return completionStrategy.isComplete(messages);
	}

	/**
	 * @return internal message list, modification is allowed, but not
	 *         recommended
	 */
	public List<Message<?>> getMessages() {
		return messages;
	}

	/**
	 * @return the correlation key that links these messages together according
	 *         to a particular CorrelationStrategy
	 */
	public Object getCorrelationKey() {
		return correlationKey;
	}

	/**
	 * Call this method to sign off on processing of certain messages e.g. from
	 * a MessageProcessor. Typically this will remove these messages from the
	 * processing backlog.
	 */
	public void onProcessingOf(Message<?>... messages) {
		for (MessageGroupListener listener : listeners) {
			listener.onProcessingOf(messages);
		}
	}

	/**
	 * Call this method to signal the completion of the processing of an entire group.
	 */
	public void onCompletion() {
		for (MessageGroupListener listener : listeners) {
			listener.onCompletionOf(correlationKey);
		}
	}

	/**
	 * This method is a shorthand for signaling that all messages in the group have been
	 * processed and that the group is completed.
	 */
	public void onCompleteProcessing() {
		onProcessingOf(messages.toArray(new Message[messages.size()]));
		onCompletion();
    }

}
