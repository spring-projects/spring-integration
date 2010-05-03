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

package org.springframework.integration.store;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.integration.core.Message;

/**
 * Represents a mutable group of correlated messages that is bound to a certain
 * {@link MessageStore} and correlation key. The group will grow during its
 * lifetime, when messages are <code>add</code>ed to it. <strong>This is not
 * thread safe and should not be used for long running aggregations</strong>.
 * 
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * 
 * @since 2.0
 */
public class SimpleMessageGroup implements MessageGroup {

	private final Object correlationKey;

	public final Collection<Message<?>> marked = new HashSet<Message<?>>();

	public final Collection<Message<?>> unmarked = new HashSet<Message<?>>();

	public SimpleMessageGroup(Object correlationKey) {
		this.correlationKey = correlationKey;
	}

	public SimpleMessageGroup(MessageGroup template) {
		this.correlationKey = template.getCorrelationKey();
		this.marked.addAll(template.getMarked());
		this.unmarked.addAll(template.getUnmarked());
	}

	public SimpleMessageGroup(Collection<? extends Message<?>> originalMessages,
			Object correlationKey) {
		this(correlationKey);
		for (Message<?> message : originalMessages) {
			add(message);
		}
	}

	public boolean add(Message<?> message) {
		if (isMember(message)) {
			return false;
		}
		this.unmarked.add(message);
		return true;
	}

	public Collection<Message<?>> getUnmarked() {
		return unmarked;
	}

	public Collection<Message<?>> getMarked() {
		return marked;
	}

	public Object getCorrelationKey() {
		return correlationKey;
	}

	public boolean isComplete() {
		if (size() == 0) {
			return true;
		}
		int sequenceSize = getSequenceSize();
		// If there is no sequence then it must be incomplete....
		return sequenceSize > 0 && sequenceSize == size();
	}

	public int getSequenceSize() {
		if (size() == 0) {
			return 0;
		}
		return getOne().getHeaders().getSequenceSize();
	}

	public void mark() {
		marked.addAll(unmarked);
		unmarked.clear();
	}

	public int size() {
		return marked.size() + unmarked.size();
	}

	public Message<?> getOne() {
		return unmarked.isEmpty() ? (marked.isEmpty() ? null : marked
				.iterator().next()) : unmarked.iterator().next();
	}

	/**
	 * This method determines whether messages have been added to this group
	 * that supersede the given message based on its sequence id. This can be
	 * helpful to avoid ending up with sequences larger than their required
	 * sequence size or sequences that are missing certain sequence numbers.
	 */
	private boolean isMember(Message<?> message) {
		if (size() == 0) {
			return false;
		}
		Integer messageSequenceNumber = message.getHeaders()
				.getSequenceNumber();
		if (messageSequenceNumber != null && messageSequenceNumber > 0) {
			Integer messageSequenceSize = message.getHeaders()
					.getSequenceSize();
			if (!messageSequenceSize.equals(getSequenceSize())
					|| containsSequenceNumber(unmarked, messageSequenceNumber)
					|| containsSequenceNumber(marked, messageSequenceNumber)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsSequenceNumber(Collection<Message<?>> messages,
			Integer messageSequenceNumber) {
		for (Message<?> member : messages) {
			Integer memberSequenceNumber = member.getHeaders()
					.getSequenceNumber();
			if (messageSequenceNumber.equals(memberSequenceNumber)) {
				return true;
			}
		}
		return false;
	}

}
