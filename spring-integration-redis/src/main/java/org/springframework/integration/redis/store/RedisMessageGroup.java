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
package org.springframework.integration.redis.store;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundSetOperations;
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
	
	private final Object groupId;
	private final RedisTemplate<String, Object> redisTemplate;
	private final MessageStore messageStore;
	
	public RedisMessageGroup(MessageStore messageStore, RedisTemplate<String, Object> redisTemplate,  Object groupId){
		this.groupId = groupId;
		this.redisTemplate = redisTemplate;
		this.messageStore = messageStore;
	}

	public boolean canAdd(Message<?> message) {
		// TODO Auto-generated method stub
		return false;
	}
	/**
	 * Will add message to this MessageGroup. This particular implementation will first add the Message ID to the Redis set of 
	 * Unmarked IDs and than will use the underlying MessageStore to add the actual Message.
	 */
	protected void add(Message<?> message) {
		BoundListOperations<String, Object> mGroupListOps = this.redisTemplate.boundListOps("L" + UNMARKED_PREFIX + this.groupId.toString());
		BoundSetOperations<String, Object> mGroupOps = this.redisTemplate.boundSetOps(UNMARKED_PREFIX + this.groupId.toString());
		String key = message.getHeaders().getId().toString();
		mGroupOps.add(key);
		mGroupListOps.rightPush(key);
		System.out.println("Members: " + mGroupOps.members());
		this.messageStore.addMessage(message);
	}
	
	protected void remove(Message<?> message) {
		BoundSetOperations<String, Object> mGroupOps = this.redisTemplate.boundSetOps(UNMARKED_PREFIX + this.groupId.toString());
		String key = message.getHeaders().getId().toString();
		mGroupOps.remove(key);
		this.messageStore.removeMessage(message.getHeaders().getId());
	}
	/**
	 * 
	 */
	public void destroy(){
		// delete unmarked
		BoundSetOperations<String, Object> mGroupOps = this.redisTemplate.boundSetOps(UNMARKED_PREFIX + this.groupId.toString());
		for (Object messageId : mGroupOps.members()) {
			this.messageStore.removeMessage(UUID.fromString(messageId.toString()));
			//this.redisTemplate.delete(messageId.toString());
		}
		this.redisTemplate.delete(UNMARKED_PREFIX + this.groupId.toString());
		// delete marked
		mGroupOps = this.redisTemplate.boundSetOps(MARKED_PREFIX + this.groupId.toString());
		for (Object messageId : mGroupOps.members()) {
			this.messageStore.removeMessage(UUID.fromString(messageId.toString()));
		}
		this.redisTemplate.delete(MARKED_PREFIX + this.groupId.toString());
	}
	
	public void markAll() {
		BoundSetOperations<String, Object> unmarkedGroupOps = this.redisTemplate.boundSetOps(UNMARKED_PREFIX + this.groupId.toString());
		long uSize = unmarkedGroupOps.size();
		String destiinationKey = MARKED_PREFIX + this.groupId.toString();
		unmarkedGroupOps.rename(destiinationKey);

		BoundSetOperations<String, Object> markedGroupOps = this.redisTemplate.boundSetOps(destiinationKey);
		long mSize = markedGroupOps.size();
		Assert.isTrue(mSize == uSize, "Failed to mark All messages in the message group");
	}
	
	protected void markMessage(String messageId) {
		BoundSetOperations<String, Object> unmarkedGroupOps = this.redisTemplate.boundSetOps(UNMARKED_PREFIX + this.groupId.toString());
		String destinationKey = MARKED_PREFIX + this.groupId.toString();
		unmarkedGroupOps.move(destinationKey, messageId);
	}

	
	public Collection<Message<?>> getUnmarked() {
		BoundSetOperations<String, Object> mGroupOps = this.redisTemplate.boundSetOps(UNMARKED_PREFIX + this.groupId.toString());
		return this.getMessages(mGroupOps);
	}

	
	public Collection<Message<?>> getMarked() {
		BoundSetOperations<String, Object> mGroupOps = this.redisTemplate.boundSetOps(MARKED_PREFIX + this.groupId.toString());
		return this.getMessages(mGroupOps);
	}

	
	public Object getGroupId() {
		return this.groupId;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.store.MessageGroup#isComplete()
	 */
	public boolean isComplete() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.store.MessageGroup#getSequenceSize()
	 */
	public int getSequenceSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	public int size() {
		BoundSetOperations<String, Object> mGroupOps = this.redisTemplate.boundSetOps(UNMARKED_PREFIX + this.groupId.toString());
		return mGroupOps.members().size();
	}

	
	public Message<?> getOne() {
		BoundListOperations<String, Object> mGroupListOps = this.redisTemplate.boundListOps("L" + UNMARKED_PREFIX + this.groupId.toString());
		Object id = mGroupListOps.leftPop();
		return this.messageStore.getMessage(UUID.fromString(id.toString()));
//		List<Message<?>> messages = (List<Message<?>>) this.getUnmarked();
//		if (messages.isEmpty()){
//			messages = (List<Message<?>>) this.getMarked();
//			if (!messages.isEmpty()){
//				return messages.get(0);
//			}
//		}
//		else {
//			return messages.get(0);
//		}
		//return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.store.MessageGroup#getTimestamp()
	 */
	public long getTimestamp() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private Collection<Message<?>> getMessages(BoundSetOperations<String, Object> mGroupOps){
		List<Message<?>> messages = new LinkedList<Message<?>>();
		for (Object messageId : mGroupOps.members()) {
			Message<?> message = this.messageStore.getMessage(UUID.fromString(messageId.toString()));
			messages.add((Message<?>) message);
		}
		return messages;
	}

}
