/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.store;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Immutable Value Object holding metadata about a MessageGroup.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.1
 */
public class MessageGroupMetadata implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Object groupId;

	private final List<UUID> messageIds = new LinkedList<UUID>();

	private final boolean complete;

	private final long timestamp;

	private volatile long lastModified;

	private final int lastReleasedMessageSequenceNumber;

	private final boolean hasMessages;

	private final int size;

	private final UUID first;

	public MessageGroupMetadata(MessageGroup messageGroup) {
		this(messageGroup, true, null);
	}

	public MessageGroupMetadata(MessageGroup messageGroup, boolean hasMessages, UUID first) {

		Assert.notNull(messageGroup, "'messageGroup' must not be null");
		this.groupId = messageGroup.getGroupId();
		if (hasMessages) {
			for (Message<?> message : messageGroup.getMessages()) {
				this.messageIds.add(message.getHeaders().getId());
			}
			this.size = this.messageIds.size();
		}
		else {
			this.size = messageGroup.size();
		}
		this.complete = messageGroup.isComplete();
		this.timestamp = messageGroup.getTimestamp();
		this.lastReleasedMessageSequenceNumber = messageGroup.getLastReleasedMessageSequenceNumber();
		this.lastModified = messageGroup.getLastModified();
		this.hasMessages = hasMessages;
		this.first = first;
	}

	public void remove(UUID messageId){
		if (!this.hasMessages) {
			throw new IllegalStateException("Messages are not available, fetch the entire group");
		}
		this.messageIds.remove(messageId);
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public Object getGroupId() {
		return this.groupId;
	}

	public Iterator<UUID> messageIdIterator(){
		if (!this.hasMessages) {
			throw new IllegalStateException("Messages are not available, fetch the entire group");
		}
		return this.messageIds.iterator();
	}

	public int size(){
		return this.size;
	}

	public UUID firstId(){
		if (this.first != null) {
			return this.first;
		}
		if (!this.hasMessages) {
			throw new IllegalStateException("Messages are not available, fetch the entire group");
		}
		if (this.messageIds.size() > 0){
			return this.messageIds.iterator().next();
		}
		return null;
	}

	public boolean isComplete() {
		return this.complete;
	}

	public long getLastModified() {
		return lastModified;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public int getLastReleasedMessageSequenceNumber() {
		return this.lastReleasedMessageSequenceNumber;
	}
}
