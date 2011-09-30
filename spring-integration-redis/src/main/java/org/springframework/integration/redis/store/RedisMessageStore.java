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

import java.util.HashMap;
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
import org.springframework.integration.store.AbstractKeyValueMessageStore;
import org.springframework.integration.store.MessageGroupMetadata;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.util.Assert;

/**
 * An implementation of both the {@link MessageStore} and {@link MessageGroupStore}
 * strategies that relies upon Redis for persistence.
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class RedisMessageStore extends AbstractKeyValueMessageStore {

	private final RedisTemplate<String, Object> redisTemplate;

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
	
	protected void storeHolderMap(String key, Object value){
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
	
	@Override
	@SuppressWarnings("unchecked")
	protected Map<UUID, Message<?>> getHolderMapForMessage(){
		BoundValueOperations<String, Object> ops = redisTemplate.boundValueOps(MESSAGES_HOLDER_MAP_NAME);	
		if (!this.redisTemplate.hasKey(MESSAGES_HOLDER_MAP_NAME)){
			this.storeHolderMap(MESSAGES_HOLDER_MAP_NAME, new HashMap<UUID, Message<?>>());
		}
		Object result = ops.get();
		return (Map<UUID, Message<?>>) result;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected Map<Object, MessageGroupMetadata> getHolderMapForMessageGroups(){
		BoundValueOperations<String, Object> ops = redisTemplate.boundValueOps(MESSAGE_GROUPS_HOLDER_MAP_NAME);	
		if (!this.redisTemplate.hasKey(MESSAGE_GROUPS_HOLDER_MAP_NAME)){
			this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP_NAME, new HashMap<String, MessageGroupMetadata>());
		}
		Object result = ops.get();
		return (Map<Object, MessageGroupMetadata>) result;
	}

}
