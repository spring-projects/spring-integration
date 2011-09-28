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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageGroupWrapper;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.util.Assert;

/**
 * An implementation of both the {@link MessageStore} and {@link MessageGroupStore}
 * strategies that relies upon Redis for persistence.
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class RedisMessageStore extends AbstractMessageGroupStore implements MessageStore {

	private final RedisTemplate<String, Object> redisTemplate;
	
	private static final String MESSAGES_HOLDER_MAP = "MESSAGES";
	
	private static final String MESSAGE_GROUPS_HOLDER_MAP = "MESSAGE_GROUPS";
	

	public RedisMessageStore(RedisConnectionFactory connectionFactory) {
		this.redisTemplate = new RedisTemplate<String, Object>();
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.setKeySerializer(new StringRedisSerializer());
		this.redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
	}


	public void setValueSerializer(RedisSerializer<?> valueSerializer) {
		Assert.notNull(valueSerializer, "'valueSerializer' must not be null");
		this.redisTemplate.setValueSerializer(valueSerializer);
	}

	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Map<String, Object> result = this.getHolderMap(MESSAGES_HOLDER_MAP);
		if (result.containsKey(id.toString())) {
			Message<?> message = (Message<?>) result.get(id.toString());
			return message;
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		UUID messageId = message.getHeaders().getId();
		Map<String, Object> result = this.getHolderMap(MESSAGES_HOLDER_MAP);
		result.put(messageId.toString(), message);
		this.storeHolderMap(MESSAGES_HOLDER_MAP, result);
		return (Message<T>) this.getMessage(messageId);
	}

	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Map<String, Object> result = this.getHolderMap(MESSAGES_HOLDER_MAP);
		Message<?> message = (Message<?>) result.remove(id.toString());
		this.storeHolderMap(MESSAGES_HOLDER_MAP, result);
		return message;
	}

	@ManagedAttribute
	public long getMessageCount() {
		Map<String, Object> result = this.getHolderMap(MESSAGES_HOLDER_MAP);
		return result.size();
	}


	// MESSAGE GROUP methods

	/**
	 * Will create a new instance of SimpleMessageGroup initializing it with
	 * data collected from the Redis Message Store.
	 */
	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Map<String, Object> result = this.getHolderMap(MESSAGE_GROUPS_HOLDER_MAP);
		if (result.containsKey(groupId.toString())){
			MessageGroupWrapper messageGroupWrapper = (MessageGroupWrapper) result.get(groupId.toString());
			ArrayList<Message<?>> markedMessages = new ArrayList<Message<?>>();
			for (UUID uuid : messageGroupWrapper.getMarkedMessageIds()) {
				markedMessages.add(this.getMessage(uuid));
			}
			
			ArrayList<Message<?>> unmarkedMessages = new ArrayList<Message<?>>();
			for (UUID uuid : messageGroupWrapper.getUnmarkedMessageIds()) {
				unmarkedMessages.add(this.getMessage(uuid));
			}
			return new SimpleMessageGroup(unmarkedMessages, markedMessages, 
					groupId, messageGroupWrapper.getTimestamp(), messageGroupWrapper.isComplete());
		}
		else {
			return new SimpleMessageGroup(groupId);
		}
	}

	/**
	 * Add a Message to the group with the provided group ID. 
	 */
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");
		
		Map<String, Object> result = this.getHolderMap(MESSAGE_GROUPS_HOLDER_MAP);
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.add(message);	
		result.put(groupId.toString(), new MessageGroupWrapper(messageGroup));
		
		this.addMessage(message);
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP, result);

		return this.getMessageGroup(groupId);
	}

	/**
	 * Mark all messages in the provided group. 
	 */
	public MessageGroup markMessageGroup(MessageGroup group) {
		Assert.notNull(group, "'group' must not be null");
		Object groupId = group.getGroupId();

		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(group);
		messageGroup.markAll();
		Map<String, Object> result = this.getHolderMap(MESSAGE_GROUPS_HOLDER_MAP);
		result.put(groupId.toString(), new MessageGroupWrapper(messageGroup));
		
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP, result);
		
		return this.getMessageGroup(groupId);
	}

	/**
	 * Remove a Message from the group with the provided group ID. 
	 */
	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");
		
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.remove(messageToRemove);
		Map<String, Object> result = this.getHolderMap(MESSAGE_GROUPS_HOLDER_MAP);
		result.put(groupId.toString(), new MessageGroupWrapper(messageGroup));
		
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP, result);
		
		return this.getMessageGroup(groupId);
	}

	/**
	 * Mark the given Message within the group corresponding to the provided group ID. 
	 */
	public MessageGroup markMessageFromGroup(Object groupId, Message<?> messageToMark) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToMark, "'messageToMark' must not be null");
		
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.mark(messageToMark);
		Map<String, Object> result = this.getHolderMap(MESSAGE_GROUPS_HOLDER_MAP);
		result.put(groupId.toString(), new MessageGroupWrapper(messageGroup));
		
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP, result);
		
		return this.getMessageGroup(groupId);
		
	}
	
	public void completeGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.complete();
		Map<String, Object> result = this.getHolderMap(MESSAGE_GROUPS_HOLDER_MAP);
		result.put(groupId.toString(), new MessageGroupWrapper(messageGroup));
		
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP, result);
	}

	/**
	 * Remove the MessageGroup with the provided group ID. 
	 */
	public void removeMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		
		Map<String, Object> result = this.getHolderMap(MESSAGE_GROUPS_HOLDER_MAP);
		MessageGroupWrapper messageGroupWrapper = (MessageGroupWrapper) result.get(groupId.toString());
		
		for (UUID messageId : messageGroupWrapper.getMarkedMessageIds()) {
			this.removeMessage(messageId);
		}
		
		for (UUID messageId : messageGroupWrapper.getUnmarkedMessageIds()) {
			this.removeMessage(messageId);
		}
		result.remove(groupId.toString());
		
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP, result);
	}

	public Iterator<MessageGroup> iterator() {
		Map<String, Object> result = this.getHolderMap(MESSAGE_GROUPS_HOLDER_MAP);
		List<MessageGroup> messageGroups = new ArrayList<MessageGroup>();
		for (Object object : result.values()) {
			MessageGroupWrapper messageGroupWrapper = (MessageGroupWrapper) object;
			messageGroups.add(this.getMessageGroup(messageGroupWrapper.getGroupId()));
		}
		return messageGroups.iterator();
		
	}
	
	private SimpleMessageGroup getSimpleMessageGroup(MessageGroup messageGroup){
		if (messageGroup instanceof SimpleMessageGroup){
			return (SimpleMessageGroup) messageGroup;
		}
		else {
			return new SimpleMessageGroup(messageGroup);
		}
	}
	
	private void storeHolderMap(String key, Object value){
		BoundValueOperations<String, Object> ops = redisTemplate.boundValueOps(key);
		try {
			ops.set(value);
		}
		catch (SerializationException e) {
			throw new IllegalArgumentException("If relying on the default RedisSerializer (JdkSerializationRedisSerializer) " +
					"the Object must be Serializable. Either make it Serializable or provide your own implementation of " +
					"RedisSerializer via 'setValueSerializer(..)'", e);
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getHolderMap(String key){
		BoundValueOperations<String, Object> ops = redisTemplate.boundValueOps(key);	
		if (!this.redisTemplate.hasKey(key)){
			this.storeHolderMap(key, new HashMap<String, Object>());
		}
		Object result = ops.get();
		return (Map<String, Object>) result;
	}

}
