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
package org.springframework.integration.atomic;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.support.atomic.RedisAtomicInteger;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.redis.channel.SubscribableRedisChannel;
/**
 * @author Oleg Zhurakousky
 *
 */
public class RedisDistributedLatch extends AbstractDistributedLatch {
	
	private final RedisAtomicInteger distributedLock;
	
	@Override
	protected boolean aquireLock() {
		return this.distributedLock.compareAndSet(0, 1);
	}

	@Override
	protected boolean releaseLock() {
		return this.distributedLock.compareAndSet(1, 0);
	}

	public RedisDistributedLatch(RedisConnectionFactory connectionFactory, int count, String correlationId){
		super(count, correlationId, createChannel(connectionFactory, correlationId));
		distributedLock = new RedisAtomicInteger(correlationId, connectionFactory);
	}
	
	private static SubscribableRedisChannel createChannel(RedisConnectionFactory connectionFactory, String topicName){
		SubscribableRedisChannel channel = new SubscribableRedisChannel((JedisConnectionFactory) connectionFactory, topicName);
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("errorChannel", new NullChannel());
		channel.setBeanFactory(bf);
		channel.afterPropertiesSet();
		return channel;
	}
}
