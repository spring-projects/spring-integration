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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mongodb.DB;
import com.mongodb.MongoException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.DbCallback;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.IndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.store.BasicMessageGroupStore;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

/**
 * The abstract MongoDB {@link BasicMessageGroupStore} implementation to provide configuration for common options
 * for implementations of this class.
 *
 * @author Artem Bilan
 * @since 4.0
 */

public abstract class AbstractConfigurableMongoDbMessageStore implements BasicMessageGroupStore, InitializingBean,
		ApplicationContextAware {

	public final static String SEQUENCE_NAME = "messagesSequence";

	/**
	 * The name of the message header that stores a flag to indicate that the message has been saved. This is an
	 * optimization for the put method.
	 */
	public static final String SAVED_KEY = "MongoDbMessageStore.SAVED";

	/**
	 * The name of the message header that stores a timestamp for the time the message was inserted.
	 */
	public static final String CREATED_DATE_KEY = "MongoDbMessageStore.CREATED_DATE";

	protected final Log logger = LogFactory.getLog(this.getClass());

	protected final String collectionName;

	protected final MongoDbFactory mongoDbFactory;

	protected MongoTemplate mongoTemplate;

	protected MappingMongoConverter mappingMongoConverter;

	protected ApplicationContext applicationContext;

	protected MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	public AbstractConfigurableMongoDbMessageStore(MongoTemplate mongoTemplate, String collectionName) {
		Assert.notNull("'mongoTemplate' must not be null");
		Assert.hasText("'collectionName' must not be empty");
		this.collectionName = collectionName;
		this.mongoTemplate = mongoTemplate;
		this.mongoDbFactory = null;
	}

	public AbstractConfigurableMongoDbMessageStore(MongoDbFactory mongoDbFactory, String collectionName) {
		this(mongoDbFactory, null, collectionName);
	}

	public AbstractConfigurableMongoDbMessageStore(MongoDbFactory mongoDbFactory, MappingMongoConverter mappingMongoConverter, String collectionName) {
		Assert.notNull("'mongoDbFactory' must not be null");
		Assert.hasText("'collectionName' must not be empty");
		this.collectionName = collectionName;
		this.mongoDbFactory = mongoDbFactory;
		this.mappingMongoConverter = mappingMongoConverter;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.applicationContext);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.mongoTemplate == null) {
			if (this.mappingMongoConverter == null) {
				this.mappingMongoConverter = new MappingMongoConverter(new DefaultDbRefResolver(this.mongoDbFactory),
						new MongoMappingContext());
				this.mappingMongoConverter.setApplicationContext(this.applicationContext);
				List<Object> customConverters = new ArrayList<Object>();
				customConverters.add(new MongoDbMessageBytesConverter());
				this.mappingMongoConverter.setCustomConversions(new CustomConversions(customConverters));
				this.mappingMongoConverter.afterPropertiesSet();
			}
			this.mongoTemplate = new MongoTemplate(this.mongoDbFactory, this.mappingMongoConverter);
			if (this.applicationContext != null) {
				this.mongoTemplate.setApplicationContext(this.applicationContext);
			}
		}

		IndexOperations indexOperations = this.mongoTemplate.indexOps(this.collectionName);

		indexOperations.ensureIndex(new Index(MessageDocumentFields.MESSAGE_ID, Sort.Direction.ASC));

		indexOperations.ensureIndex(new Index(MessageDocumentFields.GROUP_ID, Sort.Direction.ASC)
				.on(MessageDocumentFields.LAST_MODIFIED_TIME, Sort.Direction.DESC)
				.on(MessageDocumentFields.SEQUENCE, Sort.Direction.DESC));
	}

	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Query query = Query.query(Criteria.where(MessageDocumentFields.MESSAGE_ID).is(id));
		MessageDocument document = this.mongoTemplate.findOne(query, MessageDocument.class, this.collectionName);
		return document != null ? document.getMessage() : null;
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
	protected int getNextId() {
		Query query = Query.query(Criteria.where("_id").is(SEQUENCE_NAME));
		query.fields().include(MessageDocumentFields.SEQUENCE);
		return (Integer) this.mongoTemplate.findAndModify(query,
				new Update().inc(MessageDocumentFields.SEQUENCE, 1),
				FindAndModifyOptions.options().returnNew(true).upsert(true),
				Map.class, this.collectionName)
				.get(MessageDocumentFields.SEQUENCE);
	}

	protected void addMessageDocument(final MessageDocument document) {
		this.mongoTemplate.executeInSession(new DbCallback<Void>() {
			@Override
			public Void doInDB(DB db) throws MongoException, DataAccessException {
				Message<?> message = document.getMessage();
				if (message.getHeaders().containsKey(SAVED_KEY)) {
					Message<?> saved = getMessage(message.getHeaders().getId());
					if (saved != null) {
						if (saved.equals(message)) {
							return null;
						} // We need to save it under its own id
					}
				}

				final long createdDate = document.getCreatedTime() == 0 ? System.currentTimeMillis() : document.getCreatedTime();

				Message<?> result = messageBuilderFactory.fromMessage(message).setHeader(SAVED_KEY, Boolean.TRUE)
						.setHeader(CREATED_DATE_KEY, createdDate).build();

				@SuppressWarnings("unchecked")
				Map<String, Object> innerMap = (Map<String, Object>) new DirectFieldAccessor(result.getHeaders()).getPropertyValue("headers");
				// using reflection to set ID since it is immutable through MessageHeaders
				innerMap.put(MessageHeaders.ID, message.getHeaders().get(MessageHeaders.ID));
				innerMap.put(MessageHeaders.TIMESTAMP, message.getHeaders().get(MessageHeaders.TIMESTAMP));

				document.setCreatedTime(createdDate);
				mongoTemplate.insert(document, collectionName);
				return null;
			}
		});
	}

	protected static Query groupIdQuery(Object groupId) {
		return Query.query(Criteria.where(MessageDocumentFields.GROUP_ID).is(groupId));
	}

	/**
	 * A {@link org.springframework.core.convert.converter.GenericConverter} implementation to convert {@link org.springframework.messaging.Message} to
	 * serialized {@link byte[]} to store {@link org.springframework.messaging.Message} to the MongoDB.
	 * And vice versa - to convert {@link byte[]} from the MongoDB to the {@link org.springframework.messaging.Message}.
	 */
	private static class MongoDbMessageBytesConverter implements GenericConverter {

		private final Converter<Object, byte[]> serializingConverter = new SerializingConverter();

		private final Converter<byte[], Object> deserializingConverter = new DeserializingConverter();

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			Set<ConvertiblePair> convertiblePairs = new HashSet<ConvertiblePair>();
			convertiblePairs.add(new ConvertiblePair(Message.class, byte[].class));
			convertiblePairs.add(new ConvertiblePair(byte[].class, Message.class));
			return convertiblePairs;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (Message.class.isAssignableFrom(sourceType.getObjectType())) {
				return serializingConverter.convert(source);
			}
			else {
				return deserializingConverter.convert((byte[]) source);
			}
		}

	}

}
