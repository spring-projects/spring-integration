/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.gemfire.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import org.springframework.integration.Message;
import org.springframework.integration.store.MessageGroup;

/**
 * A {@link org.springframework.integration.store.MessageGroup} that manipulates keys and values to provide persistence.
 * Responsible for managing one group's messages as a {@link org.springframework.integration.store.MessageGroup}.
 *
 * @author Josh Long
 * @since 2.1
 */
@SuppressWarnings("serial")
public class KeyValueMessageGroup implements MessageGroup, Serializable {

	/**
	 * this should not be persisted. it's passed in through {@link KeyValueMessageGroupStore}, which has the reference to the {@link java.util.concurrent.ConcurrentMap} instance that should be set here
	 */
	private transient Map<String, Message<?>> marked;

	/**
	 * this should not be persisted. it's passed in through {@link KeyValueMessageGroupStore}, which has the reference to the {@link java.util.concurrent.ConcurrentMap} instance that should be set here
	 */
	private transient Map<String, Message<?>> unmarked;

	/**
	 * the #groupId is the unique ID to associate this aggregation of {@link org.springframework.integration.Message}s
	 */
	private Object groupId;

	/**
	 * passed in through the {@link org.springframework.integration.store.MessageGroupStore}
	 */
	private long timestamp;


	/**
	 * default javabean ctor (so that this object plays well as a {@link java.io.Serializable} object)
	 */
	public KeyValueMessageGroup() {
	}

	public KeyValueMessageGroup(Object groupId) {
		this(groupId, System.currentTimeMillis(), null, null);
	}

	public KeyValueMessageGroup(Object groupId, long timestamp,
								ConcurrentMap<String, Message<?>> marked,
								ConcurrentMap<String, Message<?>> unmarked) {
		this.groupId = groupId;
		this.timestamp = timestamp;
		this.marked = marked;
		this.unmarked = unmarked;
	}

	public KeyValueMessageGroup(Object groupId,
								ConcurrentMap<String, Message<?>> marked,
								ConcurrentMap<String, Message<?>> unmarked) {
		this(groupId, System.currentTimeMillis(), marked, unmarked);
	}


