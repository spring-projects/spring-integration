/*
 * Copyright 2002-2013 the original author or authors
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
		return this.buildMessageGroup(groupId, false);
	}


	/**
	 * Add a Message to the group with the provided group ID.
	 */
	@Override
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");

		// add message as is to the MG accessible by the caller
		SimpleMessageGroup messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));

		messageGroup.add(message);

		// enrich Message with additional headers and add it to MS
		Message<?> enrichedMessage = this.enrichMessage(message);

		this.addMessage(enrichedMessage);

		// build raw MessageGroup and add enriched Message to it
		SimpleMessageGroup rawGroup = this.buildMessageGroup(groupId, true);
		rawGroup.setLastModified(System.currentTimeMillis());
		rawGroup.add(enrichedMessage);

		// store MessageGroupMetadata built from enriched MG
		this.doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, new MessageGroupMetadata(rawGroup));

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

		// build raw MG
		SimpleMessageGroup rawGroup = this.buildMessageGroup(groupId, true);

		// create a clean instance of
		SimpleMessageGroup messageGroup = this.normalizeSimpleMessageGroup(rawGroup);

		for (Message<?> message : rawGroup.getMessages()) {
			if (message.getHeaders().getId().equals(messageToRemove.getHeaders().getId())){
				rawGroup.remove(message);
			}
		}
		this.removeMessage(messageToRemove.getHeaders().getId());
		rawGroup.setLastModified(System.currentTimeMillis());

		this.doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, new MessageGroupMetadata(rawGroup));
		messageGroup = this.getSimpleMessageGroup(this.getMessageGroup(groupId));

		return messageGroup;
	}


	@Override
	public void completeGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		SimpleMessageGroup messageGroup = this.buildMessageGroup(groupId, true);
		messageGroup.complete();
		messageGroup.setLastModified(System.currentTimeMillis());
		this.doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, new MessageGroupMetadata(messageGroup));
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
		SimpleMessageGroup messageGroup = this.buildMessageGroup(groupId, true);
		messageGroup.setLastReleasedMessageSequenceNumber(sequenceNumber);
		messageGroup.setLastModified(System.currentTimeMillis());
		this.doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, new MessageGroupMetadata(messageGroup));
	}

	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Object mgm = this.doRetrieve(MESSAGE_GROUP_KEY_PREFIX + groupId);
		if (mgm != null) {
			Assert.isInstanceOf(MessageGroupMetadata.class, mgm);
			MessageGroupMetadata messageGroupMetadata = (MessageGroupMetadata) mgm;

			UUID firstId = messageGroupMetadata.firstId();
			if (firstId != null){
				messageGroupMetadata.remove(firstId);
				messageGroupMetadata.setLastModified(System.currentTimeMillis());
				this.doStore(MESSAGE_GROUP_KEY_PREFIX + groupId, messageGroupMetadata);
				return this.removeMessage(firstId);
			}
		}
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<MessageGroup> iterator() {
		final Iterator<?> idIterator = this.normalizeKeys(
				(Collection<String>) this.doListKeys(MESSAGE_GROUP_KEY_PREFIX + "*"))
					.iterator();
		return new MessageGroupIterator(idIterator);
	}

	private Collection<String> normalizeKeys(Collection<String> keys){
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
		Object mgm = this.doRetrieve(MESSAGE_GROUP_KEY_PREFIX + groupId);
		if (mgm != null) {
			Assert.isInstanceOf(MessageGroupMetadata.class, mgm);
			MessageGroupMetadata messageGroupMetadata = (MessageGroupMetadata) mgm;
			return messageGroupMetadata.size();
		}
		return 0;
	}

	protected abstract Object doRetrieve(Object id);

	protected abstract void doStore(Object id, Object objectToStore);

	protected abstract Object doRemove(Object id);

	protected abstract Collection<?> doListKeys(String keyPattern);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Message<?> normalizeMessage(Message<?> message){
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
	private Message<?> enrichMessage(Message<?> message){
		Message<?> enrichedMessage = this.getMessageBuilderFactory().fromMessage(message)
				.setHeader(CREATED_DATE, System.currentTimeMillis())
				.build();
		Map innerMap = (Map) new DirectFieldAccessor(enrichedMessage.getHeaders()).getPropertyValue("headers");
		innerMap.put(MessageHeaders.ID, message.getHeaders().getId());
		innerMap.put(MessageHeaders.TIMESTAMP, message.getHeaders().getTimestamp());
		return enrichedMessage;
	}

	private SimpleMessageGroup buildMessageGroup(Object groupId, boolean raw){
		Assert.notNull(groupId, "'groupId' must not be null");
		Object mgm = this.doRetrieve(MESSAGE_GROUP_KEY_PREFIX + groupId);
		if (mgm != null) {
			Assert.isInstanceOf(MessageGroupMetadata.class, mgm);
			MessageGroupMetadata messageGroupMetadata = (MessageGroupMetadata) mgm;
			ArrayList<Message<?>> messages = new ArrayList<Message<?>>();

			Iterator<UUID> messageIds = messageGroupMetadata.messageIdIterator();
			while (messageIds.hasNext()){
				if (raw){
					messages.add(this.getRawMessage(messageIds.next()));
				}
				else {
					messages.add(this.getMessage(messageIds.next()));
				}
			}

			SimpleMessageGroup messageGroup = new SimpleMessageGroup(messages,
						groupId, messageGroupMetadata.getTimestamp(), messageGroupMetadata.isComplete());
			messageGroup.setLastModified(messageGroupMetadata.getLastModified());
			messageGroup.setLastReleasedMessageSequenceNumber(messageGroupMetadata.getLastReleasedMessageSequenceNumber());
			return messageGroup;
		}
		else {
			return new SimpleMessageGroup(groupId);
		}
	}

	private SimpleMessageGroup getSimpleMessageGroup(MessageGroup messageGroup){
		if (messageGroup instanceof SimpleMessageGroup){
			return (SimpleMessageGroup) messageGroup;
		}
		else {
			return new SimpleMessageGroup(messageGroup);
		}
	}

	private SimpleMessageGroup normalizeSimpleMessageGroup(SimpleMessageGroup messageGroup){
		SimpleMessageGroup normalizedGroup = new SimpleMessageGroup(messageGroup.getGroupId());
		for (Message<?> message : messageGroup.getMessages()) {
			Message<?> normailizedMessage = this.normalizeMessage(message);
			normalizedGroup.add(normailizedMessage);
		}
		return normalizedGroup;
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
