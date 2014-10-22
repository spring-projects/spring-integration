/*
 * Copyright 2002-2014 the original author or authors
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

/**
 * Base class for implementations of Key/Value style {@link MessageGroupStore} and {@link MessageStore}
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.1
 */
public abstract class AbstractKeyValueMessageStore extends AbstractMessageGroupStore implements MessageStore{

	protected static final String MESSAGE_KEY_PREFIX = "MESSAGE_";

	protected static final String MESSAGE_GROUP_KEY_PREFIX = "MESSAGE_GROUP_";

	protected static final String CREATED_DATE = "CREATED_DATE";

	// MessageStore methods

	@Override
	public Message<?> getMessage(UUID id) {
		Message<?> message = this.getRawMessage(id);
		if (message != null){
			return this.normalizeMessage(message);
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		UUID messageId = message.getHeaders().getId();
		this.doStore(MESSAGE_KEY_PREFIX + messageId, message);
		return (Message<T>) this.getRawMessage(messageId);
	}

	@Override
	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Object message = this.doRemove(MESSAGE_KEY_PREFIX + id);
		if (message != null) {
			Assert.isInstanceOf(Message.class, message);
		}
		if (message != null){
			return this.normalizeMessage((Message<?>) message);
		}
		return null;
	}

	@Override
	@ManagedAttribute
	public long getMessageCount() {
		Collection<?> messageIds = this.doListKeys(MESSAGE_KEY_PREFIX + "*");
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
			SimpleMessageGroup messageGroup = new SimpleMessageGroup(Collections.<Message<?>>emptyList(),
					groupId, metadata.getTimestamp(), metadata.isComplete());
			messageGroup.setLastModified(metadata.getLastModified());
			messageGroup.setLastReleasedMessageSequenceNumber(metadata.getLastReleasedMessageSequenceNumber());
			PersistentMessageGroup persistentMessageGroup = new PersistentMessageGroup(messageGroup);
			persistentMessageGroup.setSize(metadata.size());
			return persistentMessageGroup;
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
			return  (MessageGroupMetadata) mgm;
		}
		return null;
	}

	/**
	 * Add a Message to the group with the provided group ID.
	 */
	@Override
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");

		// enrich Message with additional headers and add it to MS
		Message<?> enrichedMessage = this.enrichMessage(message);

		this.addMessage(enrichedMessage);

		MessageGroupMetadata metadata = getGroupMetadata(groupId);
		if (metadata != null) {
			metadata.add(enrichedMessage.getHeaders().getId());
		}
		else {
			SimpleMessageGroup group = new SimpleMessageGroup(groupId);
			group.add(enrichedMessage);
			metadata = new MessageGroupMetadata(group);
		}
		metadata.setLastModified(System.currentTimeMillis());

		// store MessageGroupMetadata built from enriched MG
		doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, metadata);

		// return clean MG
		return this.getMessageGroup(groupId);
	}

	/**
	 * Remove a Message from the group with the provided group ID.
	 */
	@Override
	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");

		UUID id = messageToRemove.getHeaders().getId();
		removeMessage(id);

		MessageGroupMetadata metadata = getGroupMetadata(groupId);
		if (metadata != null) {
			metadata.remove(id);
			metadata.setLastModified(System.currentTimeMillis());
			doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, metadata);
		}

		return getMessageGroup(groupId);
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
		Object mgm = this.doRemove(MESSAGE_GROUP_KEY_PREFIX + groupId);
		if (mgm != null) {
			Assert.isInstanceOf(MessageGroupMetadata.class, mgm);
			MessageGroupMetadata messageGroupMetadata = (MessageGroupMetadata) mgm;

			Iterator<UUID> messageIds = messageGroupMetadata.messageIdIterator();
			while (messageIds.hasNext()){
				this.removeMessage(messageIds.next());
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
	protected Collection<Message<?>> getMessagesForGroup(Object groupId) {
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
		final Iterator<?> idIterator = this.normalizeKeys(
				(Collection<String>) this.doListKeys(MESSAGE_GROUP_KEY_PREFIX + "*"))
					.iterator();
		return new MessageGroupIterator(idIterator);
	}

	private Collection<String> normalizeKeys(Collection<String> keys) {
		Set<String> normalizedKeys = new HashSet<String>();
		for (Object key : keys) {
			String strKey = (String) key;
			if (strKey.startsWith(MESSAGE_GROUP_KEY_PREFIX)){
				strKey = strKey.replace(MESSAGE_GROUP_KEY_PREFIX, "");
			}
			else if (strKey.startsWith(MESSAGE_KEY_PREFIX)){
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

	protected abstract Object doRemove(Object id);

	protected abstract Collection<?> doListKeys(String keyPattern);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Message<?> normalizeMessage(Message<?> message) {
		Message<?> normalizedMessage = this.getMessageBuilderFactory().fromMessage(message)
				.removeHeader("CREATED_DATE")
				.build();
		Map innerMap = (Map) new DirectFieldAccessor(normalizedMessage.getHeaders()).getPropertyValue("headers");
		innerMap.put(MessageHeaders.ID, message.getHeaders().getId());
		innerMap.put(MessageHeaders.TIMESTAMP, message.getHeaders().getTimestamp());
		return normalizedMessage;
	}

	/**
	 * Will enrich Message with additional meta headers
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Message<?> enrichMessage(Message<?> message) {
		Message<?> enrichedMessage = this.getMessageBuilderFactory().fromMessage(message)
				.setHeader(CREATED_DATE, System.currentTimeMillis())
				.build();
		Map innerMap = (Map) new DirectFieldAccessor(enrichedMessage.getHeaders()).getPropertyValue("headers");
		innerMap.put(MessageHeaders.ID, message.getHeaders().getId());
		innerMap.put(MessageHeaders.TIMESTAMP, message.getHeaders().getTimestamp());
		return enrichedMessage;
	}

	private Message<?> getRawMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Object message = this.doRetrieve(MESSAGE_KEY_PREFIX + id);
		return (Message<?>) message;
	}

	private class MessageGroupIterator implements Iterator<MessageGroup> {

		private final Iterator<?> idIterator;

		private MessageGroupIterator(Iterator<?> idIterator) {
			this.idIterator = idIterator;
		}

		@Override
		public boolean hasNext() {
			return idIterator.hasNext();
		}

		@Override
		public MessageGroup next() {
			Object messageGroupId = idIterator.next();
			return getMessageGroup(messageGroupId);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