	@Override
	public int hashCode() {
		return groupId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof KeyValueMessageGroup) {
			Object otherGroupId = ((KeyValueMessageGroup) obj).getGroupId();
			return getGroupId().equals(otherGroupId);
		}
		return false;
	}

	public void setUnmarked(Map<String, Message<?>> unmarked) {
		this.unmarked = unmarked;
	}

	public void setMarked( Map<String, Message<?>> marked) {
		this.marked = marked;
	}

	/**
	 * @return the timestamp (milliseconds since epoch) associated with the creation of this group
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Query if the message can be added.
	 */
	public boolean canAdd(Message<?> message) {
		return !isMember(message);
	}

	/**
	 * Add this {@link org.springframework.integration.Message} to the
	 * {@link org.springframework.integration.store.MessageGroup}, delegating in this case to the {@link #unmarked} field
	 *
	 * @param message the {@link org.springframework.integration.Message} you are adding to the {@link java.util.Map}
	 */
	public void add(Message<?> message) {
		if (isMember(message)) {
			return;
		}

		String unmarkedKey = this.unmarkedKey(message);
		this.unmarked.put(unmarkedKey, (Message<?>) message);
	}

	/**
	 * the only reason we differentiate the keys is so that conceptually you could use the <em>same</em> {@link java.util.Map} instance for both <em>marked</em> and <em>unmarked</em> messages.
	 *
	 * This method simply differentiates the key, building on {@link #baseKey(org.springframework.integration.Message)}'s return value
	 *
	 * @param msg the {@link org.springframework.integration.Message} from which the key should be generated.
	 * @return a String to be used as a key
	 */
	protected String markedKey(Message<?> msg) {
		return baseKey(msg) + "-m";
	}

	/**
	 * the only reason we differentiate the keys is so that conceptually you could use the <em>same</em> {@link java.util.Map} instance for both <em>marked</em> and <em>unmarked</em> messages.
	 *
	 * This method simply differentiates the key, building on {@link #baseKey(org.springframework.integration.Message)}'s return value
	 *
	 * @param msg the {@link org.springframework.integration.Message} from which the key should be generated.
	 * @return a String to be used as a key
	 */
	protected String unmarkedKey(Message<?> msg) {
		return baseKey(msg) + "-u";
	}

	/**
	 * Removes this {@link org.springframework.integration.Message} from this {@link org.springframework.integration.store.MessageGroup}'s memory
	 *
	 * @param message the message to remove
	 */
	public void remove(Message<?> message) {
		if (unmarked.containsValue(message)) {
			unmarked.remove(unmarkedKey(message));
		}

		if (marked.containsValue(message)) {
			marked.remove(markedKey(message));
		}
	}

	/**
	 * the groupKey is based on the groupID and it sits at the beginning of all the keys for this {@link org.springframework.integration.store.MessageGroup}s keys
	 *
	 * @return a string based on {@link #getGroupId()}
	 */
	protected String groupKey() {
		return (getGroupId()).toString();
	}

	protected String baseKey(Message<?> msg) {
		String groupKey = groupKey();
		UUID id = msg.getHeaders().getId();
		Integer sn = msg.getHeaders().getSequenceNumber();
		Integer ss = msg.getHeaders().getSequenceSize();

		return String.format("%s-%s-%s-%s", groupKey, id.toString(),
				sn.toString(), ss.toString());
	}

	public Collection<Message<?>> getUnmarked() {
		return getMessagesForMessageGroup(this.unmarked);
	}

	/**
	 * this method will be used to discover all the messages for a given group in a {@link com.gemstone.gemfire.cache.Region}
	 *
	 * @param region the region from which we're hoping to discover these {@link org.springframework.integration.Message}s
	 * @return a collection of messages
	 */
	protected Collection<Message<?>> getMessagesForMessageGroup(
			Map<String, Message<?>> region) {
		try {
			String groupMsgKey = groupKey();
			Collection<Message<?>> msgs = new ArrayList<Message<?>>();

			for (String k : region.keySet()) {
				if (k.startsWith(groupMsgKey)) {
					msgs.add(region.get(k));
				}
			}

			return msgs;
		} catch (Throwable th) {
			throw new RuntimeException(th);
		}
	}

	public Collection<Message<?>> getMarked() {
		return getMessagesForMessageGroup(this.marked);
	}

	/**
	 * @return the key that links these messages together
	 */
	public Object getGroupId() {
		return groupId;
	}

	/**
	 * @return true if the group is complete (i.e. no more messages are expected to be added)
	 */
	public boolean isComplete() {
		if (size() == 0) {
			return true;
		}

		int sequenceSize = getSequenceSize();

		return (sequenceSize > 0) && (sequenceSize == size());
	}

	public int getSequenceSize() {
		if (size() == 0) {
			return 0;
		}

		return getOne().getHeaders().getSequenceSize();
	}

	/**
	 * Mark the given message in this group. If the message is not part of this group then this call has no effect.
	 *
	 * @param messageToMark the message that should be marked
	 */
	public void mark(Message<?> messageToMark) {
		if (this.unmarked.containsValue(messageToMark)) {
			this.unmarked.remove(baseKey(messageToMark));
		}

		this.marked.put(baseKey(messageToMark), messageToMark);
	}

	public void markAll() {
		for (Message<?> msg : getUnmarked())
			mark(msg);
	}

	/**
	 * @return the total number of messages (marked and unmarked) in this group
	 */
	public int size() {
		return getMarked().size() + getUnmarked().size();
	}

	/**
	 * @return a single message from the group
	 */
	public Message<?> getOne() {
		if (!this.unmarked.isEmpty()) {
			String aKey = this.unmarked.keySet().iterator().next();

			return this.unmarked.get(aKey);
		}

		return null;
	}

	/**
	 * This method determines whether messages have been added to this group that supersede the given message based on
	 * its sequence id. This can be helpful to avoid ending up with sequences larger than their required sequence size
	 * or sequences that are missing certain sequence numbers.
	 *
	 * @param message the message to test for candidacy
	 *
	 * @return whether or not the message is a member of the group
	 *
	 */
	protected boolean isMember(Message<?> message) {
		if (size() == 0) {
			return false;
		}

		Integer messageSequenceNumber = message.getHeaders().getSequenceNumber();

		if ((messageSequenceNumber != null) && (messageSequenceNumber > 0)) {
			Integer messageSequenceSize = message.getHeaders().getSequenceSize();

			if (!messageSequenceSize.equals(getSequenceSize())) {
				return true;
			} else {
				if (containsSequenceNumber(getUnmarked(), messageSequenceNumber) ||
						containsSequenceNumber(getUnmarked(),
								messageSequenceNumber)) {
					return true;
				}
			}
		}

		return false;
	}

	protected boolean containsSequenceNumber(Collection<Message<?>> messages,
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

	public void complete() {
		// no op for now pending complete refactoring of this class to rely on SMG
		
	}
}
