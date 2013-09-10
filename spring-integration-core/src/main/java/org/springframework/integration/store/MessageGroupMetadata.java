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
 * @since 2.1
 */
public class MessageGroupMetadata implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private final Object groupId;
	
	private final List<UUID> messageIds = new LinkedList<UUID>();

	private final boolean complete;

	private final long timestamp;
	
	private volatile long lastModified;

	private final int lastReleasedMessageSequenceNumber;

	public MessageGroupMetadata(MessageGroup messageGroup) {
		
		Assert.notNull(messageGroup, "'messageGroup' must not be null");
		this.groupId = messageGroup.getGroupId();
		for (Message<?> message : messageGroup.getMessages()) {
			this.messageIds.add(message.getHeaders().getId());
		}
		this.complete = messageGroup.isComplete();
		this.timestamp = messageGroup.getTimestamp();
		this.lastReleasedMessageSequenceNumber = messageGroup.getLastReleasedMessageSequenceNumber();
		this.lastModified = messageGroup.getLastModified();
	}

	public void remove(UUID messageId){
		this.messageIds.remove(messageId);
	}
	
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public Object getGroupId() {
		return this.groupId;
	}
	
	public Iterator<UUID> messageIdIterator(){
		return this.messageIds.iterator();
	}
	
	public int size(){
		return this.messageIds.size();
	}
	
	public UUID firstId(){
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
