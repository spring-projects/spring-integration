/*
 * Copyright 2002-2011 the original author or authors.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.query.Order;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.mongodb.DBObject;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * An implementation of both the {@link MessageStore} and {@link MessageGroupStore}
 * strategies that relies upon MongoDB for persistence.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class MongoDbMessageStore extends AbstractMessageGroupStore implements MessageStore, BeanClassLoaderAware {

	private final static String DEFAULT_COLLECTION_NAME = "messages";

	private final static String GROUP_ID_KEY = "_groupId";

	private final static String GROUP_COMPLETE_KEY = "_group_complete";
	
	private final static String LAST_RELEASED_SEQUENCE_NUMBER = "_last_released_sequence";
	
	private final static String GROUP_TIMESTAMP_KEY = "_group_timestamp";
	
	private final static String GROUP_UPDATE_TIMESTAMP_KEY = "_group_update_timestamp";

	private final static String PAYLOAD_TYPE_KEY = "_payloadType";
	
	private final static String CREATED_DATE = "_createdDate";


	private final MongoTemplate template;

	private final String collectionName;

	private volatile ClassLoader classLoader = ClassUtils.getDefaultClassLoader();


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
		MessageReadingMongoConverter converter = new MessageReadingMongoConverter(mongoDbFactory, new MongoMappingContext());
		converter.afterPropertiesSet();
		this.template = new MongoTemplate(mongoDbFactory, converter);
		this.collectionName = (StringUtils.hasText(collectionName)) ? collectionName : DEFAULT_COLLECTION_NAME;
	}


	public void setBeanClassLoader(ClassLoader classLoader) {
		Assert.notNull(classLoader, "classLoader must not be null");
		this.classLoader = classLoader;
	}

	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		this.template.insert(new MessageWrapper(message), this.collectionName);
		return message;
	}

	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper messageWrapper = this.template.findOne(whereMessageIdIs(id), MessageWrapper.class, this.collectionName);
		return (messageWrapper != null) ? messageWrapper.getMessage() : null;
	}

	@ManagedAttribute
	public long getMessageCount() {
		return this.template.getCollection(this.collectionName).getCount();
	}

	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper messageWrapper =  this.template.findAndRemove(whereMessageIdIs(id), MessageWrapper.class, this.collectionName);
		return (messageWrapper != null) ? messageWrapper.getMessage() : null;
	}

	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		List<MessageWrapper> messageWrappers = this.template.find(whereGroupIdIs(groupId), MessageWrapper.class, this.collectionName);
		List<Message<?>> messages = new ArrayList<Message<?>>();
		long timestamp = 0;
		long lastmodified = 0;
		int lastReleasedSequenceNumber = 0;
		boolean completeGroup = false;
		if (messageWrappers.size() > 0){
			MessageWrapper messageWrapper = messageWrappers.get(0);
			timestamp = messageWrapper.getGroupTimestamp();
			lastmodified = messageWrapper.getGroupUpdateTimestamp();
			completeGroup = messageWrapper.isCompletedGroup();
			lastReleasedSequenceNumber = messageWrapper.getLastReleasedSequenceNumber();
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
		long messageGroupUpdateTimestamp = messageGroup.getLastModified();
		
		if (messageGroupTimestamp == 0){
			messageGroupTimestamp = System.currentTimeMillis();
			messageGroupUpdateTimestamp = messageGroupTimestamp;
		}
		else {
			messageGroupUpdateTimestamp = System.currentTimeMillis();
		}
		
		MessageWrapper wrapper = new MessageWrapper(message);
		wrapper.setGroupId(groupId);
		wrapper.setGroupTimestamp(messageGroupTimestamp);
		wrapper.setGroupUpdateTimestamp(messageGroupUpdateTimestamp);
		wrapper.setCompletedGroup(messageGroup.isComplete());
		wrapper.setLastReleasedSequenceNumber(messageGroup.getLastReleasedMessageSequenceNumber());
		
		this.template.insert(wrapper, this.collectionName);
		return this.getMessageGroup(groupId);
	}

	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");
		this.removeMessage(messageToRemove.getHeaders().getId());
		return this.getMessageGroup(groupId);
	}

	public void removeMessageGroup(Object groupId) {
		List<MessageWrapper> messageWrappers = this.template.find(whereGroupIdIs(groupId), MessageWrapper.class, this.collectionName);
		for (MessageWrapper messageWrapper : messageWrappers) {
			this.removeMessageFromGroup(groupId, messageWrapper.getMessage());
		}
	}

	public Iterator<MessageGroup> iterator() {
		List<MessageWrapper> groupedMessages = this.template.find(whereGroupIdExists(), MessageWrapper.class, this.collectionName);
		Map<Object, MessageGroup> messageGroups = new HashMap<Object, MessageGroup>();
		for (MessageWrapper groupedMessage : groupedMessages) {
			Object groupId = groupedMessage.getGroupId();
			if (!messageGroups.containsKey(groupId)) {
				messageGroups.put(groupId, this.getMessageGroup(groupId));
			}
		}
		return messageGroups.values().iterator();
	}
	
	public void completeGroup(Object groupId) {
		Update update = Update.update(GROUP_COMPLETE_KEY, true);
		Query q = whereGroupIdIs(groupId);
		this.template.updateFirst(q, update, this.collectionName);
	}

	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		Update update = Update.update(LAST_RELEASED_SEQUENCE_NUMBER, sequenceNumber);
		Query q = whereGroupIdIs(groupId);
		this.template.updateFirst(q, update, this.collectionName);
	}
	
	public Message<?> pollMessageFromGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		List<MessageWrapper> messageWrappers = this.template.find(whereGroupIdIsOrdered(groupId), MessageWrapper.class, this.collectionName);
		Message<?> message = null;
		
		if (!CollectionUtils.isEmpty(messageWrappers)){
			message = messageWrappers.get(0).getMessage();
			this.removeMessageFromGroup(groupId, message);
		}
		
		return message;
	}

	/*
	 * Common Queries
	 */

	private static Query whereMessageIdIs(UUID id) {
		return new Query(where("headers.id").is(id.toString()));
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


	/**
	 * Custom implementation of the {@link MappingMongoConverter} strategy.
	 */
	private class MessageReadingMongoConverter extends MappingMongoConverter {

		public MessageReadingMongoConverter(MongoDbFactory mongoDbFactory,
				MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
			super(mongoDbFactory, mappingContext);
		}

		@Override
		public void afterPropertiesSet() {
			List<Converter<?, ?>> customConverters = new ArrayList<Converter<?,?>>();
			customConverters.add(new UuidToStringConverter());
			customConverters.add(new StringToUuidConverter());
			this.setCustomConversions(new CustomConversions(customConverters));
			super.afterPropertiesSet();
		}

		@Override
		public void write(Object source, DBObject target) {
			Message<?> message = null;
			Object groupId = null;

			boolean groupComplete = false;
			long groupTimestamp = 0;
			long groupUpdateTimestamp = 0;
			int lastReleasedSequenceNumber = 0;
			if (source instanceof MessageWrapper) {
				MessageWrapper wrapper = (MessageWrapper) source;
				message = wrapper.getMessage();
				groupId = wrapper.getGroupId();
				groupComplete = wrapper.isCompletedGroup();
				lastReleasedSequenceNumber = wrapper.getLastReleasedSequenceNumber();
				groupTimestamp = wrapper.getGroupTimestamp();
				groupUpdateTimestamp = wrapper.getGroupUpdateTimestamp();
			}
			else {
				Class<?> sourceType = (source != null) ? source.getClass() : null;
				throw new IllegalArgumentException("Unexpected source type [" + sourceType + "]. Should be a MessageWrapper.");
			}
			target.put(CREATED_DATE, System.currentTimeMillis());
			target.put(PAYLOAD_TYPE_KEY, message.getPayload().getClass().getName());
			if (groupId != null) {
				target.put(GROUP_ID_KEY, groupId);
				target.put(GROUP_COMPLETE_KEY, groupComplete);
				target.put(LAST_RELEASED_SEQUENCE_NUMBER, lastReleasedSequenceNumber);
				target.put(GROUP_TIMESTAMP_KEY, groupTimestamp);
				target.put(GROUP_UPDATE_TIMESTAMP_KEY, groupUpdateTimestamp);
			}
			
			super.write(message, target);
		}

		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public <S> S read(Class<S> clazz, DBObject source) {
			if (!MessageWrapper.class.equals(clazz)) {
				return super.read(clazz, source);
			}
			if (source != null) {
				Map<String, Object> headers = (Map<String, Object>) source.get("headers");
				Object payload = source.get("payload");
				Object payloadType = source.get(PAYLOAD_TYPE_KEY);
				if (payloadType != null && payload instanceof DBObject) {
					try {
						Class<?> payloadClass = ClassUtils.forName(payloadType.toString(), classLoader);
						payload = this.read(payloadClass, (DBObject) payload);
					}
					catch (Exception e) {
						throw new IllegalStateException("failed to load class: " + payloadType, e);
					}
				}
				GenericMessage message = new GenericMessage(payload, headers);
				Map innerMap = (Map) new DirectFieldAccessor(message.getHeaders()).getPropertyValue("headers");
				// using reflection to set ID and TIMESTAMP since they are immutable through MessageHeaders
				innerMap.put(MessageHeaders.ID, UUID.fromString((String) headers.get(MessageHeaders.ID)));
				innerMap.put(MessageHeaders.TIMESTAMP, headers.get(MessageHeaders.TIMESTAMP));
				Long groupTimestamp = (Long)source.get(GROUP_TIMESTAMP_KEY);
				Long groupUpdateTimestamp = (Long)source.get(GROUP_UPDATE_TIMESTAMP_KEY);
				Integer lastReleasedSequenceNumber = (Integer)source.get(LAST_RELEASED_SEQUENCE_NUMBER);
				Boolean completeGroup = (Boolean)source.get(GROUP_COMPLETE_KEY);
				
				MessageWrapper wrapper = new MessageWrapper(message);
				
				if (source.containsField(GROUP_ID_KEY)){
					wrapper.setGroupId(source.get(GROUP_ID_KEY));
				}
				if (groupTimestamp != null){
					wrapper.setGroupTimestamp(groupTimestamp);
				}
				if (groupUpdateTimestamp != null){
					wrapper.setGroupUpdateTimestamp(groupUpdateTimestamp);
				}
				if (lastReleasedSequenceNumber != null){
					wrapper.setLastReleasedSequenceNumber(lastReleasedSequenceNumber);
				}
					
				if (completeGroup != null){
					wrapper.setCompletedGroup(completeGroup.booleanValue());
				}
										
				return (S) wrapper;
			}
			return null;
		}
	}


	private static class UuidToStringConverter implements Converter<UUID, String> {
		public String convert(UUID source) {
			return source.toString();
		}
	}


	private static class StringToUuidConverter implements Converter<String, UUID> {
		public UUID convert(String source) {
			return UUID.fromString(source);
		}
	}


	/**
	 * Wrapper class used for storing Messages in MongoDB along with their "group" metadata.
	 */
	private static final class MessageWrapper {

		private volatile Object groupId;

		private final Message<?> message;
		
		private volatile long groupTimestamp; 
		
		private volatile long groupUpdateTimestamp; 

		private volatile int lastReleasedSequenceNumber; 

		private volatile boolean completedGroup;

		public MessageWrapper(Message<?> message) {
			this.message = message;
		}
		
		public int getLastReleasedSequenceNumber() {
			return lastReleasedSequenceNumber;
		}
		
		public long getGroupTimestamp() {
			return groupTimestamp;
		}

		public boolean isCompletedGroup() {
			return completedGroup;
		}

		public Object getGroupId() {
			return groupId;
		}

		public Message<?> getMessage() {
			return message;
		}
		
		public void setGroupId(Object groupId) {
			this.groupId = groupId;
		}

		public void setGroupTimestamp(long groupTimestamp) {
			this.groupTimestamp = groupTimestamp;
		}
		
		public long getGroupUpdateTimestamp() {
			return groupUpdateTimestamp;
		}

		public void setGroupUpdateTimestamp(long groupUpdateTimestamp) {
			this.groupUpdateTimestamp = groupUpdateTimestamp;
		}

		public void setLastReleasedSequenceNumber(int lastReleasedSequenceNumber) {
			this.lastReleasedSequenceNumber = lastReleasedSequenceNumber;
		}

		public void setCompletedGroup(boolean completedGroup) {
			this.completedGroup = completedGroup;
		}
	}
}
