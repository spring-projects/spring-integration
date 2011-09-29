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
package org.springframework.integration.store;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.integration.Message;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.util.Assert;

/**
 * Base class for implementations of Key/Value style {@link MessageGroupStore} and {@link MessageStore}
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public abstract class AbstractKeyValueMessageStore extends AbstractMessageGroupStore implements MessageStore{

	protected static final String MESSAGES_HOLDER_MAP_NAME = "MESSAGES";
	
	protected static final String MESSAGE_GROUPS_HOLDER_MAP_NAME = "MESSAGE_GROUPS";
	
	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Map<UUID, Message<?>> result = this.getHolderMapForMessage();
		if (result.containsKey(id)) {
			Message<?> message = result.get(id);
			return message;
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		UUID messageId = message.getHeaders().getId();
		Map<UUID, Message<?>> result = this.getHolderMapForMessage();
		result.put(messageId, message);
		this.storeHolderMap(MESSAGES_HOLDER_MAP_NAME, result);
		return (Message<T>) this.getMessage(messageId);
	}

	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Map<UUID, Message<?>> result = this.getHolderMapForMessage();
		Message<?> message = result.remove(id);
		this.storeHolderMap(MESSAGES_HOLDER_MAP_NAME, result);
		return message;
	}

	@ManagedAttribute
	public long getMessageCount() {
		Map<UUID, Message<?>> result = this.getHolderMapForMessage();
		return result.size();
	}


	// MESSAGE GROUP methods

	/**
	 * Will create a new instance of SimpleMessageGroup 
	 */
	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Map<Object, MessageGroupMetadata> result = this.getHolderMapForMessageGroups();
		if (result.containsKey(groupId)){
			MessageGroupMetadata messageGroupMetadata = result.get(groupId);
			ArrayList<Message<?>> markedMessages = new ArrayList<Message<?>>();
			for (UUID uuid : messageGroupMetadata.getMarkedMessageIds()) {
				markedMessages.add(this.getMessage(uuid));
			}
			
			ArrayList<Message<?>> unmarkedMessages = new ArrayList<Message<?>>();
			for (UUID uuid : messageGroupMetadata.getUnmarkedMessageIds()) {
				unmarkedMessages.add(this.getMessage(uuid));
			}
			SimpleMessageGroup messageGroup = new SimpleMessageGroup(unmarkedMessages, markedMessages, 
						groupId, messageGroupMetadata.getTimestamp(), messageGroupMetadata.isComplete());
			if (messageGroupMetadata.getLastReleasedMessageSequenceNumber() > 0){
				messageGroup.setLastReleasedMessageSequenceNumber(messageGroupMetadata.getLastReleasedMessageSequenceNumber());
			}
			return messageGroup;
		}
		else {
			return new SimpleMessageGroup(groupId);
		}
	}

	/**
	 * Add a Message to the group with the provided group ID. 
	 */
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");
		
		Map<Object, MessageGroupMetadata> result = this.getHolderMapForMessageGroups();
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.add(message);	
		result.put(groupId, new MessageGroupMetadata(messageGroup));
		
		this.addMessage(message);
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP_NAME, result);

		return this.getMessageGroup(groupId);
	}

	/**
	 * Mark all messages in the provided group. 
	 */
	public MessageGroup markMessageGroup(MessageGroup group) {
		Assert.notNull(group, "'group' must not be null");
		Object groupId = group.getGroupId();

		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(group);
		messageGroup.markAll();
		Map<Object, MessageGroupMetadata> result = this.getHolderMapForMessageGroups();
		result.put(groupId, new MessageGroupMetadata(messageGroup));
		
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP_NAME, result);
		
		return this.getMessageGroup(groupId);
	}

	/**
	 * Remove a Message from the group with the provided group ID. 
	 */
	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");
		
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.remove(messageToRemove);
		Map<Object, MessageGroupMetadata> result = this.getHolderMapForMessageGroups();
		result.put(groupId, new MessageGroupMetadata(messageGroup));
		
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP_NAME, result);
		
		return this.getMessageGroup(groupId);
	}

	/**
	 * Mark the given Message within the group corresponding to the provided group ID. 
	 */
	public MessageGroup markMessageFromGroup(Object groupId, Message<?> messageToMark) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToMark, "'messageToMark' must not be null");
		
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.mark(messageToMark);
		Map<Object, MessageGroupMetadata> result = this.getHolderMapForMessageGroups();
		result.put(groupId, new MessageGroupMetadata(messageGroup));
		
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP_NAME, result);
		
		return this.getMessageGroup(groupId);
		
	}
	
	public void completeGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.complete();
		Map<Object, MessageGroupMetadata> result = this.getHolderMapForMessageGroups();
		result.put(groupId, new MessageGroupMetadata(messageGroup));
		
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP_NAME, result);
	}

	/**
	 * Remove the MessageGroup with the provided group ID. 
	 */
	public void removeMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		
		Map<Object, MessageGroupMetadata> result = this.getHolderMapForMessageGroups();
		MessageGroupMetadata messageGroupWrapper = (MessageGroupMetadata) result.get(groupId);
		
		for (UUID messageId : messageGroupWrapper.getMarkedMessageIds()) {
			this.removeMessage(messageId);
		}
		
		for (UUID messageId : messageGroupWrapper.getUnmarkedMessageIds()) {
			this.removeMessage(messageId);
		}
		result.remove(groupId);
		
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP_NAME, result);
	}
	
	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		Assert.notNull(groupId, "'groupId' must not be null");
		
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.setLastReleasedMessageSequenceNumber(sequenceNumber);
		Map<Object, MessageGroupMetadata> result = this.getHolderMapForMessageGroups();
		result.put(groupId, new MessageGroupMetadata(messageGroup));
		
		this.storeHolderMap(MESSAGE_GROUPS_HOLDER_MAP_NAME, result);
	}

	public Iterator<MessageGroup> iterator() {
		Map<Object, MessageGroupMetadata> result = this.getHolderMapForMessageGroups();
		List<MessageGroup> messageGroups = new ArrayList<MessageGroup>();
		for (Object object : result.values()) {
			MessageGroupMetadata messageGroupWrapper = (MessageGroupMetadata) object;
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
	
	protected abstract void storeHolderMap(String key, Object value);
	
	protected abstract Map<UUID, Message<?>> getHolderMapForMessage();
	
	protected abstract Map<Object, MessageGroupMetadata> getHolderMapForMessageGroups();
}
