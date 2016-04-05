/*
 * Copyright 2007-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.redis.store;

import java.util.Collection;
import java.util.Set;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.store.AbstractKeyValueMessageStore;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.util.Assert;

/**
 * Redis implementation of the key/value style {@link MessageStore} and {@link MessageGroupStore}
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.1
 */
public class RedisMessageStore extends AbstractKeyValueMessageStore {

	private final RedisTemplate<Object, Object> redisTemplate;

	public RedisMessageStore(RedisConnectionFactory connectionFactory) {
		this.redisTemplate = new RedisTemplate<Object, Object>();
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.setKeySerializer(new StringRedisSerializer());
		this.redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		this.redisTemplate.afterPropertiesSet();
	}

	public void setValueSerializer(RedisSerializer<?> valueSerializer) {
		Assert.notNull(valueSerializer, "'valueSerializer' must not be null");
		this.redisTemplate.setValueSerializer(valueSerializer);
	}

	@Override
	protected Object doRetrieve(Object id) {
		Assert.notNull(id, "'id' must not be null");
		BoundValueOperations<Object, Object> ops = this.redisTemplate.boundValueOps(id);
		return ops.get();
	}


	@Override
	protected void doStore(Object id, Object objectToStore) {
		Assert.notNull(id, "'id' must not be null");
		Assert.notNull(objectToStore, "'objectToStore' must not be null");
		BoundValueOperations<Object, Object> ops = this.redisTemplate.boundValueOps(id);
		try {
			ops.set(objectToStore);
		}
		catch (SerializationException e) {
			throw new IllegalArgumentException("If relying on the default RedisSerializer (JdkSerializationRedisSerializer) " +
					"the Object must be Serializable. Either make it Serializable or provide your own implementation of " +
					"RedisSerializer via 'setValueSerializer(..)'", e);
		}
	}


	@Override
	protected Object doRemove(Object id) {
		Assert.notNull(id, "'id' must not be null");
		Object removedObject = this.doRetrieve(id);
		if (removedObject != null) {
			this.redisTemplate.delete(id);
		}
		return removedObject;
	}


	@Override
	protected Collection<?> doListKeys(String keyPattern) {
		Assert.hasText(keyPattern, "'keyPattern' must not be empty");
		Set<Object> keys = this.redisTemplate.keys(keyPattern);
		return keys;
	}
}
