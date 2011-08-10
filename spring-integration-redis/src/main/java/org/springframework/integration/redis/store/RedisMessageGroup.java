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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.Message;
import org.springframework.integration.store.MessageGroup;
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
	
	public RedisMessageGroup(RedisTemplate<String, Object> redisTemplate,  Object groupId){
		this.groupId = groupId;
		this.redisTemplate = redisTemplate;
	}

	public boolean canAdd(Message<?> message) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void add(Message<?> message) {
		BoundSetOperations<String, Object> mGroupOps = this.redisTemplate.boundSetOps(UNMARKED_PREFIX + this.groupId.toString());
		String key = message.getHeaders().getId().toString();
		mGroupOps.add(key);
		BoundValueOperations<String, Object> bvOps = this.redisTemplate.boundValueOps(key);
		bvOps.set(message);
	}
	
	public void destroy(){
		// delete unmarked
		BoundSetOperations<String, Object> mGroupOps = this.redisTemplate.boundSetOps(UNMARKED_PREFIX + this.groupId.toString());
		for (Object messageId : mGroupOps.members()) {
			this.redisTemplate.delete(messageId.toString());
		}
		this.redisTemplate.delete(UNMARKED_PREFIX + this.groupId.toString());
		// delete marked
		mGroupOps = this.redisTemplate.boundSetOps(MARKED_PREFIX + this.groupId.toString());
		for (Object messageId : mGroupOps.members()) {
			this.redisTemplate.delete(messageId.toString());
		}
		this.redisTemplate.delete(MARKED_PREFIX + this.groupId.toString());
	}
	
	public void markAll() {
		
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

	/* (non-Javadoc)
	 * @see org.springframework.integration.store.MessageGroup#getOne()
	 */
	public Message<?> getOne() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.store.MessageGroup#getTimestamp()
	 */
	public long getTimestamp() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private Collection<Message<?>> getMessages(BoundSetOperations<String, Object> mGroupOps){
		List<Message<?>> messages = new ArrayList<Message<?>>();
		for (Object messageId : mGroupOps.members()) {
			BoundValueOperations<String, Object> bvOps = this.redisTemplate.boundValueOps(messageId.toString());
			Object message = bvOps.get();
			Assert.isInstanceOf(Message.class, message, "Retrieved object is not an instance of Message");
			messages.add((Message<?>) message);
		}
		return messages;
	}

}
