/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Base class for implementations of Key/Value style {@link MessageGroupStore} and {@link MessageStore}
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 */
public abstract class AbstractKeyValueMessageStore extends AbstractMessageGroupStore implements MessageStore {

	protected static final String MESSAGE_KEY_PREFIX = "MESSAGE_";

	protected static final String MESSAGE_GROUP_KEY_PREFIX = "MESSAGE_GROUP_";

	/**
	 * Represents the time when the message has been added to the store.
	 * @deprecated since 5.0. This constant isn't used any more.
	 */
	@Deprecated
	protected static final String CREATED_DATE = "CREATED_DATE";

	// MessageStore methods

	@Override
	public Message<?> getMessage(UUID messageId) {
		Assert.notNull(messageId, "'messageId' must not be null");
		Object messageHolder = doRetrieve(MESSAGE_KEY_PREFIX + messageId);
		if (messageHolder != null) {
			Assert.isInstanceOf(MessageHolder.class, messageHolder);
			return ((MessageHolder) messageHolder).getMessage();
		}
		else {
			return null;
		}
	}

	@Override
	public MessageMetadata getMessageMetadata(UUID messageId) {
		Assert.notNull(messageId, "'messageId' must not be null");
		Object messageHolder = doRetrieve(MESSAGE_KEY_PREFIX + messageId);
		if (messageHolder != null) {
			Assert.isInstanceOf(MessageHolder.class, messageHolder);
			return ((MessageHolder) messageHolder).getMessageMetadata();
		}
		else {
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		UUID messageId = message.getHeaders().getId();
		doStoreIfAbsent(MESSAGE_KEY_PREFIX + messageId, new MessageHolder(message));
		return (Message<T>) getMessage(messageId);
	}

	@Override
	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Object message = doRemove(MESSAGE_KEY_PREFIX + id);
		if (message != null) {
			Assert.isInstanceOf(MessageHolder.class, message);
			return ((MessageHolder) message).getMessage();
		}
		else {
			return null;
		}
	}

	@Override
	@ManagedAttribute
	public long getMessageCount() {
		Collection<?> messageIds = doListKeys(MESSAGE_KEY_PREFIX + "*");
		return (messageIds != null) ? messageIds.size() : 0;
	}


	// MessageGroupStore methods

