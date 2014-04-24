/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.dao.DataAccessException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
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
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;


/**
 * An implementation of both the {@link MessageStore} and {@link MessageGroupStore}
 * strategies that relies upon MongoDB for persistence.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Sean Brandt
 * @author Jodie StJohn
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 */
public class MongoDbMessageStore extends AbstractMessageGroupStore
		implements MessageStore, BeanClassLoaderAware, ApplicationContextAware, InitializingBean {

	private final static String DEFAULT_COLLECTION_NAME = "messages";

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

	private final static String GROUP_ID_KEY = "_groupId";

	private final static String GROUP_COMPLETE_KEY = "_group_complete";

	private final static String LAST_RELEASED_SEQUENCE_NUMBER = "_last_released_sequence";

	private final static String GROUP_TIMESTAMP_KEY = "_group_timestamp";

	private final static String GROUP_UPDATE_TIMESTAMP_KEY = "_group_update_timestamp";

	private final static String CREATED_DATE = "_createdDate";

	private static final String SEQUENCE = "sequence";


	private final MongoTemplate template;

	private final MessageReadingMongoConverter converter;

	private final String collectionName;

	private volatile ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

	private ApplicationContext applicationContext;


	/**
	 * Create a MongoDbMessageStore using the provided {@link MongoDbFactory}.and the default collection name.
	 *
	 * @param mongoDbFactory The mongodb factory.
	 */
	public MongoDbMessageStore(MongoDbFactory mongoDbFactory) {
		this(mongoDbFactory, null);
	}

	/**
	 * Create a MongoDbMessageStore using the provided {@link MongoDbFactory} and collection name.
	 *
	 * @param mongoDbFactory The mongodb factory.
	 * @param collectionName The collection name.
	 */
	public MongoDbMessageStore(MongoDbFactory mongoDbFactory, String collectionName) {
		Assert.notNull(mongoDbFactory, "mongoDbFactory must not be null");
		this.converter = new MessageReadingMongoConverter(mongoDbFactory, new MongoMappingContext());
		this.template = new MongoTemplate(mongoDbFactory, this.converter);
		this.collectionName = (StringUtils.hasText(collectionName)) ? collectionName : DEFAULT_COLLECTION_NAME;
	}


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		Assert.notNull(classLoader, "classLoader must not be null");
		this.classLoader = classLoader;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.applicationContext != null) {
			this.template.setApplicationContext(this.applicationContext);
			this.converter.setApplicationContext(this.applicationContext);
		}
		this.converter.afterPropertiesSet();

		IndexOperations indexOperations = this.template.indexOps(this.collectionName);

		indexOperations.ensureIndex(new Index(GROUP_ID_KEY, Sort.Direction.ASC)
				.on(GROUP_UPDATE_TIMESTAMP_KEY, Sort.Direction.DESC)
				.on(SEQUENCE, Sort.Direction.DESC));
	}

	@Override
	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		this.addMessageDocument(new MessageWrapper(message));
		return message;
	}

	private void addMessageDocument(final MessageWrapper document) {
		this.template.executeInSession(new DbCallback<Void>() {
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

				final long createdDate = document.get_Group_timestamp() == 0 ? System.currentTimeMillis() : document.get_Group_timestamp();

				Message<?> result = getMessageBuilderFactory().fromMessage(message).setHeader(SAVED_KEY, Boolean.TRUE)
						.setHeader(CREATED_DATE_KEY, createdDate).build();

				@SuppressWarnings("unchecked")
				Map<String, Object> innerMap = (Map<String, Object>) new DirectFieldAccessor(result.getHeaders()).getPropertyValue("headers");
				// using reflection to set ID since it is immutable through MessageHeaders
				innerMap.put(MessageHeaders.ID, message.getHeaders().get(MessageHeaders.ID));
				innerMap.put(MessageHeaders.TIMESTAMP, message.getHeaders().get(MessageHeaders.TIMESTAMP));

				document.set_Group_timestamp(createdDate);
				template.insert(document, collectionName);
				return null;
			}
		});
	}

	@Override
	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper messageWrapper = this.template.findOne(whereMessageIdIs(id), MessageWrapper.class, this.collectionName);
		return (messageWrapper != null) ? messageWrapper.getMessage() : null;
	}

	@Override
	@ManagedAttribute
	public long getMessageCount() {
		return this.template.getCollection(this.collectionName).getCount();
	}

	@Override
	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper messageWrapper =  this.template.findAndRemove(whereMessageIdIs(id), MessageWrapper.class, this.collectionName);
		return (messageWrapper != null) ? messageWrapper.getMessage() : null;
	}

	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Query query = whereGroupIdOrder(groupId);
		List<MessageWrapper> messageWrappers = this.template.find(query, MessageWrapper.class, this.collectionName);
		List<Message<?>> messages = new ArrayList<Message<?>>();
		long timestamp = 0;
		long lastModified = 0;
		int lastReleasedSequenceNumber = 0;
		boolean completeGroup = false;
		if (messageWrappers.size() > 0){
			MessageWrapper messageWrapper = messageWrappers.get(0);
			timestamp = messageWrapper.get_Group_timestamp();
			lastModified = messageWrapper.get_Group_update_timestamp();
			completeGroup = messageWrapper.get_Group_complete();
			lastReleasedSequenceNumber = messageWrapper.get_LastReleasedSequenceNumber();
		}

		for (MessageWrapper messageWrapper : messageWrappers) {
			messages.add(messageWrapper.getMessage());
		}

		SimpleMessageGroup messageGroup = new SimpleMessageGroup(messages, groupId, timestamp, completeGroup);
		messageGroup.setLastModified(lastModified);
		if (lastReleasedSequenceNumber > 0){
			messageGroup.setLastReleasedMessageSequenceNumber(lastReleasedSequenceNumber);
		}

		return messageGroup;
	}

	@Override
	public MessageGroup addMessageToGroup(final Object groupId, final Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");
		return this.template.executeInSession(new DbCallback<MessageGroup>() {

			@Override
			public MessageGroup doInDB(DB db) throws MongoException, DataAccessException {
				Query query = whereGroupIdOrder(groupId);
				MessageWrapper messageDocument = template.findOne(query, MessageWrapper.class, collectionName);

				long createdTime = 0;
				int lastReleasedSequence = 0;
				boolean complete = false;

				if (messageDocument != null) {
					createdTime = messageDocument.get_Group_timestamp();
					lastReleasedSequence = messageDocument.get_LastReleasedSequenceNumber();
					complete = messageDocument.get_Group_complete();
				}


				MessageWrapper wrapper = new MessageWrapper(message);
				wrapper.set_GroupId(groupId);
				wrapper.set_Group_timestamp(createdTime == 0 ? System.currentTimeMillis() : createdTime);
				wrapper.set_Group_update_timestamp(System.currentTimeMillis());
				wrapper.set_Group_complete(complete);
				wrapper.set_LastReleasedSequenceNumber(lastReleasedSequence);
				wrapper.setSequence(getNextId());

				addMessageDocument(wrapper);
				return getMessageGroup(groupId);
			}
		});
	}

	@Override
	public MessageGroup removeMessageFromGroup(final Object groupId, final Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");

		return this.template.executeInSession(new DbCallback<MessageGroup>() {

			@Override
			public MessageGroup doInDB(DB db) throws MongoException, DataAccessException {
				template.findAndRemove(whereMessageIdIsAndGroupIdIs(messageToRemove.getHeaders().getId(), groupId),
						MessageWrapper.class, collectionName);
				updateGroup(groupId, lastModifiedUpdate());
				return getMessageGroup(groupId);
			}
		});
	}

	@Override
	public void removeMessageGroup(Object groupId) {
		this.template.remove(whereGroupIdIs(groupId), this.collectionName);
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		return this.template.executeInSession(new DbCallback<Iterator<MessageGroup>>() {

			@Override
			public Iterator<MessageGroup> doInDB(DB db) throws MongoException, DataAccessException {
				List<MessageGroup> messageGroups = new ArrayList<MessageGroup>();

				Query query = Query.query(Criteria.where(GROUP_ID_KEY).exists(true));
				@SuppressWarnings("rawtypes")
				List groupIds = template.getCollection(collectionName)
						.distinct(GROUP_ID_KEY, query.getQueryObject());

				for (Object groupId : groupIds) {
					messageGroups.add(getMessageGroup(groupId));
				}

				return messageGroups.iterator();
			}
		});
	}
	@Override
	public Message<?> pollMessageFromGroup(final Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		return this.template.executeInSession(new DbCallback<Message<?>>() {
			@Override
			public Message<?> doInDB(DB db) throws MongoException, DataAccessException {
				Query query = whereGroupIdIs(groupId).with(new Sort(GROUP_UPDATE_TIMESTAMP_KEY, SEQUENCE));
				MessageWrapper messageWrapper = template.findAndRemove(query, MessageWrapper.class, collectionName);
				Message<?> message = null;
				if (messageWrapper != null) {
					message = messageWrapper.getMessage();
				}
				updateGroup(groupId, lastModifiedUpdate());
				return message;
			}
		});
	}

	@Override
	public int messageGroupSize(Object groupId) {
		long lCount = this.template.count(new Query(Criteria.where(GROUP_ID_KEY).is(groupId)), this.collectionName);
		Assert.isTrue(lCount <= Integer.MAX_VALUE, "Message count is out of Integer's range");
		return (int) lCount;
	}

	@Override
	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		this.updateGroup(groupId, lastModifiedUpdate().set(LAST_RELEASED_SEQUENCE_NUMBER, sequenceNumber));
	}

	@Override
	public void completeGroup(Object groupId) {
		this.updateGroup(groupId, lastModifiedUpdate().set(GROUP_COMPLETE_KEY, true));
	}

	@Override
	@ManagedAttribute
	public int getMessageCountForAllMessageGroups() {
		Query query = Query.query(Criteria.where(MessageDocumentFields.MESSAGE_ID).exists(true)
				.and(MessageDocumentFields.GROUP_ID).exists(true));
		long count = this.template.count(query, this.collectionName);
		Assert.isTrue(count <= Integer.MAX_VALUE, "Message count is out of Integer's range");
		return (int) count;
	}

	@Override
	@ManagedAttribute
	public int getMessageGroupCount() {
		Query query = Query.query(Criteria.where(MessageDocumentFields.GROUP_ID).exists(true));
		return this.template.getCollection(this.collectionName)
				.distinct(MessageDocumentFields.GROUP_ID, query.getQueryObject())
				.size();
	}

	private static Update lastModifiedUpdate() {
		return Update.update(GROUP_UPDATE_TIMESTAMP_KEY, System.currentTimeMillis());
	}

	/*
	 * Common Queries
	 */

	private static Query whereMessageIdIs(UUID id) {
		return new Query(Criteria.where("headers.id._value").is(id.toString()));
	}

	private static Query whereMessageIdIsAndGroupIdIs(UUID id, Object groupId) {
		return new Query(Criteria.where("headers.id._value").is(id.toString()).and(GROUP_ID_KEY).is(groupId));
	}


	private static Query whereGroupIdOrder(Object groupId) {
		return whereGroupIdIs(groupId).with(new Sort(Sort.Direction.DESC, GROUP_UPDATE_TIMESTAMP_KEY, SEQUENCE));
	}

	private static Query whereGroupIdIs(Object groupId) {
		return new Query(Criteria.where(GROUP_ID_KEY).is(groupId));
	}

	private void updateGroup(Object groupId, Update update) {
		Query query = whereGroupIdIs(groupId).with(new Sort(Sort.Direction.DESC, GROUP_UPDATE_TIMESTAMP_KEY, SEQUENCE));
		this.template.updateFirst(query, update, this.collectionName);
	}

	private int getNextId() {
		Query query = Query.query(Criteria.where("_id").is(SEQUENCE_NAME));
		query.fields().include(SEQUENCE);
		return (Integer) this.template.findAndModify(query,
				new Update().inc(SEQUENCE, 1),
				FindAndModifyOptions.options().returnNew(true).upsert(true),
				Map.class,
				this.collectionName).get(SEQUENCE);
	}

	/**
	 * Custom implementation of the {@link MappingMongoConverter} strategy.
	 */
	private class MessageReadingMongoConverter extends MappingMongoConverter {

		public MessageReadingMongoConverter(MongoDbFactory mongoDbFactory,
				MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
			super(new DefaultDbRefResolver(mongoDbFactory), mappingContext);
		}

		@Override
		public void afterPropertiesSet() {
			List<Object> customConverters = new ArrayList<Object>();
			customConverters.add(new UuidToDBObjectConverter());
			customConverters.add(new DBObjectToUUIDConverter());
			customConverters.add(new MessageHistoryToDBObjectConverter());
			customConverters.add(new DBObjectToGenericMessageConverter());
			customConverters.add(new DBObjectToMutableMessageConverter());
			customConverters.add(new DBObjectToErrorMessageConverter());
			customConverters.add(new DBObjectToAdviceMessageConverter());
			customConverters.add(new ThrowableToBytesConverter());
			this.setCustomConversions(new CustomConversions(customConverters));
			super.afterPropertiesSet();
		}

		@Override
		public void write(Object source, DBObject target) {
			Assert.isInstanceOf(MessageWrapper.class, source);

			target.put(CREATED_DATE, System.currentTimeMillis());

			super.write(source, target);
		}

		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public <S> S read(Class<S> clazz, DBObject source) {
			if (!MessageWrapper.class.equals(clazz)) {
				return super.read(clazz, source);
			}
			if (source != null) {
				Message<?> message = null;
				Object messageType = source.get("_messageType");
				if (messageType == null) {
					messageType = GenericMessage.class.getName();
				}
				try {
					message = (Message<?>) this.read(ClassUtils.forName(messageType.toString(), classLoader), source);
				}
				catch (ClassNotFoundException e) {
					throw new IllegalStateException("failed to load class: " + messageType, e);
				}

				Long groupTimestamp = (Long)source.get(GROUP_TIMESTAMP_KEY);
				Long lastModified = (Long)source.get(GROUP_UPDATE_TIMESTAMP_KEY);
				Integer lastReleasedSequenceNumber = (Integer)source.get(LAST_RELEASED_SEQUENCE_NUMBER);
				Boolean completeGroup = (Boolean)source.get(GROUP_COMPLETE_KEY);

				MessageWrapper wrapper = new MessageWrapper(message);

				if (source.containsField(GROUP_ID_KEY)){
					wrapper.set_GroupId(source.get(GROUP_ID_KEY));
				}
				if (groupTimestamp != null){
					wrapper.set_Group_timestamp(groupTimestamp);
				}
				if (lastModified != null){
					wrapper.set_Group_update_timestamp(lastModified);
				}
				if (lastReleasedSequenceNumber != null){
					wrapper.set_LastReleasedSequenceNumber(lastReleasedSequenceNumber);
				}

				if (completeGroup != null){
					wrapper.set_Group_complete(completeGroup);
				}

				return (S) wrapper;
			}
			return null;
		}

		private Map<String, Object> normalizeHeaders(Map<String, Object> headers) {
			Map<String, Object> normalizedHeaders = new HashMap<String, Object>();
			for (String headerName : headers.keySet()) {
				Object headerValue = headers.get(headerName);
				if (headerValue instanceof DBObject) {
					DBObject source = (DBObject) headerValue;
					try {
						Class<?> typeClass = null;
						if (source.containsField("_class")) {
							Object type = source.get("_class");
							typeClass = ClassUtils.forName(type.toString(), classLoader);
						}
						else if (source instanceof BasicDBList) {
							typeClass = List.class;
						}
						else {
							throw new IllegalStateException("Unsupported 'DBObject' type: " + source.getClass());
						}
						normalizedHeaders.put(headerName, super.read(typeClass, source));
					}
					catch (Exception e) {
						logger.warn("Header '" + headerName + "' could not be deserialized.", e);
					}
				}
				else {
					normalizedHeaders.put(headerName, headerValue);
				}
			}
			return normalizedHeaders;
		}

		private Object extractPayload(DBObject source) {
			Object payload = source.get("payload");
			if (payload instanceof DBObject) {
				DBObject payloadObject = (DBObject) payload;
				Object payloadType = payloadObject.get("_class");
				try {
					Class<?> payloadClass = ClassUtils.forName(payloadType.toString(), classLoader);
					payload = this.read(payloadClass, payloadObject);
				}
				catch (Exception e) {
					throw new IllegalStateException("failed to load class: " + payloadType, e);
				}
			}
			return payload;
		}

	}

	@SuppressWarnings("unchecked")
	private static void enhanceHeaders(MessageHeaders messageHeaders, Map<String, Object> headers) {
		Map<String, Object> innerMap = (Map<String, Object>) new DirectFieldAccessor(messageHeaders).getPropertyValue("headers");
		// using reflection to set ID and TIMESTAMP since they are immutable through MessageHeaders
		innerMap.put(MessageHeaders.ID, headers.get(MessageHeaders.ID));
		innerMap.put(MessageHeaders.TIMESTAMP, headers.get(MessageHeaders.TIMESTAMP));
	}


	private static class UuidToDBObjectConverter implements Converter<UUID, DBObject> {
		@Override
		public DBObject convert(UUID source) {
			BasicDBObject dbObject = new BasicDBObject();
			dbObject.put("_value", source.toString());
			dbObject.put("_class", source.getClass().getName());
			return dbObject;
		}
	}

	private static class DBObjectToUUIDConverter implements Converter<DBObject, UUID> {
		@Override
		public UUID convert(DBObject source) {
			return UUID.fromString((String) source.get("_value"));
		}
	}


	private static class MessageHistoryToDBObjectConverter implements Converter<MessageHistory,DBObject> {

		@Override
		public DBObject convert(MessageHistory source) {
			BasicDBObject obj = new BasicDBObject();
			obj.put("_class", MessageHistory.class.getName());
			BasicDBList dbList = new BasicDBList();
			for (Properties properties : source) {
				BasicDBObject dbo = new BasicDBObject();
				dbo.put(MessageHistory.NAME_PROPERTY, properties.getProperty(MessageHistory.NAME_PROPERTY));
				dbo.put(MessageHistory.TYPE_PROPERTY, properties.getProperty(MessageHistory.TYPE_PROPERTY));
				dbo.put(MessageHistory.TIMESTAMP_PROPERTY, properties.getProperty(MessageHistory.TIMESTAMP_PROPERTY));
				dbList.add(dbo);
			}
			obj.put("components", dbList);
			return obj;
		}
	}

	private class DBObjectToGenericMessageConverter implements Converter<DBObject, GenericMessage<?>> {

		@Override

		public GenericMessage<?> convert(DBObject source) {
			@SuppressWarnings("unchecked")
			Map<String, Object> headers = MongoDbMessageStore.this.converter.normalizeHeaders((Map<String, Object>) source.get("headers"));

			GenericMessage<?> message = new GenericMessage<Object>(MongoDbMessageStore.this.converter.extractPayload(source), headers);
			enhanceHeaders(message.getHeaders(), headers);
			return message;
		}

	}

	private class DBObjectToMutableMessageConverter implements GenericConverter {

		private final Class<?> mutableMessageClass;

		private DBObjectToMutableMessageConverter() {
			try {
				this.mutableMessageClass = ClassUtils.forName("org.springframework.integration.support.MutableMessage",
						MongoDbMessageStore.this.classLoader);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			Set<ConvertiblePair> convertiblePairs = new HashSet<ConvertiblePair>();
			convertiblePairs.add(new ConvertiblePair(DBObject.class, this.mutableMessageClass));
			return convertiblePairs;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			DBObject dbObject = (DBObject) source;
			@SuppressWarnings("unchecked")
			Map<String, Object> headers = MongoDbMessageStore.this.converter.normalizeHeaders((Map<String, Object>) dbObject.get("headers"));

			return MutableMessageBuilder.withPayload(MongoDbMessageStore.this.converter.extractPayload(dbObject)).copyHeaders(headers).build();
		}
	}

	private class DBObjectToAdviceMessageConverter implements Converter<DBObject, AdviceMessage> {

		@Override
		public AdviceMessage convert(DBObject source) {
			@SuppressWarnings("unchecked")
			Map<String, Object> headers = MongoDbMessageStore.this.converter.normalizeHeaders((Map<String, Object>) source.get("headers"));

			Message<?> inputMessage = null;

			if (source.get("inputMessage") != null) {
				DBObject inputMessageObject = (DBObject) source.get("inputMessage");
				Object inputMessageType = inputMessageObject.get("_class");
				try {
					Class<?> messageClass = ClassUtils.forName(inputMessageType.toString(), classLoader);
					inputMessage = (Message<?>) MongoDbMessageStore.this.converter.read(messageClass, inputMessageObject);
				}
				catch (Exception e) {
					throw new IllegalStateException("failed to load class: " + inputMessageType, e);
				}
			}

			AdviceMessage message = new AdviceMessage(MongoDbMessageStore.this.converter.extractPayload(source), headers, inputMessage);
			enhanceHeaders(message.getHeaders(), headers);

			return message;
		}

	}

	private class DBObjectToErrorMessageConverter implements Converter<DBObject, ErrorMessage> {

		private final Converter<byte[], Object> deserializingConverter = new DeserializingConverter();

		@Override
		public ErrorMessage convert(DBObject source) {
			@SuppressWarnings("unchecked")
			Map<String, Object> headers = MongoDbMessageStore.this.converter.normalizeHeaders((Map<String, Object>) source.get("headers"));

			Object payload = this.deserializingConverter.convert((byte[]) source.get("payload"));
			ErrorMessage message = new ErrorMessage((Throwable) payload, headers);
			enhanceHeaders(message.getHeaders(), headers);

			return message;
		}

	}

	@WritingConverter
	private class ThrowableToBytesConverter implements Converter<Throwable, byte[]> {

		private final Converter<Object, byte[]> serializingConverter = new SerializingConverter();

		@Override
		public byte[] convert(Throwable source) {
			return serializingConverter.convert(source);
		}

	}


	/**
	 * Wrapper class used for storing Messages in MongoDB along with their "group" metadata.
	 */
	private static final class MessageWrapper {

		/*
		 * Needed as a persistence property to suppress 'Cannot determine IsNewStrategy' MappingException
		 * when the application context is configured with auditing. The document is not
		 * currently Auditable.
		 */
		@SuppressWarnings("unused")
		@Id
		private String _id;

		private volatile Object _groupId;

		@Transient
		private final Message<?> message;

		@SuppressWarnings("unused")
		private final String _messageType;

		@SuppressWarnings("unused")
		private final Object payload;

		@SuppressWarnings("unused")
		private final Map<String, ?> headers;

		@SuppressWarnings("unused")
		private final Message<?> inputMessage;

		private volatile long _group_timestamp;

		private volatile long _group_update_timestamp;

		private volatile int _last_released_sequence;

		private volatile boolean _group_complete;

		@SuppressWarnings("unused")
		private int sequence;

		public MessageWrapper(Message<?> message) {
			Assert.notNull(message, "'message' must not be null");
			this.message = message;
			this._messageType = message.getClass().getName();
			this.payload = message.getPayload();
			this.headers = message.getHeaders();
			if (message instanceof AdviceMessage) {
				this.inputMessage = ((AdviceMessage) message).getInputMessage();
			}
			else {
				this.inputMessage = null;
			}
		}

		public int get_LastReleasedSequenceNumber() {
			return _last_released_sequence;
		}

		public long get_Group_timestamp() {
			return _group_timestamp;
		}

		public boolean get_Group_complete() {
			return _group_complete;
		}

		public Object get_GroupId() {
			return _groupId;
		}

		public Message<?> getMessage() {
			return message;
		}

		public void set_GroupId(Object groupId) {
			this._groupId = groupId;
		}

		public void set_Group_timestamp(long groupTimestamp) {
			this._group_timestamp = groupTimestamp;
		}

		public long get_Group_update_timestamp() {
			return _group_update_timestamp;
		}

		public void set_Group_update_timestamp(long lastModified) {
			this._group_update_timestamp = lastModified;
		}

		public void set_LastReleasedSequenceNumber(int lastReleasedSequenceNumber) {
			this._last_released_sequence = lastReleasedSequenceNumber;
		}

		public void set_Group_complete(boolean completedGroup) {
			this._group_complete = completedGroup;
		}

		public void setSequence(int sequence) {
			this.sequence = sequence;
		}

	}

}
