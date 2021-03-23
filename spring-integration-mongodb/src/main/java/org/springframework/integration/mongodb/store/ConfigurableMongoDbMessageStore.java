/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.integration.mongodb.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An alternate MongoDB {@link MessageStore} and
 * {@link org.springframework.integration.store.MessageGroupStore} which allows the user to
 * configure the instance of {@link MongoTemplate}. The mechanism of storing the messages/group of messages
 * in the store is and is different from {@link MongoDbMessageStore}. Since the store uses serialization of the
 * messages by default, all the headers, and the payload of the Message must implement {@link java.io.Serializable}
 * interface
 *
 * @author Amol Nayak
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
public class ConfigurableMongoDbMessageStore extends AbstractConfigurableMongoDbMessageStore
		implements MessageStore {

	private static final String GROUP_ID_MUST_NOT_BE_NULL = "'groupId' must not be null";

	public static final String DEFAULT_COLLECTION_NAME = "configurableStoreMessages";


	public ConfigurableMongoDbMessageStore(MongoTemplate mongoTemplate) {
		this(mongoTemplate, DEFAULT_COLLECTION_NAME);
	}

	public ConfigurableMongoDbMessageStore(MongoTemplate mongoTemplate, String collectionName) {
		super(mongoTemplate, collectionName);
	}

	public ConfigurableMongoDbMessageStore(MongoDatabaseFactory mongoDbFactory) {
		this(mongoDbFactory, null, DEFAULT_COLLECTION_NAME);
	}

	public ConfigurableMongoDbMessageStore(MongoDatabaseFactory mongoDbFactory,
			MappingMongoConverter mappingMongoConverter) {

		this(mongoDbFactory, mappingMongoConverter, DEFAULT_COLLECTION_NAME);
	}

	public ConfigurableMongoDbMessageStore(MongoDatabaseFactory mongoDbFactory, String collectionName) {
		this(mongoDbFactory, null, collectionName);
	}

	public ConfigurableMongoDbMessageStore(MongoDatabaseFactory mongoDbFactory,
			MappingMongoConverter mappingMongoConverter, String collectionName) {

		super(mongoDbFactory, mappingMongoConverter, collectionName);
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
		MessageDocument document = getMongoTemplate().findAndRemove(query, MessageDocument.class, this.collectionName);
		return (document != null) ? document.getMessage() : null;
	}

	@Override
	public long getMessageCount() {
		Query query = Query.query(Criteria.where(MessageDocumentFields.MESSAGE_ID).exists(true)
				.and(MessageDocumentFields.GROUP_ID).exists(false));
		return getMongoTemplate().getCollection(this.collectionName).countDocuments(query.getQueryObject());
	}


	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);

		Query query = groupOrderQuery(groupId);
		MessageDocument messageDocument = getMongoTemplate().findOne(query, MessageDocument.class, this.collectionName);

		if (messageDocument != null) {
			long createdTime = messageDocument.getGroupCreatedTime();
			long lastModifiedTime = messageDocument.getLastModifiedTime();
			boolean complete = messageDocument.isComplete();
			int lastReleasedSequence = messageDocument.getLastReleasedSequence();

			MessageGroup messageGroup = getMessageGroupFactory()
					.create(this, groupId, createdTime, complete);
			messageGroup.setLastModified(lastModifiedTime);
			messageGroup.setLastReleasedMessageSequenceNumber(lastReleasedSequence);
			messageGroup.setCondition(messageDocument.getCondition());
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
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		Assert.notNull(messages, "'message' must not be null");

		Query query = groupOrderQuery(groupId);
		MessageDocument messageDocument = getMongoTemplate().findOne(query, MessageDocument.class, this.collectionName);

		long createdTime = System.currentTimeMillis();
		int lastReleasedSequence = 0;
		boolean complete = false;

		String condition = null;

		if (messageDocument != null) {
			createdTime = messageDocument.getGroupCreatedTime();
			lastReleasedSequence = messageDocument.getLastReleasedSequence();
			complete = messageDocument.isComplete();
			condition = messageDocument.getCondition();
		}

		for (Message<?> message : messages) {
			MessageDocument document = new MessageDocument(message);
			document.setGroupId(groupId);
			document.setComplete(complete);
			document.setLastReleasedSequence(lastReleasedSequence);
			document.setGroupCreatedTime(createdTime);
			document.setLastModifiedTime(messageDocument == null ? createdTime : System.currentTimeMillis());
			document.setSequence(getNextId());
			if (condition != null) {
				document.setCondition(condition);
			}
			addMessageDocument(document);
		}
	}

	@Override
	public void removeMessagesFromGroup(Object groupId, Collection<Message<?>> messages) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		Assert.notNull(messages, "'messageToRemove' must not be null");

		Collection<UUID> ids = new ArrayList<>();
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
		getMongoTemplate().remove(query, this.collectionName);
	}

	@Override
	public Message<?> pollMessageFromGroup(final Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);

		Sort sort = Sort.by(MessageDocumentFields.LAST_MODIFIED_TIME, MessageDocumentFields.SEQUENCE);
		Query query = groupIdQuery(groupId).with(sort);
		MessageDocument document = getMongoTemplate().findAndRemove(query, MessageDocument.class, collectionName);
		Message<?> message = null;
		if (document != null) {
			message = document.getMessage();
			updateGroup(groupId, lastModifiedUpdate());
		}
		return message;
	}

	@Override
	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		updateGroup(groupId, lastModifiedUpdate().set(MessageDocumentFields.LAST_RELEASED_SEQUENCE, sequenceNumber));
	}

	@Override
	public void setGroupCondition(Object groupId, String condition) {
		updateGroup(groupId, lastModifiedUpdate().set("condition", condition));
	}

	@Override
	public void completeGroup(Object groupId) {
		updateGroup(groupId, lastModifiedUpdate().set(MessageDocumentFields.COMPLETE, true));
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		Query query = Query.query(Criteria.where(MessageDocumentFields.GROUP_ID).exists(true));
		Iterable<String> groupIds = getMongoTemplate().getCollection(collectionName)
				.distinct(MessageDocumentFields.GROUP_ID, query.getQueryObject(), String.class);

		return StreamSupport.stream(groupIds.spliterator(), false)
				.map(this::getMessageGroup)
				.iterator();

	}

	@Override
	@ManagedAttribute
	public int getMessageCountForAllMessageGroups() {
		Query query = Query.query(Criteria.where(MessageDocumentFields.MESSAGE_ID).exists(true)
				.and(MessageDocumentFields.GROUP_ID).exists(true));
		long count = getMongoTemplate().count(query, this.collectionName);
		Assert.isTrue(count <= Integer.MAX_VALUE, "Message count is out of Integer's range");
		return (int) count;
	}

	@Override
	@ManagedAttribute
	public int getMessageGroupCount() {
		Query query = Query.query(Criteria.where(MessageDocumentFields.GROUP_ID).exists(true));
		return getMongoTemplate()
				.findDistinct(query, MessageDocumentFields.GROUP_ID, this.collectionName, Object.class)
				.size();
	}

	@Override
	public Message<?> getOneMessageFromGroup(Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		Query query = groupOrderQuery(groupId);
		MessageDocument messageDocument = getMongoTemplate().findOne(query, MessageDocument.class, this.collectionName);
		if (messageDocument != null) {
			return messageDocument.getMessage();
		}
		else {
			return null;
		}
	}

	@Override
	public Collection<Message<?>> getMessagesForGroup(Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		Query query = groupOrderQuery(groupId);
		List<MessageDocument> documents = getMongoTemplate().find(query, MessageDocument.class, this.collectionName);

		return documents.stream()
				.map(MessageDocument::getMessage)
				.collect(Collectors.toList());
	}

	@Override
	public Stream<Message<?>> streamMessagesForGroup(Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		Query query = groupOrderQuery(groupId);
		Stream<MessageDocument> documents =
				getMongoTemplate()
						.stream(query, MessageDocument.class, this.collectionName)
						.stream();

		return documents.map(MessageDocument::getMessage);
	}

	private void updateGroup(Object groupId, Update update) {
		getMongoTemplate()
				.findAndModify(groupOrderQuery(groupId), update, FindAndModifyOptions.none(), Map.class,
						this.collectionName);
	}

	private static Update lastModifiedUpdate() {
		return Update.update(MessageDocumentFields.LAST_MODIFIED_TIME, System.currentTimeMillis());
	}

	private static Query groupOrderQuery(Object groupId) {
		Sort sort = Sort.by(Sort.Direction.DESC, MessageDocumentFields.LAST_MODIFIED_TIME,
				MessageDocumentFields.SEQUENCE);
		return groupIdQuery(groupId).with(sort);
	}

}

