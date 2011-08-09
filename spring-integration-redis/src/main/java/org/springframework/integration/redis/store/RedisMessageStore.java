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

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.store.MessageStore;

/**
 * @author Oleg Zhurakousky
 * @since 2.1
 *
 */
public class RedisMessageStore implements MessageStore {

	private final RedisTemplate<UUID, Message<?>> redisTemplate;
	
	public RedisMessageStore(RedisConnectionFactory connectionFactory){
		this.redisTemplate = new RedisTemplate<UUID, Message<?>>();
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.setKeySerializer(new JacksonJsonRedisSerializer<UUID>(UUID.class));	
	}
	
	public Message<?> getMessage(final UUID id) {
		boolean exists = redisTemplate.execute(new RedisCallback<Boolean>() {

			public Boolean doInRedis(RedisConnection connection)
					throws DataAccessException {
				return connection.exists(id.toString().getBytes());
			}
		});
		if (!exists){
			return null;
		}
		else {
			BoundValueOperations<UUID, Message<?>> ops = redisTemplate.boundValueOps(id);		
			return ops.get();
		}	
	}

	
	public <T> Message<T> addMessage(Message<T> message) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Message<?> removeMessage(UUID id) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public int getMessageCount() {
		// TODO Auto-generated method stub
		return 0;
	}

}
