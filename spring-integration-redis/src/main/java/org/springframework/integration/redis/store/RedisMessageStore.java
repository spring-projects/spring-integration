/*
 * Copyright 2007-2011 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.redis.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.MessageStoreException;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @since 2.1
 *
 */
public class RedisMessageStore extends AbstractMessageGroupStore implements MessageStore, InitializingBean{
	
	private final String MESSAGE_GROUPS_KEY = "MESSAGE_GROUPS";
	private final String MARKED_PREFIX = "MARKED_";
	private final String UNMARKED_PREFIX = "UNMARKED_";
	
	private final RedisTemplate<String, Object> redisTemplate;
	
	private volatile RedisSerializer<?> valueSerializer = new JdkSerializationRedisSerializer();
	
	public RedisMessageStore(RedisConnectionFactory connectionFactory){
		this.redisTemplate = new RedisTemplate<String, Object>();
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.setKeySerializer(new StringRedisSerializer());	
		this.redisTemplate.setValueSerializer(this.valueSerializer);	
	}

	public Message<?> getMessage(final UUID id) {
		Assert.notNull(id, "'id' must not be null");
		if (this.redisTemplate.hasKey(id.toString())){
			BoundValueOperations<String, Object> ops = redisTemplate.boundValueOps(id.toString());	
			Object result = ops.get();
			Assert.isInstanceOf(Message.class, result, "Return value is not an instace of Message");
			return (Message<?>) result;
		}
		return null;	
	}

	
	@SuppressWarnings("unchecked")
	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		BoundValueOperations<String, Object> ops = redisTemplate.boundValueOps(message.getHeaders().getId().toString());	
		try {
			ops.set(message);
		} catch (SerializationException e) {
			throw new MessageStoreException(message, "It seems like while relying on the default RedisSerializer (JdkSerializationRedisSerializer) " +
					"the Message contains data that is not Serializable. Either make it Serializable or provide your own implementation of " +
					"RedisSerializer via 'setValueSerializer(..)'", e);
		}
		Object result = ops.get();
		Assert.isInstanceOf(Message.class, result, "Return value is not an instace of Message");
		return (Message<T>) result;
	}

	
	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Message<?> message = this.getMessage(id);
		if (message != null){
			this.redisTemplate.delete(id.toString());
			
		}
		return message;
	}

	@ManagedAttribute
	public long getMessageCount() {
		return redisTemplate.execute(new RedisCallback<Integer>() {
			public Integer doInRedis(RedisConnection connection)
					throws DataAccessException {
				long l = connection.dbSize();
				Assert.isTrue(l <= Integer.MAX_VALUE, "Message count is out of range");
				return (int)l;
			}
		});
	}
	
	public void setValueSerializer(RedisSerializer<?> valueSerializer) {
		this.valueSerializer = valueSerializer;
	}
	
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.valueSerializer, "'valueSerializer' must not be null");
	}
	
	// MESSAGE GROUP methods
	/**
	 * Will create a new instance of SimpleMessageGroup initializing it with
	 * data collected from Redis Message Store
	 */
	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		long timestamp = System.currentTimeMillis();

		Collection<Message<?>> unmarkedMessages = this.buildMessageList(this.redisTemplate.boundListOps(UNMARKED_PREFIX + groupId));
		Collection<Message<?>> markedMessages = this.buildMessageList(this.redisTemplate.boundListOps(MARKED_PREFIX + groupId));
		this.doMessageGroupCreate(groupId);
		
		return new SimpleMessageGroup(unmarkedMessages, markedMessages, groupId, timestamp);
	}

	/**
	 * 
	 */
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");
		
		synchronized (groupId) {
			this.doAddMessageToGroup(message, groupId);
			this.addMessage(message);
			return this.getMessageGroup(groupId);
		}
		
	}
	
	/**
	 * 
	 */
	public MessageGroup markMessageGroup(MessageGroup group) {
		Assert.notNull(group, "'group' must not be null");
		Object groupId = group.getGroupId();
		
		synchronized (groupId) {				
			this.doMarkMessageGroup(groupId);
			return this.getMessageGroup(groupId);
		}	
	}

	/**
	 * 
	 */
	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");
		
		UUID messageId = messageToRemove.getHeaders().getId();
		synchronized (groupId) {
			this.doRemoveMessageFromGroup(groupId, messageId);
			this.removeMessage(messageId);
			return this.getMessageGroup(groupId);
		}	
	}

	/**
	 * 
	 */
	public MessageGroup markMessageFromGroup(Object groupId, Message<?> messageToMark) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToMark, "'messageToMark' must not be null");
		
		String strMessageId = messageToMark.getHeaders().getId().toString();
	
		synchronized (groupId) {
			this.doMarkMessageFromGroup(strMessageId, groupId);		
			return this.getMessageGroup(groupId);
		}	
	}

	/**
	 * 
	 */
	public void removeMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		synchronized (groupId) {
			this.doRemoveMessageGroup(groupId);
		}	
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		BoundSetOperations<String, Object> mGroupsOps = this.redisTemplate.boundSetOps(MESSAGE_GROUPS_KEY);
		Set<Object> messageGroupIds = mGroupsOps.members();
		List<MessageGroup> messageGroups = new ArrayList<MessageGroup>();
		for (Object messageGroupId : messageGroupIds) {
			messageGroups.add(this.getMessageGroup(messageGroupId));
		}
		return messageGroups.iterator();
	}
	
	private Collection<Message<?>> buildMessageList(BoundListOperations<String, Object> mGroupOps){
		List<Message<?>> messages = new LinkedList<Message<?>>();
		if (mGroupOps.size() == 0){
			return Collections.emptyList();
		}
		List<Object> messageIds = mGroupOps.range(0, mGroupOps.size()-1);
		for (Object messageId : messageIds) {
			Message<?> message = this.getMessage(UUID.fromString(messageId.toString()));
			if (message != null){
				messages.add((Message<?>) message);
			}		
		}
		return messages;
	}
	
	// *** candidates for future abstract methods
	
	private void doMessageGroupCreate(Object groupId){
		BoundSetOperations<String, Object> mGroupsOps = this.redisTemplate.boundSetOps(MESSAGE_GROUPS_KEY);
		if (!mGroupsOps.members().contains(groupId)){
			mGroupsOps.add(groupId);
		}	
	}
	
	private void doAddMessageToGroup(Message<?> message, Object groupId){
		String messageId = message.getHeaders().getId().toString();
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + groupId);
		unmarkedOps.rightPush(messageId);
	}
	
	private void doMarkMessageGroup(Object groupId){
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + groupId);
		unmarkedOps.rename(MARKED_PREFIX + groupId);		
	}
	
	private void doRemoveMessageFromGroup(Object groupId, UUID messageId){
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + groupId);
		BoundListOperations<String, Object> markedOps = this.redisTemplate.boundListOps(MARKED_PREFIX + groupId);
		
		unmarkedOps.remove(0, messageId.toString());		
		markedOps.remove(0, messageId.toString());
	}
	
	private void doMarkMessageFromGroup(String strMessageId, Object groupId){
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + groupId);
		if (unmarkedOps.size() > 0){
			List<Object> messageIds = unmarkedOps.range(0, unmarkedOps.size()-1);
			int objectIndex = messageIds.indexOf(strMessageId);
			if (objectIndex > -1){
				BoundListOperations<String, Object> markedOps = this.redisTemplate.boundListOps(MARKED_PREFIX + groupId);
				markedOps.rightPush(strMessageId);
				unmarkedOps.remove(0, strMessageId);		
			}
		}
	}
	
	private void doRemoveMessageGroup(Object groupId){
		BoundListOperations<String, Object> unmarkedOps = this.redisTemplate.boundListOps(UNMARKED_PREFIX + groupId);
		if (unmarkedOps.size() > 0){
			List<Object> messageIds =  unmarkedOps.range(0, unmarkedOps.size()-1);
			for (Object messageId : messageIds) {
				this.removeMessage(UUID.fromString(messageId.toString()));
			}
			this.redisTemplate.delete(UNMARKED_PREFIX + groupId);
			
			BoundListOperations<String, Object> markedOps = this.redisTemplate.boundListOps(MARKED_PREFIX + groupId);
			messageIds =  markedOps.range(0, markedOps.size()-1);
			for (Object messageId : messageIds) {
				this.removeMessage(UUID.fromString(messageId.toString()));
			}
			this.redisTemplate.delete(MARKED_PREFIX + groupId);
			BoundSetOperations<String, Object> mGroupsOps = this.redisTemplate.boundSetOps(MESSAGE_GROUPS_KEY);
			mGroupsOps.remove(groupId);
		}	
	}
}
