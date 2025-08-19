/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.store;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Value Object holding metadata about a MessageGroup in the MessageGroupStore.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Laszlo Szabo
 * @author Ngoc Nhan
 *
 * @since 2.1
 */
public class MessageGroupMetadata implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("serial")
	private final List<UUID> messageIds;

	private long timestamp;

	private volatile boolean complete;

	private volatile long lastModified;

	private volatile int lastReleasedMessageSequenceNumber;

	private volatile @Nullable String condition;

	public MessageGroupMetadata() {
		this.messageIds = new LinkedList<>();
	}

	@JsonCreator
	private MessageGroupMetadata(List<UUID> messageIds) {
		this.messageIds = messageIds;
	}

	public MessageGroupMetadata(MessageGroup messageGroup) {
		this();
		Assert.notNull(messageGroup, "'messageGroup' must not be null");
		for (Message<?> message : messageGroup.getMessages()) {
			UUID id = message.getHeaders().getId();
			Assert.notNull(id, "Message 'id' must not be null");
			this.messageIds.add(id);
		}
		this.complete = messageGroup.isComplete();
		this.timestamp = messageGroup.getTimestamp();
		this.lastReleasedMessageSequenceNumber = messageGroup.getLastReleasedMessageSequenceNumber();
		this.lastModified = messageGroup.getLastModified();
	}

	public void remove(UUID messageId) {
		this.messageIds.remove(messageId);
	}

	public void removeAll(Collection<UUID> messageIds) {
		this.messageIds.removeAll(messageIds);
	}

	boolean add(UUID messageId) {
		return !this.messageIds.contains(messageId) && this.messageIds.add(messageId);
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public Iterator<UUID> messageIdIterator() {
		return this.messageIds.iterator();
	}

	public int size() {
		return this.messageIds.size();
	}

	public @Nullable UUID firstId() {
		if (!this.messageIds.isEmpty()) {
			return this.messageIds.get(0);
		}
		return null;
	}

	/**
	 * Obtain a {@link LinkedList} copy of the {@link #messageIds}
	 * stored in the group.
	 * @return the list of messages ids stored in the group
	 */
	public List<UUID> getMessageIds() {
		return new LinkedList<>(this.messageIds);
	}

	public void complete() {
		this.complete = true;
	}

	public boolean isComplete() {
		return this.complete;
	}

	public long getLastModified() {
		return this.lastModified;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public int getLastReleasedMessageSequenceNumber() {
		return this.lastReleasedMessageSequenceNumber;
	}

	public void setLastReleasedMessageSequenceNumber(int lastReleasedMessageSequenceNumber) {
		this.lastReleasedMessageSequenceNumber = lastReleasedMessageSequenceNumber;
	}

	public @Nullable String getCondition() {
		return this.condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

}
