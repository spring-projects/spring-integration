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
import org.springframework.beans.factory.InitializingBean;
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
import com.mongodb.WriteConcern;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class MongoDbMessageStore extends AbstractMessageGroupStore implements MessageStore, BeanClassLoaderAware, InitializingBean {

	private final static String DEFAULT_COLLECTION_NAME = "messages";
	
	private final static String MESSAGE_GROUP_HEADER = "message_group";
	
	private final static String MESSAGE_GROUP_MARKED = "message_group_marked";
	
	private final MongoTemplate template;

	private final String collectionName;

	private volatile ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
	
	private final Map<Object, MessageGroup> messageGroups = new HashMap<Object, MessageGroup>();
	
	private final Object lock = new Object();


	public MongoDbMessageStore(MongoDbFactory mongoDbFactory) {
		this(mongoDbFactory, null);
	}

	public MongoDbMessageStore(MongoDbFactory mongoDbFactory, String collectionName) {
		Assert.notNull(mongoDbFactory, "mongoDbFactory must not be null");
		MessageReadingMongoConverter converter = new MessageReadingMongoConverter(mongoDbFactory, new MongoMappingContext());
		this.template = new MongoTemplate(mongoDbFactory, converter);
		this.collectionName = (StringUtils.hasText(collectionName)) ? collectionName : DEFAULT_COLLECTION_NAME;
		this.template.setWriteConcern(new WriteConcern(true));
		//this.template.createCollection(collectionName);

	}


	public void setBeanClassLoader(ClassLoader classLoader) {
		Assert.notNull(classLoader, "classLoader must not be null");
		this.classLoader = classLoader;
	}

	public <T> Message<T> addMessage(Message<T> message) {
		Assert.notNull(message, "'message' must not be null");
		this.template.insert(message, collectionName);
		return message;
	}

	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		Message<?> message = this.template.findOne(this.idQuery(id), Message.class, this.collectionName);
		if (message != null){
			this.normalizeMessage(message);
		}
		
		return message;
	}

	@ManagedAttribute
	public long getMessageCount() {
		return this.template.getCollection(DEFAULT_COLLECTION_NAME).getCount();
	}

	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		return this.template.findAndRemove(idQuery(id), Message.class, this.collectionName);
	}
	
	public MessageGroup getMessageGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		MessageGroup messageGroup = null;
		
		synchronized (lock) {
			if (this.messageGroups.containsKey(groupId)){
				messageGroup = this.messageGroups.get(groupId);
			}
			else {
				messageGroup = new SimpleMessageGroup(groupId);
				this.messageGroups.put(groupId, messageGroup);
			}
		}
		
		return messageGroup;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(message, "'message' must not be null");
		SimpleMessageGroup messageGroup = this.getValidMessageGroupInstance(this.getMessageGroup(groupId));
		synchronized (lock) {
			Map innerMap = (Map) new DirectFieldAccessor(message.getHeaders()).getPropertyValue("headers");	
			innerMap.put(MESSAGE_GROUP_HEADER, groupId);
			this.addMessage(message);
			messageGroup.add(message);
		}
	
		return messageGroup;
	}

	public MessageGroup markMessageGroup(MessageGroup group) {
		Assert.notNull(group, "'group' must not be null");
		SimpleMessageGroup messageGroup = this.getValidMessageGroupInstance(group);
		
		synchronized (lock) {
			for (Message<?> messageToMark : messageGroup.getUnmarked()) {
				this.markMessageFromGroup(messageGroup.getGroupId(), messageToMark);
			}
			messageGroup.markAll();
		}
		return messageGroup;
	}

	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");
		SimpleMessageGroup messageGroup = this.getValidMessageGroupInstance(this.getMessageGroup(groupId));
		synchronized (lock) {
			this.removeMessage(messageToRemove.getHeaders().getId());
			messageGroup.remove(messageToRemove);
		}
		return messageGroup;
	}

	public MessageGroup markMessageFromGroup(Object groupId, Message<?> messageToMark) {
		SimpleMessageGroup messageGroup = (SimpleMessageGroup) this.getMessageGroup(groupId);
		synchronized (lock) {
			Update update = Update.update("headers." + MESSAGE_GROUP_MARKED, true);
			Query q = this.idQuery(messageToMark.getHeaders().getId());
			this.template.updateFirst(q, update, DEFAULT_COLLECTION_NAME);
			messageGroup.mark(messageToMark);
		}	
		return messageGroup;
	}

	@SuppressWarnings("rawtypes")
	public void removeMessageGroup(Object groupId) {
		synchronized (lock) {
			List<Message> groupedMessages = this.template.find(this.messagesForGroupQuery(groupId.toString()), Message.class, DEFAULT_COLLECTION_NAME);
			for (Message<?> groupedMessage : groupedMessages) {
				this.removeMessageFromGroup(groupId, groupedMessage);
			}
			this.messageGroups.remove(groupId);
		}	
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		return this.messageGroups.values().iterator();
	}

	@SuppressWarnings("rawtypes")
	public void afterPropertiesSet() throws Exception {
		List<Message> groupedMessages = this.template.find(this.groupedMessagesQuery(), Message.class, DEFAULT_COLLECTION_NAME);
		for (Message groupedMessage : groupedMessages) {
			Object groupId = groupedMessage.getHeaders().get(MESSAGE_GROUP_HEADER);
			SimpleMessageGroup messageGroup = null;
			if (this.messageGroups.containsKey(groupId)){
				messageGroup = (SimpleMessageGroup) this.messageGroups.get(groupId);
			}
			else {
				messageGroup = new SimpleMessageGroup(groupId);
				this.messageGroups.put(groupId, messageGroup);
			}
			Map innerMap = (Map) new DirectFieldAccessor(groupedMessage.getHeaders()).getPropertyValue("headers");	
			boolean marked = innerMap.containsKey(MESSAGE_GROUP_MARKED);
			innerMap.remove(MESSAGE_GROUP_HEADER);
			innerMap.remove(MESSAGE_GROUP_MARKED);
			messageGroup.add(groupedMessage);
			if (marked){
				messageGroup.mark(groupedMessage);
			}
		}
	}

	private Query idQuery(UUID id) {
		return new Query(where("_id").is(id.toString()));
	}
	
	private Query groupedMessagesQuery() {
		return new Query(where("headers." + MESSAGE_GROUP_HEADER).exists(true));
	}
	private Query messagesForGroupQuery(String groupId) {
		return new Query(where("headers." + MESSAGE_GROUP_HEADER).is(groupId));
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
			if (source instanceof Message) {
				String payloadType = ((Message<?>) source).getPayload().getClass().getName();
				target.put("_payloadType", payloadType);
				target.put("_id", ((Message<?>) source).getHeaders().getId().toString());
			}
			super.write(source, target);
		}


		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public <S> S read(Class<S> clazz, DBObject source) {
			if (!Message.class.equals(clazz)) {
				return super.read(clazz, source);
			}
			if (source != null){
				Map<String, Object> headers = (Map<String, Object>) source.get("headers");
				Object payload = source.get("payload");
				Object payloadType = source.get("_payloadType");
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
				innerMap.put(MessageHeaders.ID, UUID.fromString(source.get("_id").toString()));
				innerMap.put(MessageHeaders.TIMESTAMP, headers.get(MessageHeaders.TIMESTAMP));
				return (S) message;
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
	
	@SuppressWarnings({ "rawtypes", "unused" })
	private void normalizeMessage(Message<?> message) {
		Map innerMap = (Map) new DirectFieldAccessor(message.getHeaders()).getPropertyValue("headers");	
		boolean marked = innerMap.containsKey(MESSAGE_GROUP_MARKED);
		innerMap.remove(MESSAGE_GROUP_HEADER);
		innerMap.remove(MESSAGE_GROUP_MARKED);
	}
	
	private SimpleMessageGroup getValidMessageGroupInstance(MessageGroup messageGroup) {
		Assert.notNull(messageGroup, "'messageGroup' must not null");
		Assert.isInstanceOf(SimpleMessageGroup.class, messageGroup, "MessageGroup must be an instance of SimpleMessageGroup");
		return (SimpleMessageGroup) messageGroup;
	}
}
