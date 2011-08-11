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
package org.springframework.integration.redis.store;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.Message;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageStore;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @since 2.1
 */
class RedisMessageGroup implements MessageGroup {
	private final String MARKED_PREFIX = "MARKED_";
	private final String UNMARKED_PREFIX = "UNMARKED_";
	
	private final String unmarkedId;
	private final String markedId;
	
	private final List<Message<?>> unmarked = new LinkedList<Message<?>>();
	private final List<Message<?>> marked = new LinkedList<Message<?>>();
	
	private final Object groupId;
	private final RedisTemplate<String, Object> redisTemplate;
	private final MessageStore messageStore;
	private final Object lock = new Object();
	private final long timestamp = System.currentTimeMillis();
	
	public RedisMessageGroup(MessageStore messageStore, RedisTemplate<String, Object> redisTemplate,  Object groupId){
		this.groupId = groupId;
		this.redisTemplate = redisTemplate;
		this.messageStore = messageStore;
		this.unmarkedId = UNMARKED_PREFIX + groupId;
		this.markedId = MARKED_PREFIX + groupId;
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(unmarkedId);
		BoundListOperations<String, Object> markedOps = this.redisTemplate.boundListOps(markedId);
		this.rebuildLocalCache(unmarkedOps, markedOps);
	}

	public boolean canAdd(Message<?> message) {
		return !isMember(message);
	}
	
	public Collection<Message<?>> getUnmarked() {
		return this.unmarked;
	}

	
	public Collection<Message<?>> getMarked() {
		return this.marked;
	}

	
	public Object getGroupId() {
		return this.groupId;
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

	public int size() {
		synchronized (lock) {
			return marked.size() + unmarked.size();
		}
	}

	public Message<?> getOne() {
		synchronized (lock) {
			Message<?> one = unmarked.get(0);
			if (one == null) {
				one = marked.get(0);
			}
			return one;
		}
	}

	public long getTimestamp() {
		return this.timestamp;
	}
	
	protected void markAll() {
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(unmarkedId);
		BoundListOperations<String, Object> markedOps = this.redisTemplate.boundListOps(markedId);
		long uSize = unmarkedOps.size();
		
		synchronized (lock) {
			unmarkedOps.rename(markedId);
			this.unmarked.clear();
			this.rebuildLocalCache(null, markedOps);
		}
		
		long mSize = markedOps.size();
		Assert.isTrue(mSize == uSize, "Failed to mark All messages in the message group");	
	}
	
	protected void markMessage(String messageId) {
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(unmarkedId);
		List<Object> messageIds = unmarkedOps.range(0, unmarkedOps.size()-1);

		synchronized (lock) {
			for (Object id : messageIds) {
				if (messageId.equals(id)){
					BoundListOperations<String, Object> markedOps = this.redisTemplate.boundListOps(markedId);
					markedOps.rightPush(id);
					unmarkedOps.remove(0, id);
					this.rebuildLocalCache(unmarkedOps, markedOps);
					return;
				}
			}
		}
	}
	
	/**
	 * Will add message to this MessageGroup. This particular implementation will first add the Message ID to the Redis set of 
	 * Unmarked IDs and than will use the underlying MessageStore to add the actual Message.
	 */
	protected void add(Message<?> message) {
		String messageId = message.getHeaders().getId().toString();
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(unmarkedId);
		synchronized (lock) {
			unmarkedOps.rightPush(messageId);
			this.messageStore.addMessage(message);
			this.rebuildLocalCache(unmarkedOps, null);
		}	
	}
	
	protected void remove(Message<?> message) {
		UUID messageId = message.getHeaders().getId();
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(unmarkedId);
		BoundListOperations<String, Object> markedOps = this.redisTemplate.boundListOps(markedId);
		
		synchronized (lock) {
			unmarkedOps.remove(0, messageId.toString());		
			markedOps.remove(0, messageId.toString());
			this.messageStore.removeMessage(messageId);
			this.rebuildLocalCache(unmarkedOps, markedOps);
		}
		
	}
	/**
	 * 
	 */
	protected void destroy(){
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(unmarkedId);
		List<Object> messageIds =  unmarkedOps.range(0, unmarkedOps.size()-1);
		for (Object messageId : messageIds) {
			this.messageStore.removeMessage(UUID.fromString(messageId.toString()));
		}
		this.redisTemplate.delete(unmarkedId);
		
		BoundListOperations<String, Object> markedOps = this.redisTemplate.boundListOps(markedId);
		messageIds =  markedOps.range(0, markedOps.size()-1);
		for (Object messageId : messageIds) {
			this.messageStore.removeMessage(UUID.fromString(messageId.toString()));
		}
		this.redisTemplate.delete(markedId);
		this.rebuildLocalCache(unmarkedOps, markedOps);
	}
	
	private Collection<Message<?>> buildMessageList(BoundListOperations<String, Object> mGroupOps){
		List<Message<?>> messages = new LinkedList<Message<?>>();
		List<Object> messageIds = mGroupOps.range(0, mGroupOps.size()-1);
		for (Object messageId : messageIds) {
			Message<?> message = this.messageStore.getMessage(UUID.fromString(messageId.toString()));
			messages.add((Message<?>) message);
		}
		return messages;
	}
	
	private boolean isMember(Message<?> message){
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
				synchronized (lock) {
					
					BoundListOperations<String, Object> mGroupOps = this.redisTemplate.boundListOps(unmarkedId);
					Collection<Message<?>> unmarked = this.buildMessageList(mGroupOps);
					mGroupOps = this.redisTemplate.boundListOps(markedId);
					Collection<Message<?>> marked = this.buildMessageList(mGroupOps);
					if (containsSequenceNumber(unmarked, messageSequenceNumber)
							|| containsSequenceNumber(marked, messageSequenceNumber)) {
						return true;
					}
				}
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
	
	private void rebuildLocalCache(BoundListOperations<String, Object> unmarkedOps, BoundListOperations<String, Object> markedOps){
		
		if (unmarkedOps != null){
			this.unmarked.clear();
			this.unmarked.addAll(this.buildMessageList(unmarkedOps));
		}
		if (markedOps != null){
			this.marked.clear();
			this.marked.addAll(this.buildMessageList(markedOps));
		}
	}

}
