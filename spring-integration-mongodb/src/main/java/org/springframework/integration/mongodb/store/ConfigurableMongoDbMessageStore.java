/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.integration.mongodb.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupMetadata;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An alternate MongoDB {@link MessageStore} and {@link MessageGroupStore} which allows the user to
 * configure the instance of {@link MongoTemplate}. The mechanism of storing the messages/group of messages
 * in the store is and is different from {@link MongoDbMessageStore}. Since the store uses serialization of the
 * messages by default, all the headers, and the payload of the Message must implement {@link java.io.Serializable}
 * interface
 *
 * @author Amol Nayak
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public class ConfigurableMongoDbMessageStore extends AbstractConfigurableMongoDbMessageStore
		implements MessageStore {

	public final static String DEFAULT_COLLECTION_NAME = "configurableStoreMessages";

	private final Collection<MessageGroupCallback> expiryCallbacks = new LinkedHashSet<MessageGroupCallback>();

	private volatile boolean timeoutOnIdle;


	public ConfigurableMongoDbMessageStore(MongoTemplate mongoTemplate) {
		this(mongoTemplate, DEFAULT_COLLECTION_NAME);
	}

	public ConfigurableMongoDbMessageStore(MongoTemplate mongoTemplate, String collectionName) {
		super(mongoTemplate, collectionName);
	}

	public ConfigurableMongoDbMessageStore(MongoDbFactory mongoDbFactory) {
		this(mongoDbFactory, null, DEFAULT_COLLECTION_NAME);
	}

	public ConfigurableMongoDbMessageStore(MongoDbFactory mongoDbFactory, MappingMongoConverter mappingMongoConverter) {
		this(mongoDbFactory, mappingMongoConverter, DEFAULT_COLLECTION_NAME);
	}

	public ConfigurableMongoDbMessageStore(MongoDbFactory mongoDbFactory, String collectionName) {
		this(mongoDbFactory, null, collectionName);
	}

	public ConfigurableMongoDbMessageStore(MongoDbFactory mongoDbFactory, MappingMongoConverter mappingMongoConverter,
			String collectionName) {
		super(mongoDbFactory, mappingMongoConverter, collectionName);
	}

	/**
	 * Convenient injection point for expiry callbacks in the message store. Each of the callbacks provided will simply
	 * be registered with the store using {@link #registerMessageGroupExpiryCallback(MessageGroupCallback)}.
	 * @param expiryCallbacks the expiry callbacks to add
	 */
	public void setExpiryCallbacks(Collection<MessageGroupCallback> expiryCallbacks) {
		for (MessageGroupCallback callback : expiryCallbacks) {
			registerMessageGroupExpiryCallback(callback);
		}
	}

	public boolean isTimeoutOnIdle() {
		return this.timeoutOnIdle;
	}

	/**
	 * Allows you to override the rule for the timeout calculation. Typical timeout is based from the time
	 * the {@link MessageGroup} was created. If you want the timeout to be based on the time
	 * the {@link MessageGroup} was idling (e.g., inactive from the last update) invoke this method with 'true'.
	 * Default is 'false'.
	 * @param timeoutOnIdle The boolean.
	 */
	public void setTimeoutOnIdle(boolean timeoutOnIdle) {
		this.timeoutOnIdle = timeoutOnIdle;
	}

	@Override
	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		this.addMessageDocument(new MessageDocument(message));
		return message;
	}

	@Override
	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Query query = Query.query(Criteria.where(MessageDocumentFields.MESSAGE_ID).is(id));
		MessageDocument document = this.mongoTemplate.findAndRemove(query, MessageDocument.class, this.collectionName);
		return (document != null) ? document.getMessage() : null;
	}

	@Override
	public long getMessageCount() {
		Query query = Query.query(Criteria.where(MessageDocumentFields.MESSAGE_ID).exists(true)
				.and(MessageDocumentFields.GROUP_ID).exists(false));
		return this.mongoTemplate.getCollection(this.collectionName).count(query.getQueryObject());
	}


	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");

		Query query = groupOrderQuery(groupId);
		MessageDocument messageDocument = this.mongoTemplate.findOne(query, MessageDocument.class, this.collectionName);

		if (messageDocument != null) {
			long createdTime = messageDocument.getCreatedTime();
			long lastModifiedTime = messageDocument.getLastModifiedTime();
			boolean complete = messageDocument.isComplete();
			int lastReleasedSequence = messageDocument.getLastReleasedSequence();

			MessageGroup messageGroup = getMessageGroupFactory()
					.create(this, groupId, createdTime, complete);
			messageGroup.setLastModified(lastModifiedTime);
			messageGroup.setLastReleasedMessageSequenceNumber(lastReleasedSequence);
			return messageGroup;

		}
		else {
			return new SimpleMessageGroup(groupId);
		}
	}

	@Override
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		addMessagesToGroup(groupId, message);
		return getMessageGroup(groupId);
	}

	@Override
	public void addMessagesToGroup(Object groupId, Message<?>... messages) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messages, "'message' must not be null");

		Query query = groupOrderQuery(groupId);
		MessageDocument messageDocument = this.mongoTemplate.findOne(query, MessageDocument.class, this.collectionName);

		long createdTime = System.currentTimeMillis();
		int lastReleasedSequence = 0;
		boolean complete = false;

		if (messageDocument != null) {
			createdTime = messageDocument.getCreatedTime();
			lastReleasedSequence = messageDocument.getLastReleasedSequence();
			complete = messageDocument.isComplete();
		}

		for (Message<?> message : messages) {
			MessageDocument document = new MessageDocument(message);
			document.setGroupId(groupId);
			document.setComplete(complete);
			document.setLastReleasedSequence(lastReleasedSequence);
			document.setCreatedTime(createdTime);
			document.setLastModifiedTime(messageDocument == null ? createdTime : System.currentTimeMillis());
			document.setSequence(getNextId());

			addMessageDocument(document);
		}
	}

	@Override
	public void removeMessagesFromGroup(Object groupId, Collection<Message<?>> messages) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messages, "'messageToRemove' must not be null");

		Collection<UUID> ids = new ArrayList<UUID>();
		for (Message<?> messageToRemove : messages) {
			ids.add(messageToRemove.getHeaders().getId());
			if (ids.size() >= getRemoveBatchSize()) {
				removeMessages(groupId, ids);
				ids.clear();
			}
		}
		if (ids.size() > 0) {
			removeMessages(groupId, ids);
		}
		updateGroup(groupId, lastModifiedUpdate());
	}

	private void removeMessages(Object groupId, Collection<UUID> ids) {
		Query query = groupIdQuery(groupId)
				.addCriteria(Criteria.where(MessageDocumentFields.MESSAGE_ID).in(ids.toArray()));
		this.mongoTemplate.remove(query, this.collectionName);
	}

	@Override
	public void removeMessagesFromGroup(Object groupId, Message<?>... messages) {
		removeMessagesFromGroup(groupId, Arrays.asList(messages));
	}

	@Override
	public Message<?> pollMessageFromGroup(final Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");

		Sort sort = new Sort(MessageDocumentFields.LAST_MODIFIED_TIME, MessageDocumentFields.SEQUENCE);
		Query query = groupIdQuery(groupId).with(sort);
		MessageDocument document = mongoTemplate.findAndRemove(query, MessageDocument.class, collectionName);
		Message<?> message = null;
		if (document != null) {
			message = document.getMessage();
			updateGroup(groupId, lastModifiedUpdate());
		}
		return message;
	}

	@Override
	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		this.updateGroup(groupId, lastModifiedUpdate().set(MessageDocumentFields.LAST_RELEASED_SEQUENCE, sequenceNumber));
	}

	@Override
	public void completeGroup(Object groupId) {
		this.updateGroup(groupId, lastModifiedUpdate().set(MessageDocumentFields.COMPLETE, true));
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		List<MessageGroup> messageGroups = new ArrayList<MessageGroup>();

		Query query = Query.query(Criteria.where(MessageDocumentFields.GROUP_ID).exists(true));
		@SuppressWarnings("rawtypes")
		List groupIds = mongoTemplate.getCollection(collectionName)
				.distinct(MessageDocumentFields.GROUP_ID, query.getQueryObject());

		for (Object groupId : groupIds) {
			messageGroups.add(getMessageGroup(groupId));
		}

		return messageGroups.iterator();
	}

	@Override
	public void registerMessageGroupExpiryCallback(MessageGroupCallback callback) {
		this.expiryCallbacks.add(callback);
	}

	@Override
	@ManagedOperation
	public int expireMessageGroups(long timeout) {
		int count = 0;
		long threshold = System.currentTimeMillis() - timeout;
		for (MessageGroup group : this) {

			long timestamp = group.getTimestamp();
			if (this.isTimeoutOnIdle() && group.getLastModified() > 0) {
				timestamp = group.getLastModified();
			}

			if (timestamp <= threshold) {
				count++;
				expire(group);
			}
		}
		return count;
	}

	@Override
	@ManagedAttribute
	public int getMessageCountForAllMessageGroups() {
		Query query = Query.query(Criteria.where(MessageDocumentFields.MESSAGE_ID).exists(true)
				.and(MessageDocumentFields.GROUP_ID).exists(true));
		long count = this.mongoTemplate.count(query, this.collectionName);
		Assert.isTrue(count <= Integer.MAX_VALUE, "Message count is out of Integer's range");
		return (int) count;
	}

	@Override
	@ManagedAttribute
	public int getMessageGroupCount() {
		Query query = Query.query(Criteria.where(MessageDocumentFields.GROUP_ID).exists(true));
		return this.mongoTemplate.getCollection(this.collectionName)
				.distinct(MessageDocumentFields.GROUP_ID, query.getQueryObject())
				.size();
	}

	@Override
	public MessageGroupMetadata getGroupMetadata(Object groupId) {
		throw new UnsupportedOperationException("Not yet implemented for this store");
	}

	@Override
	public Message<?> getOneMessageFromGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Query query = groupOrderQuery(groupId);
		MessageDocument messageDocument = this.mongoTemplate.findOne(query, MessageDocument.class, this.collectionName);
		if (messageDocument != null) {
			return messageDocument.getMessage();
		}
		else {
			return null;
		}
	}

	@Override
	public Collection<Message<?>> getMessagesForGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Query query = groupOrderQuery(groupId);
		List<MessageDocument> documents = this.mongoTemplate.find(query, MessageDocument.class, this.collectionName);
		List<Message<?>> messages = new ArrayList<Message<?>>();

		for (MessageDocument document : documents) {
			messages.add(document.getMessage());
		}
		return messages;
	}

	private void expire(MessageGroup group) {

		RuntimeException exception = null;

		for (MessageGroupCallback callback : this.expiryCallbacks) {
			try {
				callback.execute(this, group);
			}
			catch (RuntimeException e) {
				if (exception == null) {
					exception = e;
				}
				logger.error("Exception in expiry callback", e);
			}
		}

		if (exception != null) {
			throw exception;
		}
	}


	private void updateGroup(Object groupId, Update update) {
		this.mongoTemplate.updateFirst(groupOrderQuery(groupId), update, this.collectionName);
	}

	private static Update lastModifiedUpdate() {
		return Update.update(MessageDocumentFields.LAST_MODIFIED_TIME, System.currentTimeMillis());
	}

	private static Query groupOrderQuery(Object groupId) {
		Sort sort = new Sort(Sort.Direction.DESC, MessageDocumentFields.LAST_MODIFIED_TIME,
				MessageDocumentFields.SEQUENCE);
		return groupIdQuery(groupId).with(sort);
	}

}

