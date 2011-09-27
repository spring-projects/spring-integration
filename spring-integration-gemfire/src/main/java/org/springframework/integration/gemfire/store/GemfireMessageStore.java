/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.gemfire.store;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.integration.Message;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class GemfireMessageStore extends AbstractMessageGroupStore implements MessageStore{

	private final Region<UUID, Message<?>> messageRegion;
	
	private final Region<Object, MessageGroup> messageGroupRegion;

	public GemfireMessageStore(Cache cache) {
		Assert.notNull(cache, "'cache' must not be null");
		try {
			RegionFactoryBean<UUID, Message<?>> messageRegionFactoryBean = new RegionFactoryBean<UUID, Message<?>>();
			messageRegionFactoryBean.setBeanName("messageRegionFactoryBean");
			messageRegionFactoryBean.setCache(cache);
			messageRegionFactoryBean.afterPropertiesSet();
			this.messageRegion = messageRegionFactoryBean.getObject();
			
			RegionFactoryBean<Object, MessageGroup> messageGroupRegionFactoryBean = new RegionFactoryBean<Object, MessageGroup>();
			messageGroupRegionFactoryBean.setBeanName("messageGroupRegionFactoryBean");
			messageGroupRegionFactoryBean.setCache(cache);
			messageGroupRegionFactoryBean.afterPropertiesSet();
			this.messageGroupRegion = messageGroupRegionFactoryBean.getObject();
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to initialize Gemfire Regions");
		}
	}

	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		return this.messageRegion.get(id);
	}

	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		this.messageRegion.put(message.getHeaders().getId(), message);
		return message;
	}

	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		return this.messageRegion.remove(id);
	}

	@ManagedAttribute
	public long getMessageCount() {
		return this.messageRegion.size();
	}

	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		MessageGroup messageGroup = null;
		if (this.messageGroupRegion.containsKey(groupId)){
			messageGroup = this.messageGroupRegion.get(groupId);
		}
		else {
			messageGroup = new SimpleMessageGroup(groupId);
			this.messageGroupRegion.put(groupId, messageGroup);
		}
		return messageGroup;
	}

	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.add(message);
		this.messageGroupRegion.put(groupId, messageGroup);
		this.addMessage(message);
		return messageGroup;
	}

	public MessageGroup markMessageGroup(MessageGroup group) {
		Assert.notNull(group, "'group' must not be null");
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(group);
		messageGroup.markAll();
		this.messageGroupRegion.put(messageGroup.getGroupId(), messageGroup);
		return messageGroup;
	}

	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.remove(messageToRemove);
		this.messageGroupRegion.put(groupId, messageGroup);
		this.removeMessage(messageToRemove.getHeaders().getId());
		return messageGroup;
	}

	public MessageGroup markMessageFromGroup(Object groupId, Message<?> messageToMark) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToMark, "'messageToMark' must not be null");
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.mark(messageToMark);
		this.messageGroupRegion.put(groupId, messageGroup);
		return messageGroup;
	}

	public void removeMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		MessageGroup messageGroup = this.messageGroupRegion.remove(groupId);
		Collection<Message<?>> markedMessages = messageGroup.getMarked();
		for (Message<?> message : markedMessages) {
			this.removeMessage(message.getHeaders().getId());
		}
		Collection<Message<?>> unmarkedMessages = messageGroup.getMarked();
		for (Message<?> message : unmarkedMessages) {
			this.removeMessage(message.getHeaders().getId());
		}
	}
	
	public Iterator<MessageGroup> iterator() {
		return this.messageGroupRegion.values().iterator();
	}
	
	private SimpleMessageGroup getSimpleMessageGroup(MessageGroup messageGroup){
		if (messageGroup instanceof SimpleMessageGroup){
			return (SimpleMessageGroup) messageGroup;
		}
		else {
			return new SimpleMessageGroup(messageGroup);
		}
	}
}
