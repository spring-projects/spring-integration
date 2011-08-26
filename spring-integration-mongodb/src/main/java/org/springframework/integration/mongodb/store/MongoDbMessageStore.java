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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.mongodb.DBObject;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class MongoDbMessageStore extends AbstractMessageGroupStore implements MessageStore, BeanClassLoaderAware {

	private final static String DEFAULT_COLLECTION_NAME = "messages";
	
	private final static String MESSAGE_GROUP_HEADER = "_groupId";
	
	private final static String MESSAGE_GROUP_MARKED = "_marked";
	
	private final static String PAYLOAD_TYPE = "_payloadType";
	
	private final static String MESSAGE_ID = "_id";
	
	private final MongoTemplate template;

	private final String collectionName;

	private volatile ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

	public MongoDbMessageStore(MongoDbFactory mongoDbFactory) {
		this(mongoDbFactory, null);
	}

	public MongoDbMessageStore(MongoDbFactory mongoDbFactory, String collectionName) {
		Assert.notNull(mongoDbFactory, "mongoDbFactory must not be null");
		MessageReadingMongoConverter converter = new MessageReadingMongoConverter(mongoDbFactory, new MongoMappingContext());
		this.template = new MongoTemplate(mongoDbFactory, converter);
		this.collectionName = (StringUtils.hasText(collectionName)) ? collectionName : DEFAULT_COLLECTION_NAME;
	}


	public void setBeanClassLoader(ClassLoader classLoader) {
		Assert.notNull(classLoader, "classLoader must not be null");
		this.classLoader = classLoader;
	}

	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		this.template.insert(new MessageWrapper(message, null, false), this.collectionName);
		return message;
	}

	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper messageWrapper = this.template.findOne(this.queryByMessageId(id), MessageWrapper.class, this.collectionName);
		if (messageWrapper != null){
			return messageWrapper.getMessage();
		}

		return null;
	}

	@ManagedAttribute
	public long getMessageCount() {
		return this.template.getCollection(this.collectionName).getCount();
	}

	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper messageWrapper =  this.template.findAndRemove(queryByMessageId(id), MessageWrapper.class, this.collectionName);
		if (messageWrapper != null){
			return messageWrapper.getMessage();
		}
		return null;
	}
	
	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
	
		List<MessageWrapper> messageWrappers = this.template.find(this.queryByGroupId(groupId), MessageWrapper.class, this.collectionName);
		List<Message<?>> unmarkedMessages = new ArrayList<Message<?>>();
		List<Message<?>> markedMessages = new ArrayList<Message<?>>();
		
		for (MessageWrapper messageWrapper : messageWrappers) {
			if (messageWrapper.isMarked()){
				markedMessages.add(messageWrapper.getMessage());
			}
			else {
				unmarkedMessages.add(messageWrapper.getMessage());
			}
		}
		SimpleMessageGroup messageGroup = new SimpleMessageGroup(unmarkedMessages, markedMessages, groupId, System.currentTimeMillis());
		
		return messageGroup;
	}

	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");

		synchronized (groupId) {
			MessageWrapper wrapper = new MessageWrapper(message, groupId, false);
			this.template.insert(wrapper, this.collectionName);
			return this.getMessageGroup(groupId);
		}
	}

	public MessageGroup markMessageGroup(MessageGroup group) {
		Assert.notNull(group, "'group' must not be null");
		Object groupId = group.getGroupId();
		
		synchronized (groupId) {
			List<MessageWrapper> messageWrappers = this.template.find(this.queryByGroupId(groupId), MessageWrapper.class, this.collectionName);
			for (MessageWrapper messageWrapper : messageWrappers) {
				this.markMessageFromGroup(groupId, messageWrapper.getMessage());
			}
			return this.getMessageGroup(groupId);
		}	
	}

	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");
	
		synchronized (groupId) {
			this.removeMessage(messageToRemove.getHeaders().getId());
			return this.getMessageGroup(groupId);
		}
	}

	public MessageGroup markMessageFromGroup(Object groupId, Message<?> messageToMark) {
		Update update = Update.update(MESSAGE_GROUP_MARKED, true);
		Query q = this.queryByMessageId(messageToMark.getHeaders().getId());
		
		synchronized (groupId) {	
			this.template.updateFirst(q, update, this.collectionName);
			return this.getMessageGroup(groupId);
		}	    
	}

	public void removeMessageGroup(Object groupId) {
		synchronized (groupId) {
			List<MessageWrapper> messageWrappers = this.template.find(this.queryByGroupId(groupId), MessageWrapper.class, this.collectionName);
			for (MessageWrapper messageWrapper : messageWrappers) {
				this.removeMessageFromGroup(groupId, messageWrapper.getMessage());
			}
		}	
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		List<MessageWrapper> groupedMessages = this.template.find(this.queryOfAllMessageGroups(), MessageWrapper.class, this.collectionName);
		Map<Object, MessageGroup> messageGroups = new HashMap<Object, MessageGroup>();
		for (MessageWrapper groupedMessage : groupedMessages) {
			Object groupId = groupedMessage.getGroupId();
			if (!messageGroups.containsKey(groupId)){
				messageGroups.put(groupId, this.getMessageGroup(groupId));
			}
		}
		return messageGroups.values().iterator();
	}

	private Query queryByMessageId(UUID id) {
		return new Query(where("_id").is(id.toString()));
	}
	
	private Query queryOfAllMessageGroups() {
		return new Query(where(MESSAGE_GROUP_HEADER).exists(true));
	}
	private Query queryByGroupId(Object groupId) {
		return new Query(where(MESSAGE_GROUP_HEADER).is(groupId));
	}
	
	/**
	 */
	private class MessageReadingMongoConverter extends MappingMongoConverter {

		public MessageReadingMongoConverter(MongoDbFactory mongoDbFactory,
				MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
			super(mongoDbFactory, mappingContext);
		}


		@Override
		public void afterPropertiesSet() {
			super.afterPropertiesSet();
			List<Converter<?, ?>> customConverters = new ArrayList<Converter<?,?>>();
			customConverters.add(new UuidToStringConverter());
			customConverters.add(new StringToUuidConverter());
			this.setCustomConversions(new CustomConversions(customConverters));
		}
	
		@Override
		public void write(Object source, DBObject target) {
			Message<?> message = null;
			Object groupId = null;
			boolean marked = false;
			if (source instanceof MessageWrapper){
				MessageWrapper wrapper = (MessageWrapper) source;
				message = wrapper.getMessage();
				groupId = wrapper.getGroupId();
				marked = wrapper.isMarked();
			}
			else {
				throw new IllegalArgumentException("Unrecognized source. Should either be MessageWrapper");
			}
			String payloadType = message.getPayload().getClass().getName();
			target.put(PAYLOAD_TYPE, payloadType);
			target.put(MESSAGE_ID, message.getHeaders().getId().toString());
			if (groupId != null){
				target.put(MESSAGE_GROUP_HEADER, groupId);
			}
			if (marked){
				target.put(MESSAGE_GROUP_MARKED, marked);
			}
			super.write(message, target);
		}


		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public <S> S read(Class<S> clazz, DBObject source) {
			if (!MessageWrapper.class.equals(clazz)) {
				return super.read(clazz, source);
			}
			if (source != null){
				Map<String, Object> headers = (Map<String, Object>) source.get("headers");
				Object payload = source.get("payload");
				Object payloadType = source.get(PAYLOAD_TYPE);
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
				// TODO: unpick this mess
				innerMap.put(MessageHeaders.ID, UUID.fromString(source.get(MESSAGE_ID).toString()));
				innerMap.put(MessageHeaders.TIMESTAMP, headers.get(MessageHeaders.TIMESTAMP));
				
				MessageWrapper wrapper = new MessageWrapper(message, source.get(MESSAGE_GROUP_HEADER), source.get(MESSAGE_GROUP_MARKED) != null);
				
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

	private class MessageWrapper {
		
		private final Object groupId;

		private final boolean marked;
		
		private final Message<?> message;
		
		public MessageWrapper(Message<?> message, Object groupId, boolean marked){
			this.marked = marked;
			this.message = message;
			this.groupId = groupId;
		}
		public Object getGroupId() {
			return groupId;
		}

		public boolean isMarked() {
			return marked;
		}

		public Message<?> getMessage() {
			return message;
		}
	}
}
