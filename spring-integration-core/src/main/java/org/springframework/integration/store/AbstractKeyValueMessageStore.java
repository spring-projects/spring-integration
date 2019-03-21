/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.store;

import java.util.ArrayList;
import java.util.Collection;
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
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public abstract class AbstractKeyValueMessageStore extends AbstractMessageGroupStore implements MessageStore {

	protected static final String MESSAGE_KEY_PREFIX = "MESSAGE_";

	protected static final String MESSAGE_GROUP_KEY_PREFIX = "MESSAGE_GROUP_";

	protected static final String CREATED_DATE = "CREATED_DATE";

	private final String messagePrefix;

	private final String groupPrefix;

	protected AbstractKeyValueMessageStore() {
		this("");
	}

	/**
	 * Construct an instance based on the provided prefix for keys to distinguish between
	 * different store instances in the same target key-value data base. Defaults to an
	 * empty string - no prefix. The actual prefix for messages is
	 * {@code prefix + MESSAGE_}; for message groups - {@code prefix + MESSAGE_GROUP_}
	 * @param prefix the prefix to use
	 * @since 4.3.12
	 */
	protected AbstractKeyValueMessageStore(String prefix) {
		Assert.notNull(prefix, "'prefix' must not be null");
		this.messagePrefix = prefix + MESSAGE_KEY_PREFIX;
		this.groupPrefix = prefix + MESSAGE_GROUP_KEY_PREFIX;
	}

	/**
	 * Return the configured prefix for message keys to distinguish between different
	 * store instances in the same target key-value data base. Defaults to the
	 * {@value MESSAGE_KEY_PREFIX} - without a custom prefix.
	 * @return the prefix for keys
	 * @since 4.3.12
	 */
	protected String getMessagePrefix() {
		return this.messagePrefix;
	}

	/**
	 * Return the configured prefix for message group keys to distinguish between
	 * different store instances in the same target key-value data base. Defaults to the
	 * {@value MESSAGE_GROUP_KEY_PREFIX} - without custom prefix.
	 * @return the prefix for keys
	 * @since 4.3.12
	 */
	public String getGroupPrefix() {
		return this.groupPrefix;
	}

	// MessageStore methods

	@Override
	public Message<?> getMessage(UUID id) {
		Message<?> message = getRawMessage(id);
		if (message != null) {
			return normalizeMessage(message);
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Message<T> addMessage(Message<T> message) {
		doAddMessage(message);
		return (Message<T>) getMessage(message.getHeaders().getId());
	}

	protected void doAddMessage(Message<?> message) {
		Assert.notNull(message, "'message' must not be null");
		UUID messageId = message.getHeaders().getId();
		doStore(this.messagePrefix + messageId, message);
	}

	@Override
	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Object message = doRemove(this.messagePrefix + id);
		if (message != null) {
			Assert.isInstanceOf(Message.class, message);
		}
		if (message != null) {
			return normalizeMessage((Message<?>) message);
		}
		return null;
	}

	@Override
	@ManagedAttribute
	public long getMessageCount() {
		Collection<?> messageIds = doListKeys(this.messagePrefix + "*");
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
		Object mgm = this.doRetrieve(this.groupPrefix + groupId);
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
			// enrich Message with additional headers and add it to MS
			Message<?> enrichedMessage = enrichMessage(message);
			doAddMessage(enrichedMessage);
			if (metadata != null) {
				metadata.add(enrichedMessage.getHeaders().getId());
			}
			else {
				group.add(enrichedMessage);
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
		doStore(this.groupPrefix + groupId, metadata);
	}

	/**
	 * Remove a Message from the group with the provided group ID.
	 */
	@Override
	@Deprecated
	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");

		UUID id = messageToRemove.getHeaders().getId();
		removeMessage(id);

		MessageGroupMetadata metadata = getGroupMetadata(groupId);
		if (metadata != null) {
			metadata.remove(id);
			metadata.setLastModified(System.currentTimeMillis());
			doStore(this.groupPrefix + groupId, metadata);
		}

		return getMessageGroup(groupId);
	}


	@Override
	public void removeMessagesFromGroup(Object groupId, Collection<Message<?>> messages) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messages, "'messages' must not be null");

		Object mgm = doRetrieve(this.groupPrefix + groupId);
		if (mgm != null) {
			Assert.isInstanceOf(MessageGroupMetadata.class, mgm);
			MessageGroupMetadata messageGroupMetadata = (MessageGroupMetadata) mgm;
			for (Message<?> messageToRemove : messages) {
				UUID messageId = messageToRemove.getHeaders().getId();
				messageGroupMetadata.remove(messageId);
				doRemove(this.messagePrefix + messageId);
			}
			messageGroupMetadata.setLastModified(System.currentTimeMillis());
			doStore(this.groupPrefix + groupId, messageGroupMetadata);
		}
	}

	@Override
	public void completeGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		MessageGroupMetadata metadata = getGroupMetadata(groupId);
		if (metadata != null) {
			metadata.complete();
			metadata.setLastModified(System.currentTimeMillis());
			doStore(this.groupPrefix + groupId, metadata);
		}
	}

	/**
	 * Remove the MessageGroup with the provided group ID.
	 */
	@Override
	public void removeMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Object mgm = doRemove(this.groupPrefix + groupId);
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
		doStore(this.groupPrefix + groupId, metadata);
	}

	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {
		MessageGroupMetadata groupMetadata = getGroupMetadata(groupId);
		if (groupMetadata != null) {
			UUID firstId = groupMetadata.firstId();
			if (firstId != null) {
				groupMetadata.remove(firstId);
				groupMetadata.setLastModified(System.currentTimeMillis());
				doStore(this.groupPrefix + groupId, groupMetadata);
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
				(Collection<String>) doListKeys(this.groupPrefix + "*"))
				.iterator();
		return new MessageGroupIterator(idIterator);
	}

	private Collection<String> normalizeKeys(Collection<String> keys) {
		Set<String> normalizedKeys = new HashSet<String>();
		for (Object key : keys) {
			String strKey = (String) key;
			if (strKey.startsWith(this.groupPrefix)) {
				strKey = strKey.replace(this.groupPrefix, "");
			}
			else if (strKey.startsWith(this.messagePrefix)) {
				strKey = strKey.replace(this.messagePrefix, "");
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
		Message<?> normalizedMessage = getMessageBuilderFactory().fromMessage(message)
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
		Message<?> enrichedMessage = getMessageBuilderFactory().fromMessage(message)
				.setHeader(CREATED_DATE, System.currentTimeMillis())
				.build();
		Map innerMap = (Map) new DirectFieldAccessor(enrichedMessage.getHeaders()).getPropertyValue("headers");
		innerMap.put(MessageHeaders.ID, message.getHeaders().getId());
		innerMap.put(MessageHeaders.TIMESTAMP, message.getHeaders().getTimestamp());
		return enrichedMessage;
	}

	private Message<?> getRawMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Object message = doRetrieve(this.messagePrefix + id);
		return (Message<?>) message;
	}

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
