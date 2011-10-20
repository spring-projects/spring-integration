/*
 * Copyright 2002-2011 the original author or authors.
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
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.integration.Message;

/**
 * Represents a mutable group of correlated messages that is bound to a certain {@link MessageStore} and group id. The
 * group will grow during its lifetime, when messages are <code>add</code>ed to it. This MessageGroup is thread safe.
 * 
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @since 2.0
 */
public class SimpleMessageGroup implements MessageGroup {

	private final Object groupId;
	
	public final BlockingQueue<Message<?>> messages = new LinkedBlockingQueue<Message<?>>();
	
	private volatile int lastReleasedMessageSequence;

	private final long timestamp;
	
	private volatile boolean complete;

	public SimpleMessageGroup(Object groupId) {
		this(Collections.<Message<?>> emptyList(), groupId, System.currentTimeMillis(), false);
	}

	public SimpleMessageGroup(Collection<? extends Message<?>> messages, Object groupId) {
		this(messages, groupId, System.currentTimeMillis(), false);
	}

	public SimpleMessageGroup(Collection<? extends Message<?>> messages, Object groupId, long timestamp, boolean complete) {
		this.groupId = groupId;
		this.timestamp = timestamp;
		this.complete = complete;
		for (Message<?> message : messages) {
			addMessage(message);
		}
	}

	public SimpleMessageGroup(MessageGroup messageGroup) {
		this(messageGroup.getMessages(), messageGroup.getGroupId(), messageGroup.getTimestamp(), messageGroup.isComplete());
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public boolean canAdd(Message<?> message) {
		return !isMember(message);
	}

	public void add(Message<?> message) {
		addMessage(message);
	}

	public void remove(Message<?> message) {
		messages.remove(message);
	}
	
	public int getLastReleasedMessageSequenceNumber() {
		return lastReleasedMessageSequence;
	}

	private boolean addMessage(Message<?> message) {
		return this.messages.offer(message);
	}

	public Collection<Message<?>> getMessages() {
		return Collections.unmodifiableCollection(messages);
	}
	
	public void setLastReleasedMessageSequenceNumber(int sequenceNumber){
		this.lastReleasedMessageSequence = sequenceNumber;
	}

	public Object getGroupId() {
		return groupId;
	}

	public boolean isComplete() {
		return this.complete;
	}
	
	public void complete() {
		this.complete = true;
	}
	
	public int getSequenceSize() {
		if (size() == 0) {
			return 0;
		}
		return getOne().getHeaders().getSequenceSize();
	}

	public int size() {
		return this.messages.size();
	}

	public Message<?> getOne() {
		Message<?> one = messages.peek();
		return one;
	}
	
	public void clear(){
		this.messages.clear();
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
			if (!messageSequenceSize.equals(getSequenceSize())) {
				return true;
			}
			else {
				return this.containsSequenceNumber(messages, messageSequenceNumber);
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

	@Override
	public String toString() {
		return "SimpleMessageGroup{" +
				"groupId=" + groupId +
				", messages=" + messages +
				", timestamp=" + timestamp +
				'}';
	}
}