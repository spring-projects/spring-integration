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
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.integration.Message;
import org.springframework.util.Assert;

/**
 * Immutable Value Object holding metadata about a MessageGroup.
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class MessageGroupMetadata implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private final static String CREATED_DATE = "CREATED_DATE";

	private final Object groupId;

	private final TreeMap<Long, UUID> messageCreationDateToIdMappings;

	private final boolean complete;

	private final long timestamp;
	
	private volatile long lastModified;

	private final int lastReleasedMessageSequenceNumber;

	public MessageGroupMetadata(MessageGroup messageGroup) {
		
		Assert.notNull(messageGroup, "'messageGroup' must not be null");
		this.groupId = messageGroup.getGroupId();
		this.messageCreationDateToIdMappings = new TreeMap<Long, UUID>();

		for (Message<?> message : messageGroup.getMessages()) {
			Long createdDate = (Long) message.getHeaders().get(CREATED_DATE);
			Assert.notNull(createdDate > 0,  CREATED_DATE  + " must not be null");
			if (this.messageCreationDateToIdMappings.containsKey(createdDate)){
				createdDate += 1;
			}
			this.messageCreationDateToIdMappings.put(createdDate, message.getHeaders().getId());
		}
		this.complete = messageGroup.isComplete();
		this.timestamp = messageGroup.getTimestamp();
		this.lastReleasedMessageSequenceNumber = messageGroup.getLastReleasedMessageSequenceNumber();
		this.lastModified = messageGroup.getLastModified();
	}

	public void remove(UUID messageId){
		long currentTimestamp = 0;
		for (Entry<Long, UUID> entry : messageCreationDateToIdMappings.entrySet()) {
			if (entry.getValue().equals(messageId)){
				currentTimestamp = entry.getKey();
				break;
			}
		}
		this.messageCreationDateToIdMappings.remove(currentTimestamp);
	}
	
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public Object getGroupId() {
		return this.groupId;
	}
	
	public Iterator<UUID> messageIdIterator(){
		return this.messageCreationDateToIdMappings.values().iterator();
	}
	
	public UUID firstId(){
		Entry<Long, UUID> firstEntry = messageCreationDateToIdMappings.firstEntry();
		if (firstEntry != null){
			return firstEntry.getValue();
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
