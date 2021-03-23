/*
 * Copyright 2014-2021 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.mongodb.support.BinaryToMessageConverter;
import org.springframework.integration.mongodb.support.MessageToBinaryConverter;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageMetadata;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The abstract MongoDB {@link AbstractMessageGroupStore} implementation to provide configuration for common options
 * for implementations of this class.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */

public abstract class AbstractConfigurableMongoDbMessageStore extends AbstractMessageGroupStore
		implements InitializingBean, ApplicationContextAware {

	public static final String SEQUENCE_NAME = "messagesSequence";

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR - final

	private static final RuntimeException NOT_IMPLEMENTED =
			new UnsupportedOperationException("The operation isn't implemented for this class.");

	protected final String collectionName; // NOSONAR - final

	protected final MongoDatabaseFactory mongoDbFactory; // NOSONAR - final

	private MongoTemplate mongoTemplate;

	private MappingMongoConverter mappingMongoConverter;

	private ApplicationContext applicationContext;

	private MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	public AbstractConfigurableMongoDbMessageStore(MongoTemplate mongoTemplate, String collectionName) {
		Assert.notNull(mongoTemplate, "'mongoTemplate' must not be null");
		Assert.hasText(collectionName, "'collectionName' must not be empty");
		this.collectionName = collectionName;
		this.mongoTemplate = mongoTemplate;
		this.mongoDbFactory = null;
	}

	public AbstractConfigurableMongoDbMessageStore(MongoDatabaseFactory mongoDbFactory, String collectionName) {
		this(mongoDbFactory, null, collectionName);
	}

	public AbstractConfigurableMongoDbMessageStore(MongoDatabaseFactory mongoDbFactory,
			MappingMongoConverter mappingMongoConverter, String collectionName) {
		Assert.notNull(mongoDbFactory, "'mongoDbFactory' must not be null");
		Assert.hasText(collectionName, "'collectionName' must not be empty");
		this.collectionName = collectionName;
		this.mongoDbFactory = mongoDbFactory;
		this.mappingMongoConverter = mappingMongoConverter;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	protected MongoTemplate getMongoTemplate() {
		return this.mongoTemplate;
	}

	protected MappingMongoConverter getMappingMongoConverter() {
		return this.mappingMongoConverter;
	}

	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		return this.messageBuilderFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.mongoTemplate == null) {
			if (this.mappingMongoConverter == null) {
				this.mappingMongoConverter = new MappingMongoConverter(new DefaultDbRefResolver(this.mongoDbFactory),
						new MongoMappingContext());
				this.mappingMongoConverter.setApplicationContext(this.applicationContext);
				List<Object> customConverters = new ArrayList<>();
				customConverters.add(new MessageToBinaryConverter());
				customConverters.add(new BinaryToMessageConverter());
				this.mappingMongoConverter.setCustomConversions(new MongoCustomConversions(customConverters));
				this.mappingMongoConverter.afterPropertiesSet();
			}
			this.mongoTemplate = new MongoTemplate(this.mongoDbFactory, this.mappingMongoConverter);
		}

		this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.applicationContext);

		IndexOperations indexOperations = this.mongoTemplate.indexOps(this.collectionName);

		indexOperations.ensureIndex(new Index(MessageDocumentFields.MESSAGE_ID, Sort.Direction.ASC));

		indexOperations.ensureIndex(
				new Index(MessageDocumentFields.GROUP_ID, Sort.Direction.ASC)
						.on(MessageDocumentFields.MESSAGE_ID, Sort.Direction.ASC)
						.unique());

		indexOperations.ensureIndex(
				new Index(MessageDocumentFields.GROUP_ID, Sort.Direction.ASC)
						.on(MessageDocumentFields.LAST_MODIFIED_TIME, Sort.Direction.DESC)
						.on(MessageDocumentFields.SEQUENCE, Sort.Direction.DESC));
	}

	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Query query = Query.query(Criteria.where(MessageDocumentFields.MESSAGE_ID).is(id));
		MessageDocument document = this.mongoTemplate.findOne(query, MessageDocument.class, this.collectionName);
		return document != null ? document.getMessage() : null;
	}

	public MessageMetadata getMessageMetadata(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Query query = Query.query(Criteria.where(MessageDocumentFields.MESSAGE_ID).is(id));
		MessageDocument document = this.mongoTemplate.findOne(query, MessageDocument.class, this.collectionName);
		if (document != null) {
			MessageMetadata messageMetadata = new MessageMetadata(id);
			messageMetadata.setTimestamp(document.getCreatedTime());
			return messageMetadata;
		}
		else {
			return null;
		}
	}

	@Override
	public void removeMessageGroup(Object groupId) {
		this.mongoTemplate.remove(groupIdQuery(groupId), this.collectionName);
	}

	@Override
	public int messageGroupSize(Object groupId) {
		long lCount = this.mongoTemplate.count(groupIdQuery(groupId), this.collectionName);
		Assert.isTrue(lCount <= Integer.MAX_VALUE, "Message count is out of Integer's range");
		return (int) lCount;
	}

	/**
	 * Perform MongoDB {@code INC} operation for the document, which contains the {@link MessageDocument}
	 * {@code sequence}, and return the new incremented value for the new {@link MessageDocument}.
	 * The {@link #SEQUENCE_NAME} document is created on demand.
	 * @return the next sequence value.
	 */
	protected long getNextId() {
		Query query = Query.query(Criteria.where("_id").is(SEQUENCE_NAME));
		query.fields().include(MessageDocumentFields.SEQUENCE);
		return ((Number) this.mongoTemplate.findAndModify(query,
				new Update().inc(MessageDocumentFields.SEQUENCE, 1L),
				FindAndModifyOptions.options().returnNew(true).upsert(true),
				Map.class, this.collectionName)
				.get(MessageDocumentFields.SEQUENCE))  // NOSONAR - never returns null
				.longValue();
	}

	protected void addMessageDocument(final MessageDocument document) {
		if (document.getGroupCreatedTime() == 0) {
			document.setGroupCreatedTime(System.currentTimeMillis());
		}
		document.setCreatedTime(System.currentTimeMillis());
		try {
			this.mongoTemplate.insert(document, this.collectionName);
		}
		catch (DuplicateKeyException e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("The Message with id [" + document.getMessageId() + "] already exists.\n" +
						"Ignoring INSERT and SELECT existing...");
			}
		}
	}

	protected static Query groupIdQuery(Object groupId) {
		return Query.query(Criteria.where(MessageDocumentFields.GROUP_ID).is(groupId));
	}

	@Override
	public void removeMessagesFromGroup(Object key, Collection<Message<?>> messages) {
		throw NOT_IMPLEMENTED;
	}

	@Override
	public void setGroupCondition(Object groupId, String condition) {
		throw NOT_IMPLEMENTED;
	}

	@Override
	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		throw NOT_IMPLEMENTED;
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		throw NOT_IMPLEMENTED;
	}

	@Override
	public void completeGroup(Object groupId) {
		throw NOT_IMPLEMENTED;
	}

	@Override
	public Message<?> getOneMessageFromGroup(Object groupId) {
		throw NOT_IMPLEMENTED;
	}

	@Override
	public void addMessagesToGroup(Object groupId, Message<?>... messages) {
		throw NOT_IMPLEMENTED;
	}

	@Override
	public Collection<Message<?>> getMessagesForGroup(Object groupId) {
		throw NOT_IMPLEMENTED;
	}

}
