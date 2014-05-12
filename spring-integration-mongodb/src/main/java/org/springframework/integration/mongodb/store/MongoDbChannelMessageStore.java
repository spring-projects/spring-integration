/*
 * Copyright 2014 the original author or authors.
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

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.PriorityCapableChannelMessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * MongoDB {@link PriorityCapableChannelMessageStore} implementation.
 * This message store shall be used for message channels only.
 *
 * <p>Provide the {@link #priorityEnabled} option to allow to poll messages via {@code priority} manner.
 *
 * <p>As a priority document field the {@link org.springframework.integration.IntegrationMessageHeaderAccessor#PRIORITY}
 * message header is used.
 *
 * <p>The same collection can be used for {@code org.springframework.integration.channel.QueueChannel}s and
 * {@code org.springframework.integration.channel.PriorityChannel}s, but the different instances of
 * {@link MongoDbChannelMessageStore} should be used for those cases, and the last one with
 * {@code priorityEnabled = true} option.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class MongoDbChannelMessageStore extends AbstractConfigurableMongoDbMessageStore
		implements PriorityCapableChannelMessageStore {

	public final static String DEFAULT_COLLECTION_NAME = "channelMessages";

	private volatile boolean priorityEnabled;

	public MongoDbChannelMessageStore(MongoTemplate mongoTemplate) {
		this(mongoTemplate, DEFAULT_COLLECTION_NAME);
	}

	public MongoDbChannelMessageStore(MongoTemplate mongoTemplate, String collectionName) {
		super(mongoTemplate, collectionName);
	}

	public MongoDbChannelMessageStore(MongoDbFactory mongoDbFactory) {
		this(mongoDbFactory, null, DEFAULT_COLLECTION_NAME);
	}

	public MongoDbChannelMessageStore(MongoDbFactory mongoDbFactory, MappingMongoConverter mappingMongoConverter) {
		this(mongoDbFactory, mappingMongoConverter, DEFAULT_COLLECTION_NAME);
	}

	public MongoDbChannelMessageStore(MongoDbFactory mongoDbFactory, String collectionName) {
		this(mongoDbFactory, null, collectionName);
	}

	public MongoDbChannelMessageStore(MongoDbFactory mongoDbFactory, MappingMongoConverter mappingMongoConverter, String collectionName) {
		super(mongoDbFactory, mappingMongoConverter, collectionName);
	}

	public void setPriorityEnabled(boolean priorityEnabled) {
		this.priorityEnabled = priorityEnabled;
	}

	@Override
	public boolean isPriorityEnabled() {
		return this.priorityEnabled;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		this.mongoTemplate.indexOps(this.collectionName)
				.ensureIndex(new Index(MessageDocumentFields.GROUP_ID, Sort.Direction.ASC)
						.on(MessageDocumentFields.PRIORITY, Sort.Direction.DESC)
						.on(MessageDocumentFields.LAST_MODIFIED_TIME, Sort.Direction.ASC)
						.on(MessageDocumentFields.SEQUENCE, Sort.Direction.ASC));
	}

	@Override
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");

		MessageDocument document = new MessageDocument(message);
		document.setGroupId(groupId);
		document.setCreatedTime(System.currentTimeMillis());
		document.setLastModifiedTime(System.currentTimeMillis());
		if (this.priorityEnabled) {
			document.setPriority(new IntegrationMessageHeaderAccessor(message).getPriority());
		}
		document.setSequence(this.getNextId());

		this.addMessageDocument(document);
		return this.getMessageGroup(groupId);
	}

	/**
	 * Not fully used. Only wraps the provided group id.
	 */
	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		return new SimpleMessageGroup(groupId);
	}

	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");

		Sort sort = new Sort(MessageDocumentFields.LAST_MODIFIED_TIME, MessageDocumentFields.SEQUENCE);
		if (this.priorityEnabled) {
			sort = new Sort(Sort.Direction.DESC, MessageDocumentFields.PRIORITY).and(sort);
		}
		Query query = groupIdQuery(groupId).with(sort);
		MessageDocument document = this.mongoTemplate.findAndRemove(query, MessageDocument.class, this.collectionName);
		Message<?> message = null;
		if (document != null) {
			message = document.getMessage();
		}
		return message;
	}

}
