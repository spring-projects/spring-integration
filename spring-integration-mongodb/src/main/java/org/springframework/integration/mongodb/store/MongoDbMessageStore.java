/*
 * Copyright 2002-2021 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
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
import org.springframework.integration.store.MessageMetadata;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;


/**
 * An implementation of both the {@link MessageStore} and
 * {@link org.springframework.integration.store.MessageGroupStore}
 * strategies that relies upon MongoDB for persistence.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Sean Brandt
 * @author Jodie StJohn
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class MongoDbMessageStore extends AbstractMessageGroupStore
		implements MessageStore, BeanClassLoaderAware, ApplicationContextAware, InitializingBean {

	public static final String SEQUENCE_NAME = "messagesSequence";

	private static final String HEADERS = "headers";

	private static final String UNCHECKED = "unchecked";

	private static final String GROUP_ID_MUST_NOT_BE_NULL = "'groupId' must not be null";

	private static final String DEFAULT_COLLECTION_NAME = "messages";

	private static final String GROUP_ID_KEY = "_groupId";

	private static final String GROUP_COMPLETE_KEY = "_group_complete";

	private static final String LAST_RELEASED_SEQUENCE_NUMBER = "_last_released_sequence";

	private static final String GROUP_TIMESTAMP_KEY = "_group_timestamp";

	private static final String GROUP_UPDATE_TIMESTAMP_KEY = "_group_update_timestamp";

	private static final String CREATED_DATE = "_createdDate";

	private static final String SEQUENCE = "sequence";


	private final MongoTemplate template;

	private final MessageReadingMongoConverter converter;

	private final String collectionName;

	private ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

	private ApplicationContext applicationContext;

	private String[] allowedPatterns;

	/**
	 * Create a MongoDbMessageStore using the provided {@link MongoDatabaseFactory}.and the default collection name.
	 * @param mongoDbFactory The mongodb factory.
	 */
	public MongoDbMessageStore(MongoDatabaseFactory mongoDbFactory) {
		this(mongoDbFactory, null);
	}

	/**
	 * Create a MongoDbMessageStore using the provided {@link MongoDatabaseFactory} and collection name.
	 * @param mongoDbFactory The mongodb factory.
	 * @param collectionName The collection name.
	 */
	public MongoDbMessageStore(MongoDatabaseFactory mongoDbFactory, @Nullable String collectionName) {
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

	/**
	 * Add patterns for packages/classes that are allowed to be deserialized. A class can
	 * be fully qualified or a wildcard '*' is allowed at the beginning or end of the
	 * class name. Examples: {@code com.foo.*}, {@code *.MyClass}.
	 * @param patterns the patterns.
	 * @since 5.4
	 */
	public void addAllowedPatterns(String... patterns) {
		this.allowedPatterns = patterns != null ? Arrays.copyOf(patterns, patterns.length) : null;
	}

	/**
	 * Configure a set of converters to use in the {@link MappingMongoConverter}.
	 * Must be instances of {@code org.springframework.core.convert.converter.Converter},
	 * {@code org.springframework.core.convert.converter.ConverterFactory},
	 * {@code org.springframework.core.convert.converter.GenericConverter} or
	 * {@code org.springframework.data.convert.ConverterBuilder.ConverterAware}.
	 * @param customConverters the converters to use.
	 * @since 5.1.6
	 */
	public void setCustomConverters(Object... customConverters) {
		this.converter.setCustomConverters(customConverters);
	}

	@Override
	public void afterPropertiesSet() {
		if (this.applicationContext != null) {
			this.converter.setApplicationContext(this.applicationContext);
		}

		this.converter.afterPropertiesSet();

		IndexOperations indexOperations = this.template.indexOps(this.collectionName);

		indexOperations.ensureIndex(
				new Index(GROUP_ID_KEY, Sort.Direction.ASC)
						.on(GROUP_UPDATE_TIMESTAMP_KEY, Sort.Direction.DESC)
						.on(SEQUENCE, Sort.Direction.DESC));
	}

	@Override
	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		addMessageDocument(new MessageWrapper(message));
		return message;
	}

	private void addMessageDocument(MessageWrapper document) {
		UUID messageId = (UUID) document.headers.get(MessageHeaders.ID);
		Assert.notNull(messageId, "ID header must not be null");
		Query query = whereMessageIdIsAndGroupIdIs(messageId, document.get_GroupId());
		if (!this.template.exists(query, MessageWrapper.class, this.collectionName)) {
			if (document.get_Group_timestamp() == 0) {
				document.set_Group_timestamp(System.currentTimeMillis());
			}
			document.set_message_timestamp(System.currentTimeMillis());
			this.template.insert(document, this.collectionName);
		}
	}

	@Override
	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper messageWrapper =
				this.template.findOne(whereMessageIdIs(id), MessageWrapper.class, this.collectionName);
		return (messageWrapper != null) ? messageWrapper.getMessage() : null;
	}

	@Override
	public MessageMetadata getMessageMetadata(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper messageWrapper =
				this.template.findOne(whereMessageIdIs(id), MessageWrapper.class, this.collectionName);
		if (messageWrapper != null) {
			MessageMetadata messageMetadata = new MessageMetadata(id);
			messageMetadata.setTimestamp(messageWrapper.get_message_timestamp());
			return messageMetadata;
		}
		else {
			return null;
		}
	}

	@Override
	@ManagedAttribute
	public long getMessageCount() {
		return this.template.getCollection(this.collectionName).countDocuments();
	}

	@Override
	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper messageWrapper =
				this.template.findAndRemove(whereMessageIdIs(id), MessageWrapper.class, this.collectionName);
		return (messageWrapper != null ? messageWrapper.getMessage() : null);
	}

	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		Query query = whereGroupIdOrder(groupId);
		MessageWrapper messageWrapper = this.template.findOne(query, MessageWrapper.class, this.collectionName);

		if (messageWrapper != null) {
			long createdTime = messageWrapper.get_Group_timestamp();
			long lastModifiedTime = messageWrapper.get_Group_update_timestamp();
			boolean complete = messageWrapper.get_Group_complete();
			int lastReleasedSequence = messageWrapper.get_LastReleasedSequenceNumber();

			MessageGroup messageGroup = getMessageGroupFactory()
					.create(this, groupId, createdTime, complete);
			messageGroup.setLastModified(lastModifiedTime);
			messageGroup.setLastReleasedMessageSequenceNumber(lastReleasedSequence);
			messageGroup.setCondition(messageWrapper.getCondition());
			return messageGroup;

		}
		else {
			return new SimpleMessageGroup(groupId);
		}
	}

	@Override
	public void addMessagesToGroup(Object groupId, Message<?>... messages) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		Assert.notNull(messages, "'message' must not be null");
		Query query = whereGroupIdOrder(groupId);
		MessageWrapper messageDocument = this.template.findOne(query, MessageWrapper.class, this.collectionName);

		long createdTime = System.currentTimeMillis();
		int lastReleasedSequence = 0;
		boolean complete = false;
		String condition = null;
		if (messageDocument != null) {
			createdTime = messageDocument.get_Group_timestamp();
			lastReleasedSequence = messageDocument.get_LastReleasedSequenceNumber();
			complete = messageDocument.get_Group_complete();
			condition = messageDocument.getCondition();
		}

		for (Message<?> message : messages) {
			MessageWrapper wrapper = new MessageWrapper(message);
			wrapper.set_GroupId(groupId);
			wrapper.set_Group_timestamp(createdTime);
			wrapper.set_Group_update_timestamp(messageDocument == null ? createdTime : System.currentTimeMillis());
			wrapper.set_Group_complete(complete);
			wrapper.set_LastReleasedSequenceNumber(lastReleasedSequence);
			wrapper.setSequence(getNextId());
			if (condition != null) {
				wrapper.setCondition(condition);
			}

			addMessageDocument(wrapper);
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
				bulkRemove(groupId, ids);
				ids.clear();
			}
		}
		if (ids.size() > 0) {
			bulkRemove(groupId, ids);
		}
		updateGroup(groupId, lastModifiedUpdate());
	}

	private void bulkRemove(Object groupId, Collection<UUID> ids) {
		BulkOperations bulkOperations = this.template.bulkOps(BulkOperations.BulkMode.ORDERED, this.collectionName);

		for (UUID id : ids) {
			bulkOperations.remove(whereMessageIdIsAndGroupIdIs(id, groupId));
		}
		bulkOperations.execute();
	}

	@Override
	public void removeMessageGroup(Object groupId) {
		this.template.remove(whereGroupIdIs(groupId), this.collectionName);
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		List<MessageGroup> messageGroups = new ArrayList<>();

		Query query = Query.query(Criteria.where(GROUP_ID_KEY).exists(true));

		Iterable<String> groupIds = this.template.getCollection(this.collectionName)
				.distinct(GROUP_ID_KEY, query.getQueryObject(), String.class);

		for (Object groupId : groupIds) {
			messageGroups.add(getMessageGroup(groupId));
		}

		return messageGroups.iterator();
	}

	@Override
	public Message<?> pollMessageFromGroup(final Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		Query query = whereGroupIdIs(groupId).with(Sort.by(GROUP_UPDATE_TIMESTAMP_KEY, SEQUENCE));
		MessageWrapper messageWrapper = this.template.findAndRemove(query, MessageWrapper.class, this.collectionName);
		Message<?> message = null;
		if (messageWrapper != null) {
			message = messageWrapper.getMessage();
		}
		updateGroup(groupId, lastModifiedUpdate());
		return message;
	}

	@Override
	public int messageGroupSize(Object groupId) {
		long lCount = this.template.count(new Query(Criteria.where(GROUP_ID_KEY).is(groupId)), this.collectionName);
		Assert.isTrue(lCount <= Integer.MAX_VALUE, "Message count is out of Integer's range");
		return (int) lCount;
	}

	@Override
	public void setGroupCondition(Object groupId, String condition) {
		updateGroup(groupId, lastModifiedUpdate().set("_condition", condition));
	}

	@Override
	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		updateGroup(groupId, lastModifiedUpdate().set(LAST_RELEASED_SEQUENCE_NUMBER, sequenceNumber));
	}

	@Override
	public void completeGroup(Object groupId) {
		this.updateGroup(groupId, lastModifiedUpdate().set(GROUP_COMPLETE_KEY, true));
	}

	@Override
	public Message<?> getOneMessageFromGroup(Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		Query query = whereGroupIdOrder(groupId);
		MessageWrapper messageWrapper = this.template.findOne(query, MessageWrapper.class, this.collectionName);
		if (messageWrapper != null) {
			return messageWrapper.getMessage();
		}
		else {
			return null;
		}
	}

	@Override
	public Collection<Message<?>> getMessagesForGroup(Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		Query query = whereGroupIdOrder(groupId);
		List<MessageWrapper> messageWrappers = this.template.find(query, MessageWrapper.class, this.collectionName);

		return messageWrappers.stream()
				.map(MessageWrapper::getMessage)
				.collect(Collectors.toList());
	}

	@Override
	public Stream<Message<?>> streamMessagesForGroup(Object groupId) {
		Assert.notNull(groupId, GROUP_ID_MUST_NOT_BE_NULL);
		Query query = whereGroupIdOrder(groupId);
		Stream<MessageWrapper> messageWrappers =
				this.template.stream(query, MessageWrapper.class, this.collectionName)
						.stream();

		return messageWrappers.map(MessageWrapper::getMessage);
	}

	@Override
	@ManagedAttribute
	public int getMessageCountForAllMessageGroups() {
		Query query = Query.query(Criteria.where(GROUP_ID_KEY).exists(true));
		return (int) this.template.count(query, this.collectionName);
	}

	@Override
	@ManagedAttribute
	public int getMessageGroupCount() {
		Query query = Query.query(Criteria.where(GROUP_ID_KEY).exists(true));
		return this.template.findDistinct(query, GROUP_ID_KEY, this.collectionName, Object.class)
				.size();
	}

	private static Update lastModifiedUpdate() {
		return Update.update(GROUP_UPDATE_TIMESTAMP_KEY, System.currentTimeMillis());
	}

	/*
	 * Common Queries
	 */

	private static Query whereMessageIdIs(UUID id) {
		return new Query(Criteria.where("headers.id").is(id));
	}

	private static Query whereMessageIdIsAndGroupIdIs(UUID id, Object groupId) {
		return new Query(Criteria.where("headers.id").is(id).and(GROUP_ID_KEY).is(groupId));
	}

	private static Query whereGroupIdOrder(Object groupId) {
		return whereGroupIdIs(groupId).with(Sort.by(Sort.Direction.DESC, GROUP_UPDATE_TIMESTAMP_KEY, SEQUENCE));
	}

	private static Query whereGroupIdIs(Object groupId) {
		return new Query(Criteria.where(GROUP_ID_KEY).is(groupId));
	}

	private void updateGroup(Object groupId, Update update) {
		Query query = whereGroupIdIs(groupId).with(Sort.by(Sort.Direction.DESC, GROUP_UPDATE_TIMESTAMP_KEY, SEQUENCE));
		this.template.findAndModify(query, update, FindAndModifyOptions.none(), Map.class, this.collectionName);
	}

	private long getNextId() {
		Query query = Query.query(Criteria.where("_id").is(SEQUENCE_NAME));
		query.fields().include(SEQUENCE);
		return ((Number) this.template.findAndModify(query,
				new Update().inc(SEQUENCE, 1L),
				FindAndModifyOptions.options().returnNew(true).upsert(true),
				Map.class, this.collectionName)
				.get(SEQUENCE))  // NOSONAR - never returns null
				.longValue();
	}

	@SuppressWarnings(UNCHECKED)
	private static void enhanceHeaders(MessageHeaders messageHeaders, Map<String, Object> headers) {
		Map<String, Object> innerMap =
				(Map<String, Object>) new DirectFieldAccessor(messageHeaders).getPropertyValue(HEADERS);
		// using reflection to set ID and TIMESTAMP since they are immutable through MessageHeaders
		Object idHeader = headers.get(MessageHeaders.ID);
		if (idHeader != null) {
			innerMap.put(MessageHeaders.ID, idHeader);
		}
		Object tsHeader = headers.get(MessageHeaders.TIMESTAMP);
		if (tsHeader != null) {
			innerMap.put(MessageHeaders.TIMESTAMP, tsHeader);
		}
	}

	@SuppressWarnings(UNCHECKED)
	private static Map<String, Object> asMap(Bson bson) {
		if (bson instanceof Document) {
			return (Document) bson;
		}

		if (bson instanceof DBObject) {
			return ((DBObject) bson).toMap();
		}

		throw new IllegalArgumentException(
				String.format("Cannot read %s. as map. Given Bson must be a Document or DBObject!", bson.getClass()));
	}


	/**
	 * Custom implementation of the {@link MappingMongoConverter} strategy.
	 */
	private final class MessageReadingMongoConverter extends MappingMongoConverter {

		private static final String CLASS = "_class";

		private Object[] customConverters;

		MessageReadingMongoConverter(MongoDatabaseFactory mongoDbFactory,
				MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
			super(new DefaultDbRefResolver(mongoDbFactory), mappingContext);
		}

		void setCustomConverters(Object... customConverters) {
			this.customConverters =
					customConverters != null ? Arrays.copyOf(customConverters, customConverters.length) : null;
		}

		@Override
		public void afterPropertiesSet() {
			List<Object> converters = new ArrayList<>();
			converters.add(new MessageHistoryToDocumentConverter());
			converters.add(new DocumentToGenericMessageConverter());
			converters.add(new DocumentToMutableMessageConverter());
			DocumentToErrorMessageConverter docToErrorMessageConverter = new DocumentToErrorMessageConverter();
			if (MongoDbMessageStore.this.allowedPatterns != null) {
				docToErrorMessageConverter.deserializingConverter
						.addAllowedPatterns(MongoDbMessageStore.this.allowedPatterns);
			}
			converters.add(docToErrorMessageConverter);
			converters.add(new DocumentToAdviceMessageConverter());
			converters.add(new ThrowableToBytesConverter());

			if (this.customConverters != null) {
				Collections.addAll(converters, this.customConverters);
			}

			setCustomConversions(new MongoCustomConversions(converters));
			super.afterPropertiesSet();
		}

		@Override
		public void write(Object source, Bson target) {
			Assert.isInstanceOf(MessageWrapper.class, source);

			asMap(target).put(CREATED_DATE, System.currentTimeMillis());

			super.write(source, target);
		}

		@Override
		@SuppressWarnings({ UNCHECKED })
		public <S> S read(Class<S> clazz, Bson source) {
			if (!MessageWrapper.class.equals(clazz)) {
				return super.read(clazz, source);
			}
			if (source != null) {
				Map<String, Object> sourceMap = asMap(source);
				Message<?> message = null;
				Object messageType = sourceMap.get("_messageType");
				if (messageType == null) {
					messageType = GenericMessage.class.getName();
				}
				try {
					message = (Message<?>) read(ClassUtils.forName(messageType.toString(),
							MongoDbMessageStore.this.classLoader), source);
				}
				catch (ClassNotFoundException e) {
					throw new IllegalStateException("failed to load class: " + messageType, e);
				}

				Long groupTimestamp = (Long) sourceMap.get(GROUP_TIMESTAMP_KEY);
				Long lastModified = (Long) sourceMap.get(GROUP_UPDATE_TIMESTAMP_KEY);
				Integer lastReleasedSequenceNumber = (Integer) sourceMap.get(LAST_RELEASED_SEQUENCE_NUMBER);
				Boolean completeGroup = (Boolean) sourceMap.get(GROUP_COMPLETE_KEY);

				MessageWrapper wrapper = new MessageWrapper(message);

				if (sourceMap.containsKey(GROUP_ID_KEY)) {
					wrapper.set_GroupId(sourceMap.get(GROUP_ID_KEY));
				}
				if (groupTimestamp != null) {
					wrapper.set_Group_timestamp(groupTimestamp);
				}
				if (lastModified != null) {
					wrapper.set_Group_update_timestamp(lastModified);
				}
				if (lastReleasedSequenceNumber != null) {
					wrapper.set_LastReleasedSequenceNumber(lastReleasedSequenceNumber);
				}

				if (completeGroup != null) {
					wrapper.set_Group_complete(completeGroup);
				}
				wrapper.setCondition((String) sourceMap.get("_condition"));

				return (S) wrapper;
			}
			return null;
		}

		private Map<String, Object> normalizeHeaders(Map<String, Object> headers) {
			Map<String, Object> normalizedHeaders = new HashMap<>();
			for (Entry<String, Object> entry : headers.entrySet()) {
				String headerName = entry.getKey();
				Object headerValue = entry.getValue();
				if (headerValue instanceof Bson) {
					Bson source = (Bson) headerValue;
					Map<String, Object> document = asMap(source);
					try {
						Class<?> typeClass = null;
						if (document.containsKey(CLASS)) {
							Object type = document.get(CLASS);
							typeClass = ClassUtils.forName(type.toString(), MongoDbMessageStore.this.classLoader);
						}
						else if (source instanceof BasicDBList) {
							typeClass = List.class;
						}
						else {
							throw new IllegalStateException("Unsupported 'Bson' type: " + source.getClass());
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

		private Object extractPayload(Bson source) {
			Object payload = asMap(source).get("payload");

			if (payload instanceof Bson) {
				Bson payloadObject = (Bson) payload;
				Object payloadType = asMap(payloadObject).get(CLASS);
				try {
					Class<?> payloadClass =
							ClassUtils.forName(payloadType.toString(), MongoDbMessageStore.this.classLoader);
					payload = read(payloadClass, payloadObject);
				}
				catch (Exception e) {
					throw new IllegalStateException("failed to load class: " + payloadType, e);
				}
			}
			return payload;
		}

	}


	@WritingConverter
	private static class MessageHistoryToDocumentConverter implements Converter<MessageHistory, Document> {

		MessageHistoryToDocumentConverter() {
		}

		@Override
		public Document convert(MessageHistory source) {
			BasicDBList dbList = new BasicDBList();
			for (Properties properties : source) {
				Document historyProperty = new Document()
						.append(MessageHistory.NAME_PROPERTY, properties.getProperty(MessageHistory.NAME_PROPERTY))
						.append(MessageHistory.TYPE_PROPERTY, properties.getProperty(MessageHistory.TYPE_PROPERTY))
						.append(MessageHistory.TIMESTAMP_PROPERTY,
								properties.getProperty(MessageHistory.TIMESTAMP_PROPERTY));
				dbList.add(historyProperty);
			}
			return new Document("components", dbList)
					.append("_class", MessageHistory.class.getName());
		}

	}

	@ReadingConverter
	private class DocumentToGenericMessageConverter implements Converter<Document, GenericMessage<?>> {

		DocumentToGenericMessageConverter() {
		}

		@Override
		public GenericMessage<?> convert(Document source) {
			@SuppressWarnings(UNCHECKED)
			Map<String, Object> headers =
					MongoDbMessageStore.this.converter.normalizeHeaders((Map<String, Object>) source.get(HEADERS));

			GenericMessage<?> message =
					new GenericMessage<>(MongoDbMessageStore.this.converter.extractPayload(source), headers);
			enhanceHeaders(message.getHeaders(), headers);
			return message;
		}

	}

	@ReadingConverter
	private final class DocumentToMutableMessageConverter implements Converter<Document, MutableMessage<?>> {

		DocumentToMutableMessageConverter() {
		}

		@Override
		public MutableMessage<?> convert(Document source) {
			@SuppressWarnings(UNCHECKED)
			Map<String, Object> headers =
					MongoDbMessageStore.this.converter.normalizeHeaders((Map<String, Object>) source.get(HEADERS));

			Object payload = MongoDbMessageStore.this.converter.extractPayload(source);
			return (MutableMessage<?>) MutableMessageBuilder.withPayload(payload)
					.copyHeaders(headers)
					.build();
		}

	}

	@ReadingConverter
	private class DocumentToAdviceMessageConverter implements Converter<Document, AdviceMessage<?>> {

		DocumentToAdviceMessageConverter() {
		}

		@Override
		public AdviceMessage<?> convert(Document source) {
			@SuppressWarnings(UNCHECKED)
			Map<String, Object> headers =
					MongoDbMessageStore.this.converter.normalizeHeaders((Map<String, Object>) source.get(HEADERS));

			Message<?> inputMessage = null;

			if (source.get("inputMessage") != null) {
				Bson inputMessageObject = (Bson) source.get("inputMessage");
				Object inputMessageType = asMap(inputMessageObject).get("_class");
				try {
					Class<?> messageClass = ClassUtils.forName(inputMessageType.toString(),
							MongoDbMessageStore.this.classLoader);
					inputMessage = (Message<?>) MongoDbMessageStore.this.converter.read(messageClass,
							inputMessageObject);
				}
				catch (Exception e) {
					throw new IllegalStateException("failed to load class: " + inputMessageType, e);
				}
			}

			AdviceMessage<?> message = new AdviceMessage<>(
					MongoDbMessageStore.this.converter.extractPayload(source), headers, inputMessage);
			enhanceHeaders(message.getHeaders(), headers);

			return message;
		}

	}

	@ReadingConverter
	private class DocumentToErrorMessageConverter implements Converter<Document, ErrorMessage> {

		private final AllowListDeserializingConverter deserializingConverter = new AllowListDeserializingConverter();

		DocumentToErrorMessageConverter() {
		}

		@Override
		public ErrorMessage convert(Document source) {
			@SuppressWarnings(UNCHECKED)
			Map<String, Object> headers =
					MongoDbMessageStore.this.converter.normalizeHeaders((Map<String, Object>) source.get(HEADERS));

			Object payload = this.deserializingConverter.convert(((Binary) source.get("payload")).getData());
			ErrorMessage message = new ErrorMessage((Throwable) payload, headers); // NOSONAR not null
			enhanceHeaders(message.getHeaders(), headers);

			return message;
		}

	}

	@WritingConverter
	private static class ThrowableToBytesConverter implements Converter<Throwable, byte[]> {

		private final Converter<Object, byte[]> serializingConverter = new SerializingConverter();

		ThrowableToBytesConverter() {
		}

		@Override
		public byte[] convert(Throwable source) {
			return this.serializingConverter.convert(source);
		}

	}


	/**
	 * Wrapper class used for storing Messages in MongoDB along with their "group" metadata.
	 */
	private static final class MessageWrapper {

		private static final String UNUSED = "unused";

		/*
		 * Needed as a persistence property to suppress 'Cannot determine IsNewStrategy' MappingException
		 * when the application context is configured with auditing. The document is not
		 * currently Auditable.
		 */
		@SuppressWarnings(UNUSED)
		@Id
		private String _id; // NOSONAR name

		private volatile Object _groupId; // NOSONAR name

		@Transient
		private final Message<?> message; // NOSONAR name

		@SuppressWarnings(UNUSED)
		private final String _messageType; // NOSONAR name

		@SuppressWarnings(UNUSED)
		private final Object payload;

		@SuppressWarnings(UNUSED)
		private final Map<String, ?> headers;

		@SuppressWarnings(UNUSED)
		private final Message<?> inputMessage;

		private long _message_timestamp; // NOSONAR name

		private volatile long _group_timestamp; // NOSONAR name

		private volatile long _group_update_timestamp; // NOSONAR name

		private volatile int _last_released_sequence; // NOSONAR name

		private volatile boolean _group_complete; // NOSONAR name

		private volatile String _condition; // NOSONAR name

		@SuppressWarnings(UNUSED)
		private long sequence;

		MessageWrapper(Message<?> message) {
			Assert.notNull(message, "'message' must not be null");
			this.message = message;
			this._messageType = message.getClass().getName();
			this.payload = message.getPayload();
			this.headers = message.getHeaders();
			if (message instanceof AdviceMessage) {
				this.inputMessage = ((AdviceMessage<?>) message).getInputMessage();
			}
			else {
				this.inputMessage = null;
			}
		}

		public int get_LastReleasedSequenceNumber() { // NOSONAR name
			return this._last_released_sequence;
		}

		public long get_Group_timestamp() { // NOSONAR name
			return this._group_timestamp;
		}

		public boolean get_Group_complete() { // NOSONAR name
			return this._group_complete;
		}

		@SuppressWarnings(UNUSED)
		public Object get_GroupId() { // NOSONAR name
			return this._groupId;
		}

		public Message<?> getMessage() {
			return this.message;
		}

		public void set_GroupId(Object groupId) { // NOSONAR name
			this._groupId = groupId;
		}

		public void set_Group_timestamp(long groupTimestamp) { // NOSONAR name
			this._group_timestamp = groupTimestamp;
		}

		public long get_message_timestamp() { // NOSONAR name
			return this._message_timestamp;
		}

		public void set_message_timestamp(long _message_timestamp) { // NOSONAR name
			this._message_timestamp = _message_timestamp;
		}

		public long get_Group_update_timestamp() { // NOSONAR name
			return this._group_update_timestamp;
		}

		public void set_Group_update_timestamp(long lastModified) { // NOSONAR name
			this._group_update_timestamp = lastModified;
		}

		public void set_LastReleasedSequenceNumber(int lastReleasedSequenceNumber) { // NOSONAR name
			this._last_released_sequence = lastReleasedSequenceNumber;
		}

		public void set_Group_complete(boolean completedGroup) { // NOSONAR name
			this._group_complete = completedGroup;
		}

		public String getCondition() {
			return this._condition;
		}

		public void setCondition(String condition) {
			this._condition = condition;
		}

		public void setSequence(long sequence) {
			this.sequence = sequence;
		}

	}

}
