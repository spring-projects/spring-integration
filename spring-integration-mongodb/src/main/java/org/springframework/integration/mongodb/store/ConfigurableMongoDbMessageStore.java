/*
 * Copyright 2013-2014 the original author or authors.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.IndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Order;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.PriorityCapableChannelMessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
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
 * @since 3.0
 */
public class ConfigurableMongoDbMessageStore extends AbstractMessageGroupStore
		implements MessageStore, ChannelMessageStore, PriorityCapableChannelMessageStore, InitializingBean, ApplicationContextAware {

	public final static String DEFAULT_COLLECTION_NAME = "configurableStoreMessages";

	public final static String SEQUENCE_NAME = "messagesSequence";

	/**
	 * The name of the message header that stores a flag to indicate that the message has been saved. This is an
	 * optimization for the put method.
	 */
	public static final String SAVED_KEY = ConfigurableMongoDbMessageStore.class.getSimpleName() + ".SAVED";

	/**
	 * The name of the message header that stores a timestamp for the time the message was inserted.
	 */
	public static final String CREATED_DATE_KEY = ConfigurableMongoDbMessageStore.class.getSimpleName() + ".CREATED_DATE";

	private static final String MESSAGE_ID = "messageId";

	private static final String PRIORITY = "priority";

	private static final String GROUP_ID = "groupId";

	private static final String LAST_MODIFIED_TIME = "lastModifiedTime";

	private static final String SEQUENCE = "sequence";

	private static final String LAST_RELEASED_SEQUENCE = "lastReleasedSequence";

	private static final String COMPLETE = "complete";

	private final String collectionName;

	private final MongoDbFactory mongoDbFactory;

	private volatile MongoTemplate mongoTemplate;

	private volatile MappingMongoConverter mappingMongoConverter;

	private boolean priorityEnabled;

	private ApplicationContext applicationContext;


	public ConfigurableMongoDbMessageStore(MongoTemplate mongoTemplate) {
		this(mongoTemplate, DEFAULT_COLLECTION_NAME);
	}

	public ConfigurableMongoDbMessageStore(MongoTemplate mongoTemplate, String collectionName) {
		Assert.notNull("'mongoTemplate' must not be null");
		Assert.hasText("'collectionName' must not be empty");
		this.collectionName = collectionName;
		this.mongoTemplate = mongoTemplate;
		this.mongoDbFactory = null;
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

	public ConfigurableMongoDbMessageStore(MongoDbFactory mongoDbFactory, MappingMongoConverter mappingMongoConverter, String collectionName) {
		Assert.notNull("'mongoDbFactory' must not be null");
		Assert.hasText("'collectionName' must not be empty");
		this.collectionName = collectionName;
		this.mongoDbFactory = mongoDbFactory;
		this.mappingMongoConverter = mappingMongoConverter;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
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
		if (this.mongoTemplate == null) {
			if (this.mappingMongoConverter == null) {
				this.mappingMongoConverter = new MappingMongoConverter(this.mongoDbFactory, new MongoMappingContext());
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
		indexOperations.ensureIndex(new Index(MESSAGE_ID, Order.ASCENDING));
		if (this.priorityEnabled) {
			indexOperations.ensureIndex(new Index(GROUP_ID, Order.ASCENDING)
					.on(PRIORITY, Order.DESCENDING)
					.on(LAST_MODIFIED_TIME, Order.ASCENDING)
					.on(SEQUENCE, Order.ASCENDING));
		}
		else {
			indexOperations.ensureIndex(new Index(GROUP_ID, Order.ASCENDING)
					.on(LAST_MODIFIED_TIME, Order.DESCENDING)
					.on(SEQUENCE, Order.DESCENDING));
		}
	}


	@Override
	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageDocument document = this.mongoTemplate.findOne(Query.query(Criteria.where(MESSAGE_ID).is(id)),
				MessageDocument.class, this.collectionName);
		return (document != null) ? document.getMessage() : null;
	}

	@Override
	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		this.addMessageDocument(new MessageDocument(message));
		return message;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addMessageDocument(MessageDocument document) {
		Message<?> message = document.getMessage();
		if (message.getHeaders().containsKey(SAVED_KEY)) {
			Message<?> saved = getMessage(message.getHeaders().getId());
			if (saved != null) {
				if (saved.equals(message)) {
					return;
				} // We need to save it under its own id
			}
		}

		final long createdDate = document.getCreatedTime() == 0 ? System.currentTimeMillis() : document.getCreatedTime();

		Message<?> result = this.getMessageBuilderFactory().fromMessage(message).setHeader(SAVED_KEY, Boolean.TRUE)
				.setHeader(CREATED_DATE_KEY, createdDate).build();

		Map innerMap = (Map) new DirectFieldAccessor(result.getHeaders()).getPropertyValue("headers");
		// using reflection to set ID since it is immutable through MessageHeaders
		innerMap.put(MessageHeaders.ID, message.getHeaders().get(MessageHeaders.ID));
		innerMap.put(MessageHeaders.TIMESTAMP, message.getHeaders().get(MessageHeaders.TIMESTAMP));

		document.setCreatedTime(createdDate);
		this.mongoTemplate.insert(document, this.collectionName);
	}

	@Override
	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageDocument document = this.mongoTemplate.findAndRemove(Query.query(Criteria.where(MESSAGE_ID).is(id)),
				MessageDocument.class, this.collectionName);
		return (document != null) ? document.getMessage() : null;
	}

	@Override
	public long getMessageCount() {
		return this.mongoTemplate.getCollection(this.collectionName).getCount();
	}


	@Override
	public int messageGroupSize(Object groupId) {
		long lCount = this.mongoTemplate.count(groupIdQuery(groupId), this.collectionName);
		Assert.isTrue(lCount <= Integer.MAX_VALUE, "Message count is out of Integer's range");
		return (int) lCount;
	}

	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		List<MessageDocument> messageDocuments = this.mongoTemplate.find(groupIdQuery(groupId), MessageDocument.class,
				this.collectionName);

		long createdTime = 0;
		long lastModifiedTime = 0;
		int lastReleasedSequence = 0;
		boolean complete = false;

		if (messageDocuments.size() > 0) {
			MessageDocument document = messageDocuments.get(0);
			createdTime = document.getCreatedTime();
			lastModifiedTime = document.getLastModifiedTime();
			complete = document.isComplete();
			lastReleasedSequence = document.getLastReleasedSequence();
		}

		List<Message<?>> messages = new ArrayList<Message<?>>();
		for (MessageDocument document : messageDocuments) {
			messages.add(document.getMessage());
		}
		SimpleMessageGroup group = new SimpleMessageGroup(messages, groupId, createdTime, complete);
		group.setLastReleasedMessageSequenceNumber(lastReleasedSequence);
		group.setLastModified(lastModifiedTime);

		return group;
	}

	@Override
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");
		MessageDocument messageDocument = this.mongoTemplate.findOne(groupIdQuery(groupId), MessageDocument.class,
				this.collectionName);

		long createdTime = 0;
		int lastReleasedSequence = 0;
		boolean complete = false;

		if (messageDocument != null) {
			createdTime = messageDocument.getCreatedTime();
			lastReleasedSequence = messageDocument.getLastReleasedSequence();
			complete = messageDocument.isComplete();
		}

		MessageDocument document = new MessageDocument(message);
		document.setGroupId(groupId);
		document.setComplete(complete);
		document.setLastReleasedSequence(lastReleasedSequence);
		document.setCreatedTime(createdTime == 0 ? System.currentTimeMillis() : createdTime);
		document.setLastModifiedTime(System.currentTimeMillis());
		document.setSequence(this.getNextId());

		this.addMessageDocument(document);

		return this.getMessageGroup(groupId);
	}

	@Override
	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");
		Query query = groupIdQuery(groupId).addCriteria(Criteria.where(MESSAGE_ID).is(messageToRemove.getHeaders().getId()));
		this.mongoTemplate.remove(query, this.collectionName);
		this.updateGroup(groupId, lastModifiedUpdate());
		return this.getMessageGroup(groupId);
	}

	@Override
	public void removeMessageGroup(Object groupId) {
		this.mongoTemplate.remove(groupIdQuery(groupId), this.collectionName);
	}

	@Override
	@SuppressWarnings({ "rawtypes" })
	public Iterator<MessageGroup> iterator() {
		Map<Object, MessageGroup> messageGroupMap = new HashMap<Object, MessageGroup>();
		Query query = Query.query(Criteria.where(GROUP_ID).exists(true));
		query.fields().include(GROUP_ID);
		List<Map> groupIds = this.mongoTemplate.find(query, Map.class, this.collectionName);
		for (Map groupId : groupIds) {
			Object key = groupId.get(GROUP_ID);
			if (!messageGroupMap.containsKey(key)) {
				messageGroupMap.put(key, this.getMessageGroup(groupId));
			}
		}
		return messageGroupMap.values().iterator();
	}

	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");

		Sort sort = new Sort(LAST_MODIFIED_TIME, SEQUENCE);
		if (this.priorityEnabled) {
			sort = new Sort(Sort.Direction.DESC, PRIORITY).and(sort);
		}
		Query query = groupIdQuery(groupId).with(sort);
		MessageDocument document = this.mongoTemplate.findAndRemove(query, MessageDocument.class, this.collectionName);
		Message<?> message = null;
		if (document != null) {
			message = document.getMessage();
			this.updateGroup(groupId, lastModifiedUpdate());
		}
		return message;
	}

	@Override
	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		this.updateGroup(groupId, lastModifiedUpdate().set(LAST_RELEASED_SEQUENCE, sequenceNumber));
	}

	@Override
	public void completeGroup(Object groupId) {
		this.updateGroup(groupId, lastModifiedUpdate().set(COMPLETE, true));
	}


	private void updateGroup(Object groupId, Update update) {
		this.mongoTemplate.updateFirst(groupIdQuery(groupId), update, this.collectionName);
	}

	private static Update lastModifiedUpdate() {
		return Update.update(LAST_MODIFIED_TIME, System.currentTimeMillis());
	}


	private static Query groupIdQuery(Object groupId) {
		return Query.query(Criteria.where(GROUP_ID).is(groupId));
	}

	private int getNextId() {
		Query query = Query.query(Criteria.where("_id").is(SEQUENCE_NAME));
		query.fields().include(SEQUENCE);
		return (Integer) this.mongoTemplate.findAndModify(query,
				new Update().inc(SEQUENCE, 1),
				FindAndModifyOptions.options().returnNew(true).upsert(true),
				Map.class,
				this.collectionName).get(SEQUENCE);
	}

	/**
	 * The entity class to wrap {@link Message} to the MongoDB document.
	 */
	private static class MessageDocument {

		/*
		 * Needed as a persistence property to suppress 'Cannot determine IsNewStrategy' MappingException
		 * when the application context is configured with auditing. The document is not
		 * currently Auditable.
		 */
		@SuppressWarnings("unused")
		@Id
		private String _id;

		private final Message<?> message;

		@SuppressWarnings("unused")
		private final UUID messageId;

		@SuppressWarnings("unused")
		private final Integer priority;

		private Long createdTime = 0L;

		@SuppressWarnings("unused")
		private Object groupId;

		private Long lastModifiedTime = 0L;

		private Boolean complete = false;

		private Integer lastReleasedSequence = 0;

		private int sequence;

		public MessageDocument(Message<?> message) {
			Assert.notNull(message, "'message' must not be null");
			this.message = message;
			this.messageId = message.getHeaders().getId();
			this.priority = new IntegrationMessageHeaderAccessor(message).getPriority();
		}

		public Message<?> getMessage() {
			return message;
		}

		public void setGroupId(Object groupId) {
			this.groupId = groupId;
		}

		public Long getLastModifiedTime() {
			return lastModifiedTime;
		}

		public void setLastModifiedTime(long lastModifiedTime) {
			this.lastModifiedTime = lastModifiedTime;
		}

		public Long getCreatedTime() {
			return createdTime;
		}

		public void setCreatedTime(long createdTime) {
			this.createdTime = createdTime;
		}

		public Boolean isComplete() {
			return complete;
		}

		public void setComplete(boolean complete) {
			this.complete = complete;
		}

		public Integer getLastReleasedSequence() {
			return lastReleasedSequence;
		}

		public void setLastReleasedSequence(int lastReleasedSequence) {
			this.lastReleasedSequence = lastReleasedSequence;
		}

		public void setSequence(int sequence) {
			this.sequence = sequence;
		}
	}

	/**
	 * A {@link GenericConverter} implementation to convert {@link Message} to
	 * serialized {@link byte[]} to store {@link Message} to the MongoDB.
	 * And vice versa - to convert {@link byte[]} from the MongoDB to the {@link Message}.
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

