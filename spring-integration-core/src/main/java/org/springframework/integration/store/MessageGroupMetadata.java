/*
 * Copyright 2002-2013 the original author or authors.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.integration.Message;
import org.springframework.util.Assert;

/**
 * Immutable Value Object holding metadata about a MessageGroup.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.1
 */
public class MessageGroupMetadata implements Serializable{

	private static final long serialVersionUID = 1L;

	private final Object groupId;

	private final Map<UUID, Integer> messageIds = new LinkedHashMap<UUID, Integer>();

	private final boolean complete;

	private final long timestamp;

	private volatile long lastModified;

	private final int lastReleasedMessageSequenceNumber;

	public MessageGroupMetadata(MessageGroup messageGroup) {

		Assert.notNull(messageGroup, "'messageGroup' must not be null");
		this.groupId = messageGroup.getGroupId();
		for (Message<?> message : messageGroup.getMessages()) {
			this.messageIds.put(message.getHeaders().getId(), message.getHeaders().getPriority());
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
		return this.messageIds.keySet().iterator();
	}

	public int size(){
		return this.messageIds.size();
	}

	public UUID firstId(){
		if (this.messageIds.size() > 0){
			return this.messageIds.keySet().iterator().next();
		}

		return null;
	}

	public UUID firstIdByPriority(){
		if (this.messageIds.size() > 0){
			List<Map.Entry<UUID, Integer>> messageEntries = new ArrayList<Map.Entry<UUID, Integer>>(messageIds.entrySet());
			Collections.sort(messageEntries, new Comparator<Map.Entry<UUID, Integer>>() {
				@Override
				public int compare(Map.Entry<UUID, Integer> m1, Map.Entry<UUID, Integer> m2) {
					Integer priority1 = m1.getValue();
					Integer priority2 = m2.getValue();

					priority1 = priority1 != null ? priority1 : 0;
					priority2 = priority2 != null ? priority2 : 0;
					return priority2.compareTo(priority1);
				}
			});
			return messageEntries.get(0).getKey();
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
