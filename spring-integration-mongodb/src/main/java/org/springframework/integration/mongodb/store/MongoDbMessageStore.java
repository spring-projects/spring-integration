/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.CREATED_DATE;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.DEFAULT_COLLECTION_NAME;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.GROUP_COMPLETE_KEY;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.GROUP_ID_KEY;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.GROUP_UPDATE_TIMESTAMP_KEY;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.LAST_RELEASED_SEQUENCE_NUMBER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Order;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.Message;
import org.springframework.integration.mongodb.MessageReadingMongoConverter;
import org.springframework.integration.mongodb.MessageWrapper;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


/**
 * An implementation of both the {@link MessageStore} and {@link MessageGroupStore}
 * strategies that relies upon MongoDB for persistence.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Sean Brandt
 * @author Amol Nayak
 *
 * @since 2.1
 */
public class MongoDbMessageStore extends AbstractMessageGroupStore implements MessageStore, BeanClassLoaderAware{


	private final MongoOperations operations;

	private final String collectionName;

	private final MessageReadingMongoConverter converter;

	/**
	 * Create a MongoDbMessageStore using the provided {@link MongoDbFactory}.and the default collection name.
	 */
	public MongoDbMessageStore(MongoDbFactory mongoDbFactory) {
		this(mongoDbFactory, null);
	}

