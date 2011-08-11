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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.integration.Message;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.MessageStoreException;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @since 2.1
 *
 */
public class RedisMessageStore extends AbstractMessageGroupStore implements MessageStore, InitializingBean{
	
	private final String MESSAGE_GROUPS_KEY = "MESSAGE_GROUPS";

	private final RedisTemplate<String, Object> redisTemplate;
	
	private final Map<Object, RedisMessageGroup> messageGroups = new HashMap<Object, RedisMessageGroup>();
	
	private volatile RedisSerializer<?> valueSerializer = new JdkSerializationRedisSerializer();
	
	private final Object lock = new Object();
	
	public RedisMessageStore(RedisConnectionFactory connectionFactory){
		this.redisTemplate = new RedisTemplate<String, Object>();
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.setKeySerializer(new KeySerializer());	
		this.redisTemplate.setValueSerializer(this.valueSerializer);	
	}

	public Message<?> getMessage(final UUID id) {
		Assert.notNull(id, "'id' must not be null");
		if (this.keyExists(id)){
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
			return message;
		}
		else {
			throw new IllegalArgumentException("Message with id '" + id + "' can not be removed since it does NOT exist");
		}
	}

	
	public int getMessageCount() {
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
	
	private boolean keyExists(final UUID id){
		return redisTemplate.execute(new RedisCallback<Boolean>() {
			public Boolean doInRedis(RedisConnection connection)
					throws DataAccessException {
				return connection.exists(id.toString().getBytes());
			}
		});
	}
	
	private static class KeySerializer implements RedisSerializer<String> {

		public byte[] serialize(String value) throws SerializationException {
			return value.getBytes();
		}

		public String deserialize(byte[] bytes) throws SerializationException {
			return new String(bytes);
		}
		
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.valueSerializer, "'valueSerializer' must not be null");
	}
	/**
	 * 
	 */
	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		RedisMessageGroup group = this.messageGroups.get(groupId);
		if (group == null){
			synchronized (lock) {
				BoundSetOperations<String, Object> mGroupsOps = this.redisTemplate.boundSetOps(MESSAGE_GROUPS_KEY);
				if (!mGroupsOps.isMember(groupId)){
					mGroupsOps.add(groupId);
				}
				group = new RedisMessageGroup(this, this.redisTemplate, groupId);
				this.messageGroups.put(groupId, group);
			}
		}
		
		return group;
	}
	/**
	 * 
	 */
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");	
		Assert.notNull(message, "'message' must not be null");
		
		MessageGroup mg = this.getMessageGroup(groupId);
		
		Assert.isInstanceOf(RedisMessageGroup.class, mg, "MessageGroup is not an instance of RedisMessageGroup");
		((RedisMessageGroup)mg).add(message);
		return mg;
	}

	public MessageGroup markMessageGroup(MessageGroup group) {
		Assert.isInstanceOf(RedisMessageGroup.class, group, "MessageGroup is not an instance of RedisMessageGroup");
		((RedisMessageGroup)group).markAll();
		return group;
	}

	public MessageGroup removeMessageFromGroup(Object key,
			Message<?> messageToRemove) {
		RedisMessageGroup messageGroup = (RedisMessageGroup) this.getMessageGroup(key);
		messageGroup.remove(messageToRemove);
		return messageGroup;
	}

	public MessageGroup markMessageFromGroup(Object key,
			Message<?> messageToMark) {
		RedisMessageGroup messageGroup = (RedisMessageGroup) this.getMessageGroup(key);
		messageGroup.markMessage(messageToMark.getHeaders().getId().toString());
		return messageGroup;
	}

	public void removeMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		RedisMessageGroup messageGroup = (RedisMessageGroup) this.getMessageGroup(groupId);
		synchronized (messageGroup) {
			messageGroup.destroy();
			BoundSetOperations<String, Object> mGroupsOps = this.redisTemplate.boundSetOps(MESSAGE_GROUPS_KEY);
			mGroupsOps.remove(groupId);
			this.redisTemplate.delete(groupId.toString());
			this.messageGroups.remove(groupId);
		}
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		BoundSetOperations<String, Object> mGroupsOps = this.redisTemplate.boundSetOps(MESSAGE_GROUPS_KEY);
		List<MessageGroup> messageGroups = new ArrayList<MessageGroup>();
		for (Object msgGroupId : mGroupsOps.members()) {
			messageGroups.add(new RedisMessageGroup(this, this.redisTemplate, msgGroupId));
		}
		return messageGroups.iterator();
	}
}
