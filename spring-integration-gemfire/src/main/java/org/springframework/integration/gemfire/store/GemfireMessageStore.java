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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import org.springframework.data.gemfire.RegionFactoryBean;
import org.springframework.integration.Message;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupWrapper;
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

	private final Region<Object, Message<?>> messageRegion;
	
	private final Region<Object, MessageGroupWrapper> messageGroupRegion;

	public GemfireMessageStore(Cache cache) {
		Assert.notNull(cache, "'cache' must not be null");
		try {
			RegionFactoryBean<Object, Message<?>> messageRegionFactoryBean = new RegionFactoryBean<Object, Message<?>>();
			messageRegionFactoryBean.setBeanName("messageRegionFactoryBean");
			messageRegionFactoryBean.setCache(cache);
			messageRegionFactoryBean.afterPropertiesSet();
			this.messageRegion = messageRegionFactoryBean.getObject();
			
			RegionFactoryBean<Object, MessageGroupWrapper> messageGroupRegionFactoryBean = new RegionFactoryBean<Object, MessageGroupWrapper>();
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
			MessageGroupWrapper messageGroupWrapper = this.messageGroupRegion.get(groupId);
			
			ArrayList<Message<?>> markedMessages = new ArrayList<Message<?>>();
			for (UUID uuid : messageGroupWrapper.getMarkedMessageIds()) {
				markedMessages.add(this.getMessage(uuid));
			}
			
			ArrayList<Message<?>> unmarkedMessages = new ArrayList<Message<?>>();
			for (UUID uuid : messageGroupWrapper.getUnmarkedMessageIds()) {
				unmarkedMessages.add(this.getMessage(uuid));
			}
			messageGroup = new SimpleMessageGroup(unmarkedMessages, markedMessages, 
					groupId, messageGroupWrapper.getTimestamp(), messageGroupWrapper.isComplete());
		}
		else {
			messageGroup = new SimpleMessageGroup(groupId);
			//this.messageGroupRegion.put(groupId, new MessageGroupWrapper(messageGroup));
		}
		return messageGroup;
	}

	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.add(message);
		this.messageGroupRegion.put(groupId, new MessageGroupWrapper(messageGroup));
		this.addMessage(message);
		return messageGroup;
	}

	public MessageGroup markMessageGroup(MessageGroup group) {
		Assert.notNull(group, "'group' must not be null");
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(group);
		messageGroup.markAll();
		this.messageGroupRegion.put(messageGroup.getGroupId(), new MessageGroupWrapper(messageGroup));
		return messageGroup;
	}

	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.remove(messageToRemove);
		this.messageGroupRegion.put(groupId, new MessageGroupWrapper(messageGroup));
		this.removeMessage(messageToRemove.getHeaders().getId());
		return messageGroup;
	}

	public MessageGroup markMessageFromGroup(Object groupId, Message<?> messageToMark) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToMark, "'messageToMark' must not be null");
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.mark(messageToMark);
		this.messageGroupRegion.put(groupId, new MessageGroupWrapper(messageGroup));
		return messageGroup;
	}

	public void removeMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		MessageGroupWrapper messageGroupWrapper = this.messageGroupRegion.remove(groupId);
		Collection<UUID> markedMessagesId = messageGroupWrapper.getMarkedMessageIds();
		for (UUID uuid : markedMessagesId) {
			this.removeMessage(uuid);
		}
		Collection<UUID> unmarkedMessagesId = messageGroupWrapper.getUnmarkedMessageIds();
		for (UUID uuid : unmarkedMessagesId) {
			this.removeMessage(uuid);
		}
	}
	
	public Iterator<MessageGroup> iterator() {
		ArrayList<MessageGroup> messageGroups = new ArrayList<MessageGroup>();
		Iterator<MessageGroupWrapper> messageGroupWrappers = this.messageGroupRegion.values().iterator();
		while (messageGroupWrappers.hasNext()) {
			MessageGroupWrapper messageGroupWrapper = messageGroupWrappers.next();
			messageGroups.add(this.getMessageGroup(messageGroupWrapper.getGroupId()));
		}
		return messageGroups.iterator();
	}
	
	private SimpleMessageGroup getSimpleMessageGroup(MessageGroup messageGroup){
		if (messageGroup instanceof SimpleMessageGroup){
			return (SimpleMessageGroup) messageGroup;
		}
		else {
			return new SimpleMessageGroup(messageGroup);
		}
	}

	public void completeGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.complete();
		this.messageGroupRegion.put(groupId, new MessageGroupWrapper(messageGroup));
	}
}