	/**
	 * Create a MongoDbMessageStore using the provided {@link MongoDbFactory} and collection name.
	 */
	public MongoDbMessageStore(MongoDbFactory mongoDbFactory, String collectionName) {
		Assert.notNull(mongoDbFactory, "mongoDbFactory must not be null");
		converter = new MessageReadingMongoConverter(mongoDbFactory, new MongoMappingContext());
		converter.afterPropertiesSet();
		this.operations = new MongoTemplate(mongoDbFactory, converter);
		this.collectionName = (StringUtils.hasText(collectionName)) ? collectionName : DEFAULT_COLLECTION_NAME;
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		Assert.notNull(classLoader, "classLoader must not be null");
		converter.setBeanClassLoader(classLoader);
	}

	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		this.operations.insert(new MessageWrapper(message), this.collectionName);
		return message;
	}

	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper messageWrapper = this.operations.findOne(whereMessageIdIs(id), MessageWrapper.class, this.collectionName);
		return (messageWrapper != null) ? messageWrapper.getMessage() : null;
	}

	@ManagedAttribute
	public long getMessageCount() {
		return this.operations.getCollection(this.collectionName).getCount();
	}

	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper messageWrapper =  this.operations.findAndRemove(whereMessageIdIs(id), MessageWrapper.class, this.collectionName);
		return (messageWrapper != null) ? messageWrapper.getMessage() : null;
	}

	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		List<MessageWrapper> messageWrappers = this.operations.find(whereGroupIdIs(groupId), MessageWrapper.class, this.collectionName);
		List<Message<?>> messages = new ArrayList<Message<?>>();
		long timestamp = 0;
		long lastmodified = 0;
		int lastReleasedSequenceNumber = 0;
		boolean completeGroup = false;
		if (messageWrappers.size() > 0){
			MessageWrapper messageWrapper = messageWrappers.get(0);
			timestamp = messageWrapper.get_Group_timestamp();
			lastmodified = messageWrapper.get_Group_update_timestamp();
			completeGroup = messageWrapper.get_Group_complete();
			lastReleasedSequenceNumber = messageWrapper.get_LastReleasedSequenceNumber();
		}

		for (MessageWrapper messageWrapper : messageWrappers) {
			messages.add(messageWrapper.getMessage());
		}

		SimpleMessageGroup messageGroup = new SimpleMessageGroup(messages, groupId, timestamp, completeGroup);
		messageGroup.setLastModified(lastmodified);
		if (lastReleasedSequenceNumber > 0){
			messageGroup.setLastReleasedMessageSequenceNumber(lastReleasedSequenceNumber);
		}

		return messageGroup;
	}

	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");
		MessageGroup messageGroup = this.getMessageGroup(groupId);

		long messageGroupTimestamp = messageGroup.getTimestamp();
		long lastModified = messageGroup.getLastModified();

		if (messageGroupTimestamp == 0){
			messageGroupTimestamp = System.currentTimeMillis();
			lastModified = messageGroupTimestamp;
		}
		else {
			lastModified = System.currentTimeMillis();
		}

		MessageWrapper wrapper = new MessageWrapper(message);
		wrapper.set_GroupId(groupId);
		wrapper.set_Group_timestamp(messageGroupTimestamp);
		wrapper.set_Group_update_timestamp(lastModified);
		wrapper.set_Group_complete(messageGroup.isComplete());
		wrapper.set_LastReleasedSequenceNumber(messageGroup.getLastReleasedMessageSequenceNumber());

		this.operations.insert(wrapper, this.collectionName);
		return this.getMessageGroup(groupId);
	}

	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");
		this.removeMessage(messageToRemove.getHeaders().getId());
		this.updateGroup(groupId);
		return this.getMessageGroup(groupId);
	}

	public void removeMessageGroup(Object groupId) {
		List<MessageWrapper> messageWrappers = this.operations.find(whereGroupIdIs(groupId), MessageWrapper.class, this.collectionName);
		for (MessageWrapper messageWrapper : messageWrappers) {
			this.removeMessageFromGroup(groupId, messageWrapper.getMessage());
		}
	}

	public Iterator<MessageGroup> iterator() {
		List<MessageWrapper> groupedMessages = this.operations.find(whereGroupIdExists(), MessageWrapper.class, this.collectionName);
		Map<Object, MessageGroup> messageGroups = new HashMap<Object, MessageGroup>();
		for (MessageWrapper groupedMessage : groupedMessages) {
			Object groupId = groupedMessage.get_GroupId();
			if (!messageGroups.containsKey(groupId)) {
				messageGroups.put(groupId, this.getMessageGroup(groupId));
			}
		}
		return messageGroups.values().iterator();
	}

	public void completeGroup(Object groupId) {
		Update update = Update.update(GROUP_COMPLETE_KEY, true);
		Query q = whereGroupIdIs(groupId);
		this.operations.updateFirst(q, update, this.collectionName);
		this.updateGroup(groupId);
	}

	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		Update update = Update.update(LAST_RELEASED_SEQUENCE_NUMBER, sequenceNumber);
		Query q = whereGroupIdIs(groupId);
		this.operations.updateFirst(q, update, this.collectionName);
		this.updateGroup(groupId);
	}

	public Message<?> pollMessageFromGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		List<MessageWrapper> messageWrappers = this.operations.find(whereGroupIdIsOrdered(groupId), MessageWrapper.class, this.collectionName);
		Message<?> message = null;

		if (!CollectionUtils.isEmpty(messageWrappers)){
			message = messageWrappers.get(0).getMessage();
			this.removeMessageFromGroup(groupId, message);
		}
		this.updateGroup(groupId);
		return message;
	}

	public int messageGroupSize(Object groupId) {
		long lCount = this.operations.count(new Query(where(GROUP_ID_KEY).is(groupId)), this.collectionName);
		Assert.isTrue(lCount <= Integer.MAX_VALUE, "Message count is out of Integer's range");
		return (int) lCount;
	}

	/*
	 * Common Queries
	 */

	private static Query whereMessageIdIs(UUID id) {
		return new Query(where("headers.id._value").is(id.toString()));
	}

	private static Query whereGroupIdIs(Object groupId) {
		Query q = new Query(where(GROUP_ID_KEY).is(groupId));
		q.sort().on(GROUP_UPDATE_TIMESTAMP_KEY, Order.DESCENDING);
		return q;
	}

	private static Query whereGroupIdExists() {
		return new Query(where(GROUP_ID_KEY).exists(true));
	}

	private static Query whereGroupIdIsOrdered(Object groupId) {
		Query q = new Query(where(GROUP_ID_KEY).is(groupId)).limit(1);
		q.sort().on(CREATED_DATE, Order.ASCENDING);
		return q;
	}

	private void updateGroup(Object groupId) {
		Update update = Update.update(GROUP_UPDATE_TIMESTAMP_KEY, System.currentTimeMillis());
		Query q = whereGroupIdIs(groupId);
		this.operations.updateFirst(q, update, this.collectionName);
	}
}
