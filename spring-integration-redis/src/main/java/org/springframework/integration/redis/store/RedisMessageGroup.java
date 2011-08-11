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
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + this.groupId.toString());
		BoundListOperations<String, Object> markedOps = this.redisTemplate.boundListOps(MARKED_PREFIX + this.groupId.toString());
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
		BoundListOperations<String, Object> mGroupOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + this.groupId.toString());
		long usize = mGroupOps.size();
		mGroupOps = this.redisTemplate.boundListOps(MARKED_PREFIX + this.groupId.toString());
		long msize = mGroupOps.size();
		return (int)(usize + msize);
	}

	public Message<?> getOne() {
		Message<?> one = unmarked.get(0);
		if (one == null) {
			one = marked.get(0);
		}
		return one;
	}

	public long getTimestamp() {
		return this.timestamp;
	}
	
	protected void markAll() {
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + this.groupId.toString());
		BoundListOperations<String, Object> markedOps = this.redisTemplate.boundListOps(MARKED_PREFIX + this.groupId.toString());
		long uSize = unmarkedOps.size();
		String destiinationKey = MARKED_PREFIX + this.groupId.toString();
		
		synchronized (lock) {
			unmarkedOps.rename(destiinationKey);
			this.unmarked.clear();
			this.rebuildLocalCache(null, markedOps);
		}
		
		long mSize = markedOps.size();
		Assert.isTrue(mSize == uSize, "Failed to mark All messages in the message group");	
	}
	
	protected void markMessage(String messageId) {
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + this.groupId.toString());
		List<Object> messageIds = unmarkedOps.range(0, unmarkedOps.size()-1);

		synchronized (lock) {
			for (Object id : messageIds) {
				if (messageId.equals(id)){
					BoundListOperations<String, Object> markedOps = this.redisTemplate.boundListOps(MARKED_PREFIX + this.groupId.toString());
					markedOps.rightPush(id);
					System.out.println(unmarkedOps.size());
					unmarkedOps.remove(0, id);
					System.out.println(unmarkedOps.size());
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
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + this.groupId.toString());
		synchronized (lock) {
			unmarkedOps.rightPush(messageId);
			this.messageStore.addMessage(message);
			this.rebuildLocalCache(unmarkedOps, null);
		}	
	}
	
	protected void remove(Message<?> message) {
		UUID messageId = message.getHeaders().getId();
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + this.groupId.toString());
		BoundListOperations<String, Object> markedOps = this.redisTemplate.boundListOps(MARKED_PREFIX + this.groupId.toString());
		
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
		BoundListOperations<String, Object> mGroupListOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + this.groupId.toString());
		List<Object> messageIds =  mGroupListOps.range(0, mGroupListOps.size()-1);
		for (Object messageId : messageIds) {
			this.messageStore.removeMessage(UUID.fromString(messageId.toString()));
		}
		this.redisTemplate.delete(UNMARKED_PREFIX + this.groupId.toString());
		
		mGroupListOps = this.redisTemplate.boundListOps(MARKED_PREFIX + this.groupId.toString());
		messageIds =  mGroupListOps.range(0, mGroupListOps.size()-1);
		for (Object messageId : messageIds) {
			this.messageStore.removeMessage(UUID.fromString(messageId.toString()));
		}
		this.redisTemplate.delete(MARKED_PREFIX + this.groupId.toString());
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
					
					BoundListOperations<String, Object> mGroupOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + this.groupId.toString());
					Collection<Message<?>> unmarked = this.buildMessageList(mGroupOps);
					mGroupOps = this.redisTemplate.boundListOps(MARKED_PREFIX + this.groupId.toString());
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
