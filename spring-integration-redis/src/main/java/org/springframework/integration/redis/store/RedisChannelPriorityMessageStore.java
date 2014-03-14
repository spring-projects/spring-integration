/*
 * Copyright 2014 the original author or authors.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.store.ChannelPriorityMessageStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Specialized Redis {@link ChannelMessageStore} that uses lists to back a QueueChannel.
 * Messages are removed in priority order ({@link IntegrationMessageHeaderAccessor#PRIORITY}.
 * Priorities 0-9 are supported; higher values are treated with the same priority (none)
 * as messages with no priority header.
 * <p>
 * Requires that groupId is a String.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public class RedisChannelPriorityMessageStore extends RedisChannelMessageStore implements ChannelPriorityMessageStore {

	public RedisChannelPriorityMessageStore(RedisConnectionFactory connectionFactory) {
		super(connectionFactory);
	}


	@Override
	@ManagedAttribute
	public int messageGroupSize(Object groupId) {
		Assert.isInstanceOf(String.class, groupId);
		List<String> list = sortedKeys((String) groupId);
		int count = 0;
		for (String key : list) {
			count += this.getRedisTemplate().boundListOps(key).size();
		}
		return count;
	}

	@SuppressWarnings("unchecked")
	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		List<Object> allMessages = new LinkedList<Object>();
		List<String> list = sortedKeys((String) groupId);
		for (String key : list) {
			List<?> messages = this.getRedisTemplate().boundListOps(key).range(0, -1);
			allMessages.addAll(messages);
		}
		return new SimpleMessageGroup((Collection<? extends Message<?>>) allMessages, groupId);
	}


	@Override
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.isInstanceOf(String.class, groupId);
		String key = (String) groupId;
		Integer priority = new IntegrationMessageHeaderAccessor(message).getPriority();
		if (priority != null && priority < 10 && priority >=0) {
			key = key + ":" + priority;
		}
		else {
			key = key + ":z";
		}
		return super.addMessageToGroup(key, message);
	}

	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {
		Assert.isInstanceOf(String.class, groupId);
		List<String> list = sortedKeys((String) groupId);
		Message<?> message;
		for (String key : list) {
			message = super.pollMessageFromGroup(key);
			if (message != null) {
				return message;
			}
		}
		return null;
	}

	private List<String> sortedKeys(String channel) {
		Set<Object> keys = this.getRedisTemplate().keys(this.getBeanName() + ":" + channel == null ? "*" : (channel + "*"));
		List<String> list = new LinkedList<String>();
		for (Object key : keys) {
			Assert.isInstanceOf(String.class, key);
			list.add((String) key);
		}
		Collections.sort(list);
		return list;
	}

	@Override
	@ManagedAttribute
	public int getMessageGroupCount() {
		Set<Object> narrowedKeys = narrowedKeys();
		return narrowedKeys.size();
	}


	private Set<Object> narrowedKeys() {
		Set<Object> keys = this.getRedisTemplate().keys(this.getBeanName() + ":*");
		Set<Object> narrowedKeys = new HashSet<Object>();
		Iterator<Object> iterator = keys.iterator();
		while (iterator.hasNext()) {
			Object key = iterator.next();
			Assert.isInstanceOf(String.class, key);
			String keyString = (String) key;
			int lastIndexOfColon = keyString.lastIndexOf(":");
			if (keyString.indexOf(":") != lastIndexOfColon) {
				narrowedKeys.add(keyString.substring(0, lastIndexOfColon));
			}
			else {
				narrowedKeys.add(key);
			}
		}
		return narrowedKeys;
	}

	@Override
	public void removeMessageGroup(Object groupId) {
		Assert.isInstanceOf(String.class, groupId);
		List<String> list = sortedKeys((String) groupId);
		for (String key : list) {
			super.removeMessageGroup(key);
		}
	}

	@Override
	@ManagedAttribute
	public int getMessageCountForAllMessageGroups() {
		Set<Object> narrowedKeys = narrowedKeys();
		int count = 0;
		for (Object key : narrowedKeys) {
			count += this.messageGroupSize(key);
		}
		return count;
	}


}
