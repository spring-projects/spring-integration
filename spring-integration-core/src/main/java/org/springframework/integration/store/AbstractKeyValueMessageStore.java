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
import java.util.Collection;
import java.util.Iterator;
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

	protected static final String MESSAGE_KEY_PREFIX = "MESSAGE_";
	
	protected static final String MESSAGE_GROUP_KEY_PREFIX = "MESSAGE_GROUP_";
	
	// MessageStore methods
	
	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Object message = this.doRetrieve(MESSAGE_KEY_PREFIX + id);
		if (message != null){
			Assert.isInstanceOf(Message.class, message);
		}	
		return (Message<?>) message;
	}

	@SuppressWarnings("unchecked")
	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		UUID messageId = message.getHeaders().getId(); 
		this.doStore(MESSAGE_KEY_PREFIX + messageId, message);
		return (Message<T>) this.getMessage(messageId);
	}

	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Object message = this.doRemove(MESSAGE_KEY_PREFIX + id);
		Assert.isInstanceOf(Message.class, message);
		return (Message<?>) message;
	}

	@ManagedAttribute
	public long getMessageCount() {
		Collection<?> messageIds = this.doListKeys(MESSAGE_KEY_PREFIX + "*");
		if (messageIds != null){
			return messageIds.size();
		}
		else {
			return 0;
		}
	}

	// MessageGroupStore methods

	/**
	 * Will create a new instance of SimpleMessageGroup 
	 */
	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Object mgm = this.doRetrieve(MESSAGE_GROUP_KEY_PREFIX + groupId);
		if (mgm != null){
			Assert.isInstanceOf(MessageGroupMetadata.class, mgm);
			MessageGroupMetadata messageGroupMetadata = (MessageGroupMetadata) mgm;
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

		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.add(message);	
		this.doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, new MessageGroupMetadata(messageGroup));
		this.addMessage(message);
		
		return messageGroup;
	}

	/**
	 * Mark all messages in the provided group. 
	 */
	public MessageGroup markMessageGroup(MessageGroup group) {
		Assert.notNull(group, "'group' must not be null");
		Object groupId = group.getGroupId();

		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(group);
		messageGroup.markAll();
		this.doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, new MessageGroupMetadata(messageGroup));
		
		return messageGroup;
	}

	/**
	 * Remove a Message from the group with the provided group ID. 
	 */
	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");
		
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.remove(messageToRemove);
		this.doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, new MessageGroupMetadata(messageGroup));
		
		return messageGroup;
	}

	/**
	 * Mark the given Message within the group corresponding to the provided group ID. 
	 */
	public MessageGroup markMessageFromGroup(Object groupId, Message<?> messageToMark) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToMark, "'messageToMark' must not be null");
		
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.mark(messageToMark);
		this.doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, new MessageGroupMetadata(messageGroup));
		
		return messageGroup;
		
	}
	
	public void completeGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.complete();
		this.doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, new MessageGroupMetadata(messageGroup));
	}

	/**
	 * Remove the MessageGroup with the provided group ID. 
	 */
	public void removeMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		
		Object mgm = this.doRemove(MESSAGE_GROUP_KEY_PREFIX + groupId);
		if (mgm != null){
			Assert.isInstanceOf(MessageGroupMetadata.class, mgm);
			MessageGroupMetadata messageGroupMetadata = (MessageGroupMetadata) mgm;
			for (UUID messageId : messageGroupMetadata.getMarkedMessageIds()) {
				this.removeMessage(messageId);
			}
			
			for (UUID messageId : messageGroupMetadata.getUnmarkedMessageIds()) {
				this.removeMessage(messageId);
			}
		}
	}
	
	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		Assert.notNull(groupId, "'groupId' must not be null");
		
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));
		messageGroup.setLastReleasedMessageSequenceNumber(sequenceNumber);
		this.doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, new MessageGroupMetadata(messageGroup));
	}

	public Iterator<MessageGroup> iterator() {
		final Iterator<?> messageGroupKeys = this.doListKeys(MESSAGE_GROUP_KEY_PREFIX + "*").iterator();
		return new Iterator<MessageGroup>() {

			public boolean hasNext() {
				return messageGroupKeys.hasNext();
			}

			public MessageGroup next() {
				Object messageGroupId = messageGroupKeys.next();
				return getMessageGroup(messageGroupId);
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	private SimpleMessageGroup getSimpleMessageGroup(MessageGroup messageGroup){
		if (messageGroup instanceof SimpleMessageGroup){
			return (SimpleMessageGroup) messageGroup;
		}
		else {
			return new SimpleMessageGroup(messageGroup);
		}
	}

	protected abstract Object doRetrieve(Object id);
	
	protected abstract void doStore(Object id, Object objectToStore);
	
	protected abstract Object doRemove(Object id);  

	protected abstract Collection<?> doListKeys(String keyPattern);
}
