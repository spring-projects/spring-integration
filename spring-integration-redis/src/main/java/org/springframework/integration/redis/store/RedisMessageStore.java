/*
 * Copyright 2007-2017 the original author or authors.
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

import org.springframework.beans.factory.BeanClassLoaderAware;
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
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class RedisMessageStore extends AbstractKeyValueMessageStore implements BeanClassLoaderAware {

	private final RedisTemplate<Object, Object> redisTemplate;

	private boolean valueSerializerSet;

	/**
	 * Construct {@link RedisMessageStore} based on the provided
	 * {@link RedisConnectionFactory} and default empty prefix.
	 * @param connectionFactory the RedisConnectionFactory to use
	 */
	public RedisMessageStore(RedisConnectionFactory connectionFactory) {
		this(connectionFactory, "");
	}

	/**
	 * Construct {@link RedisMessageStore} based on the provided
	 * {@link RedisConnectionFactory} and prefix.
	 * @param connectionFactory the RedisConnectionFactory to use
	 * @param prefix the key prefix to use, allowing the same broker to be used for
	 * multiple stores.
	 * @since 4.3.12
	 * @see AbstractKeyValueMessageStore#AbstractKeyValueMessageStore(String)
	 */
	public RedisMessageStore(RedisConnectionFactory connectionFactory, String prefix) {
		super(prefix);
		this.redisTemplate = new RedisTemplate<Object, Object>();
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.setKeySerializer(new StringRedisSerializer());
		this.redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		this.redisTemplate.afterPropertiesSet();
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.valueSerializerSet) {
			this.redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer(classLoader));
		}
	}

	public void setValueSerializer(RedisSerializer<?> valueSerializer) {
		Assert.notNull(valueSerializer, "'valueSerializer' must not be null");
		this.redisTemplate.setValueSerializer(valueSerializer);
		this.valueSerializerSet = true;
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
			rethrowAsIllegalArgumentException(e);

		}
	}

	@Override
	protected void doStoreIfAbsent(Object id, Object objectToStore) {
		Assert.notNull(id, "'id' must not be null");
		Assert.notNull(objectToStore, "'objectToStore' must not be null");
		BoundValueOperations<Object, Object> ops = this.redisTemplate.boundValueOps(id);
		try {
			Boolean present = ops.setIfAbsent(objectToStore);
			if (present != null && logger.isDebugEnabled()) {
				logger.debug("The message: [" + present + "] is already present in the store. " +
						"The [" + objectToStore + "] is ignored.");
			}
		}
		catch (SerializationException e) {
			rethrowAsIllegalArgumentException(e);
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
		return this.redisTemplate.keys(keyPattern);
	}

	private void rethrowAsIllegalArgumentException(SerializationException e) {
		throw new IllegalArgumentException("If relying on the default RedisSerializer " +
				"(JdkSerializationRedisSerializer) the Object must be Serializable. " +
				"Either make it Serializable or provide your own implementation of " +
				"RedisSerializer via 'setValueSerializer(..)'", e);
	}

}
