/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.Collection;
import java.util.HashSet;

import org.springframework.integration.core.Message;
import org.springframework.integration.store.MessageStore;

/**
 * Represents a mutable group of correlated messages that is bound to a certain {@link MessageStore} and correlation
 * key. The group will grow during its lifetime, when messages are <code>add</code>ed to it. <strong>This is not thread
 * safe and should not be used for long running aggregations</strong>.
 * 
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * 
 * @since 2.0
 */
public class MessageGroup {

	private final Object correlationKey;

	private final Collection<Message<?>> marked = new HashSet<Message<?>>();

	private final Collection<Message<?>> unmarked = new HashSet<Message<?>>();

	public MessageGroup(Object correlationKey) {
		this.correlationKey = correlationKey;
	}

	public MessageGroup(Collection<? extends Message<?>> originalMessages, Object correlationKey) {
		this(correlationKey);
		for (Message<?> message : originalMessages) {
			add(message);
		}
	}

	/**
	 * Add a message to the internal list. This is needed to avoid hitting the underlying store or copying the internal
	 * list. Use with care.
	 */
	public boolean add(Message<?> message) {
		if (isMember(message)) {
			return false;
		}
		if (message.getHeaders().containsKey(MessageStore.PROCESSED)
				&& message.getHeaders().get(MessageStore.PROCESSED, Boolean.class)) {
			this.marked.add(message);
		} else {
			this.unmarked.add(message);
		}
		return true;
	}

	/**
	 * @return internal message list, modification is allowed, but not recommended
	 */
	public Collection<Message<?>> getUnmarked() {
		return unmarked;
	}

	/**
	 * @return internal message list, modification is allowed, but not recommended
	 */
	public Collection<Message<?>> getMarked() {
		return marked;
	}

	/**
	 * @return the correlation key that links these messages together according to a particular CorrelationStrategy
	 */
	public Object getCorrelationKey() {
		return correlationKey;
	}

	public boolean isComplete() {
		if (size() == 0) {
			return true;
		}
		int sequenceSize = getSequenceSize();
		// If there is no sequence then it must be complete....
		return sequenceSize == 0 || sequenceSize == size();
	}

	public int getSequenceSize() {
		if (size() == 0) {
			return 0;
		}
		return getOne().getHeaders().getSequenceSize();
	}

	private Message<?> getOne() {
		return unmarked.isEmpty() ? (marked.isEmpty() ? null : marked.iterator().next()) : unmarked.iterator().next();
	}

	public int size() {
		return marked.size() + unmarked.size();
	}

	/**
	 * This method determines whether messages have been added to this group that supersede the given message based on
	 * its sequence id. This can be helpful to avoid ending up with sequences larger than their required sequence size
	 * or sequences that are missing certain sequence numbers.
	 */
	private boolean isMember(Message<?> message) {
		if (size() == 0) {
			return false;
		}
		Integer messageSequenceNumber = message.getHeaders().getSequenceNumber();
		if (messageSequenceNumber != null && messageSequenceNumber > 0) {
			Integer messageSequenceSize = message.getHeaders().getSequenceSize();
			if (!messageSequenceSize.equals(getSequenceSize())
					|| containsSequenceNumber(unmarked, messageSequenceNumber)
					|| containsSequenceNumber(marked, messageSequenceNumber)) {
				return true;
			}
		}
		return false;
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