	/**
	 * Will create a new instance of SimpleMessageGroup if necessary.
	 */
	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		MessageGroupMetadata metadata = getGroupMetadata(groupId);
		if (metadata != null) {

			MessageGroup messageGroup = getMessageGroupFactory()
					.create(this, groupId, metadata.getTimestamp(), metadata.isComplete());
			messageGroup.setLastModified(metadata.getLastModified());
			messageGroup.setLastReleasedMessageSequenceNumber(metadata.getLastReleasedMessageSequenceNumber());
			return messageGroup;
		}
		else {
			return new SimpleMessageGroup(groupId);
		}
	}

	@Override
	public MessageGroupMetadata getGroupMetadata(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Object mgm = this.doRetrieve(MESSAGE_GROUP_KEY_PREFIX + groupId);
		if (mgm != null) {
			Assert.isInstanceOf(MessageGroupMetadata.class, mgm);
			return (MessageGroupMetadata) mgm;
		}
		return null;
	}

	@Override
	public void addMessagesToGroup(Object groupId, Message<?>... messages) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messages, "'messages' must not be null");

		MessageGroupMetadata metadata = getGroupMetadata(groupId);
		SimpleMessageGroup group = null;
		if (metadata == null) {
			group = new SimpleMessageGroup(groupId);
		}

		for (Message<?> message : messages) {
			addMessage(message);
			if (metadata != null) {
				metadata.add(message.getHeaders().getId());
			}
			else {
				group.add(message);
			}
		}

		if (group != null) {
			metadata = new MessageGroupMetadata(group);
			// When the group is new reuse "create time" as a "last modified"
			metadata.setLastModified(group.getTimestamp());
		}
		else {
			metadata.setLastModified(System.currentTimeMillis());
		}


		// store MessageGroupMetadata built from enriched MG
		doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, metadata);
	}

	@Override
	public void removeMessagesFromGroup(Object groupId, Collection<Message<?>> messages) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messages, "'messages' must not be null");

		Object mgm = doRetrieve(MESSAGE_GROUP_KEY_PREFIX + groupId);
		if (mgm != null) {
			Assert.isInstanceOf(MessageGroupMetadata.class, mgm);
			MessageGroupMetadata messageGroupMetadata = (MessageGroupMetadata) mgm;
			for (Message<?> messageToRemove : messages) {
				UUID messageId = messageToRemove.getHeaders().getId();
				messageGroupMetadata.remove(messageId);
				doRemove(MESSAGE_KEY_PREFIX + messageId);
			}
			messageGroupMetadata.setLastModified(System.currentTimeMillis());
			doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, messageGroupMetadata);
		}
	}

	@Override
	public void completeGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		MessageGroupMetadata metadata = getGroupMetadata(groupId);
		if (metadata != null) {
			metadata.complete();
			metadata.setLastModified(System.currentTimeMillis());
			doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, metadata);
		}
	}

	/**
	 * Remove the MessageGroup with the provided group ID.
	 */
	@Override
	public void removeMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Object mgm = doRemove(MESSAGE_GROUP_KEY_PREFIX + groupId);
		if (mgm != null) {
			Assert.isInstanceOf(MessageGroupMetadata.class, mgm);
			MessageGroupMetadata messageGroupMetadata = (MessageGroupMetadata) mgm;

			Iterator<UUID> messageIds = messageGroupMetadata.messageIdIterator();
			while (messageIds.hasNext()) {
				removeMessage(messageIds.next());
			}
		}
	}

	@Override
	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		Assert.notNull(groupId, "'groupId' must not be null");
		MessageGroupMetadata metadata = getGroupMetadata(groupId);
		if (metadata == null) {
			SimpleMessageGroup messageGroup = new SimpleMessageGroup(groupId);
			metadata = new MessageGroupMetadata(messageGroup);
		}
		metadata.setLastReleasedMessageSequenceNumber(sequenceNumber);
		metadata.setLastModified(System.currentTimeMillis());
		doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, metadata);
	}

	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {
		MessageGroupMetadata groupMetadata = getGroupMetadata(groupId);
		if (groupMetadata != null) {
			UUID firstId = groupMetadata.firstId();
			if (firstId != null) {
				groupMetadata.remove(firstId);
				groupMetadata.setLastModified(System.currentTimeMillis());
				doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, groupMetadata);
				return removeMessage(firstId);
			}
		}
		return null;
	}

	@Override
	public Message<?> getOneMessageFromGroup(Object groupId) {
		MessageGroupMetadata groupMetadata = getGroupMetadata(groupId);
		if (groupMetadata != null) {
			UUID messageId = groupMetadata.firstId();
			if (messageId != null) {
				return getMessage(messageId);
			}
		}
		return null;
	}

	@Override
	public Collection<Message<?>> getMessagesForGroup(Object groupId) {
		MessageGroupMetadata groupMetadata = getGroupMetadata(groupId);
		ArrayList<Message<?>> messages = new ArrayList<Message<?>>();
		if (groupMetadata != null) {
			Iterator<UUID> messageIds = groupMetadata.messageIdIterator();
			while (messageIds.hasNext()) {
				messages.add(getMessage(messageIds.next()));
			}
		}
		return messages;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<MessageGroup> iterator() {
		final Iterator<?> idIterator = normalizeKeys(
				(Collection<String>) doListKeys(MESSAGE_GROUP_KEY_PREFIX + "*"))
				.iterator();
		return new MessageGroupIterator(idIterator);
	}

	private Collection<String> normalizeKeys(Collection<String> keys) {
		Set<String> normalizedKeys = new HashSet<String>();
		for (Object key : keys) {
			String strKey = (String) key;
			if (strKey.startsWith(MESSAGE_GROUP_KEY_PREFIX)) {
				strKey = strKey.replace(MESSAGE_GROUP_KEY_PREFIX, "");
			}
			else if (strKey.startsWith(MESSAGE_KEY_PREFIX)) {
				strKey = strKey.replace(MESSAGE_KEY_PREFIX, "");
			}
			normalizedKeys.add(strKey);
		}
		return normalizedKeys;
	}

	@Override
	public int messageGroupSize(Object groupId) {
		MessageGroupMetadata mgm = getGroupMetadata(groupId);
		if (mgm != null) {
			return mgm.size();
		}
		else {
			return 0;
		}
	}

	protected abstract Object doRetrieve(Object id);

	protected abstract void doStore(Object id, Object objectToStore);

	protected abstract void doStoreIfAbsent(Object id, Object objectToStore);

	protected abstract Object doRemove(Object id);

	protected abstract Collection<?> doListKeys(String keyPattern);

	private final class MessageGroupIterator implements Iterator<MessageGroup> {

		private final Iterator<?> idIterator;

		private MessageGroupIterator(Iterator<?> idIterator) {
			this.idIterator = idIterator;
		}

		@Override
		public boolean hasNext() {
			return this.idIterator.hasNext();
		}

		@Override
		public MessageGroup next() {
			Object messageGroupId = this.idIterator.next();
			return getMessageGroup(messageGroupId);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
