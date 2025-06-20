/*
 * Copyright 2014-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.redis.store;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupFactory;
import org.springframework.integration.store.SimpleMessageGroupFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Specialized Redis {@link ChannelMessageStore} that uses a list to back a QueueChannel.
 * <p>
 * Requires {@link #setBeanName(String)} which is used as part of the key.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
public class RedisChannelMessageStore
		implements ChannelMessageStore, BeanNameAware, InitializingBean, BeanClassLoaderAware {

	private final RedisTemplate<Object, Message<?>> redisTemplate;

	private String beanName;

	private MessageGroupFactory messageGroupFactory = new SimpleMessageGroupFactory();

	private boolean valueSerializerExplicitlySet;

	/**
	 * Construct a message store that uses Java Serialization for messages.
	 *
	 * @param connectionFactory The redis connection factory.
	 */
	public RedisChannelMessageStore(RedisConnectionFactory connectionFactory) {
		this.redisTemplate = new RedisTemplate<>();
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.setKeySerializer(new StringRedisSerializer());
		this.redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		this.redisTemplate.afterPropertiesSet();
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.valueSerializerExplicitlySet) {
			this.redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer(classLoader));
		}
	}

	/**
	 * Use a different serializer (default {@link JdkSerializationRedisSerializer} for
	 * the {@link Message}.
	 *
	 * @param valueSerializer The value serializer.
	 */
	public void setValueSerializer(RedisSerializer<?> valueSerializer) {
		Assert.notNull(valueSerializer, "'valueSerializer' must not be null");
		this.redisTemplate.setValueSerializer(valueSerializer);
		this.valueSerializerExplicitlySet = true;
	}

	/**
	 * Specify the {@link MessageGroupFactory} to create {@link MessageGroup} object where
	 * it is necessary.
	 * Defaults to {@link SimpleMessageGroupFactory}.
	 * @param messageGroupFactory the {@link MessageGroupFactory} to use.
	 * @since 4.3
	 */
	public void setMessageGroupFactory(MessageGroupFactory messageGroupFactory) {
		Assert.notNull(messageGroupFactory, "'messageGroupFactory' must not be null");
		this.messageGroupFactory = messageGroupFactory;
	}

	protected MessageGroupFactory getMessageGroupFactory() {
		return this.messageGroupFactory;
	}

	@Override
	public void setBeanName(String name) {
		Assert.notNull(name, "'beanName' must not be null");
		this.beanName = name;
	}

	protected String getBeanName() {
		return this.beanName;
	}

	protected RedisTemplate<Object, Message<?>> getRedisTemplate() {
		return this.redisTemplate;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.beanName, "'beanName' must not be null");
	}

	@Override
	@ManagedAttribute
	public int messageGroupSize(Object groupId) {
		Long size = this.redisTemplate.boundListOps(groupId).size();
		return size == null ? 0 : (int) size.longValue();
	}

	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		List<Message<?>> messages = this.redisTemplate.boundListOps(groupId).range(0, -1);
		return getMessageGroupFactory().create(messages, groupId);
	}

	@Override
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		this.redisTemplate.boundListOps(groupId).leftPush(message);
		return null;
	}

	@Override
	public void removeMessageGroup(Object groupId) {
		this.redisTemplate.boundListOps(groupId).trim(1, 0);
	}

	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {
		return this.redisTemplate.boundListOps(groupId).rightPop();
	}

	@ManagedAttribute
	public int getMessageCountForAllMessageGroups() {
		Set<?> keys = this.redisTemplate.keys(this.beanName + ":*");
		if (keys == null) {
			return 0;
		}
		int count = 0;
		for (Object key : keys) {
			count += this.messageGroupSize(key);
		}
		return count;
	}

	@ManagedAttribute
	public int getMessageGroupCount() {
		Set<Object> keys = this.redisTemplate.keys(this.beanName + ":*");
		return keys == null ? 0 : keys.size();
	}

}
