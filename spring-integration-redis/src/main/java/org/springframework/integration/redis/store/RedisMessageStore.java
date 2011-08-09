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

import java.util.UUID;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.integration.Message;
import org.springframework.integration.store.MessageStore;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @since 2.1
 *
 */
public class RedisMessageStore implements MessageStore, InitializingBean{

	private final RedisTemplate<UUID, Message<?>> redisTemplate;
	
	private volatile RedisSerializer<?> valueSerializer = new JdkSerializationRedisSerializer();
	
	public RedisMessageStore(RedisConnectionFactory connectionFactory){
		this.redisTemplate = new RedisTemplate<UUID, Message<?>>();
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.setKeySerializer(new UuidSerializer());	
		this.redisTemplate.setValueSerializer(this.valueSerializer);	
	}

	public Message<?> getMessage(final UUID id) {
		Assert.notNull(id, "'id' must not be null");
		if (this.messageExists(id)){
			BoundValueOperations<UUID, Message<?>> ops = redisTemplate.boundValueOps(id);		
			return ops.get();
		}
		return null;	
	}

	
	@SuppressWarnings("unchecked")
	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		BoundValueOperations<UUID, Message<?>> ops = redisTemplate.boundValueOps(message.getHeaders().getId());	
		ops.set(message);
		return (Message<T>) ops.get();
	}

	
	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Message<?> message = this.getMessage(id);
		if (message != null){
			this.redisTemplate.delete(id);
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
	
	private boolean messageExists(final UUID id){
		return redisTemplate.execute(new RedisCallback<Boolean>() {
			public Boolean doInRedis(RedisConnection connection)
					throws DataAccessException {
				return connection.exists(id.toString().getBytes());
			}
		});
	}
	
	private static class UuidSerializer implements RedisSerializer<UUID> {

		public byte[] serialize(UUID t) throws SerializationException {
			return t.toString().getBytes();
		}

		public UUID deserialize(byte[] bytes) throws SerializationException {
			return UUID.fromString(new String(bytes));
		}
		
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.valueSerializer, "'valueSerializer' must not be null");
	}
}
