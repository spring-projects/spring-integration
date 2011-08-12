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
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;
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
	
	private final MongoTemplate template;

	private final String collectionName;

	private volatile ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
	
	private final Map<Object, MessageGroup> messageGroups = new HashMap<Object, MessageGroup>();


	public MongoDbMessageStore(MongoDbFactory mongoDbFactory) {
		this(mongoDbFactory, null);
	}

	@SuppressWarnings("rawtypes")
	public MongoDbMessageStore(MongoDbFactory mongoDbFactory, String collectionName) {
		Assert.notNull(mongoDbFactory, "mongoDbFactory must not be null");
		MessageReadingMongoConverter converter = new MessageReadingMongoConverter(mongoDbFactory, new MongoMappingContext());
		this.template = new MongoTemplate(mongoDbFactory, converter);
		this.collectionName = (StringUtils.hasText(collectionName)) ? collectionName : DEFAULT_COLLECTION_NAME;
		//this.template.createCollection(collectionName);
		
		List<Message> groupedMessages = this.template.find(this.groupedMessagesQuery(), Message.class, DEFAULT_COLLECTION_NAME);
		
		for (Message<?> groupedMessage : groupedMessages) {
			Object messageGroupId = groupedMessage.getHeaders().get("message_group");
			SimpleMessageGroup messageGroup = (SimpleMessageGroup) this.messageGroups.get(messageGroupId);
			if (messageGroup == null){
				messageGroup = new SimpleMessageGroup(messageGroupId);
				this.messageGroups.put(messageGroupId, messageGroup);
			}
			messageGroup.add(groupedMessage);
		}
	}


	public void setBeanClassLoader(ClassLoader classLoader) {
		Assert.notNull(classLoader, "classLoader must not be null");
		this.classLoader = classLoader;
	}

	public <T> Message<T> addMessage(Message<T> message) {
		this.template.insert(message, collectionName);
		return message;
	}

	public Message<?> getMessage(UUID id) {
		try {
			return this.template.findOne(this.idQuery(id), Message.class, this.collectionName);
		} catch (Exception e) {
			// ignore, but should be removed once https://jira.springsource.org/browse/DATADOC-239 is fixed
		}
		return null;
	}

	@ManagedAttribute
	public long getMessageCount() {
		return this.template.getCollection(DEFAULT_COLLECTION_NAME).getCount();
	}

	public Message<?> removeMessage(UUID id) {
		return this.template.findAndRemove(idQuery(id), Message.class, this.collectionName);
	}

	private Query idQuery(UUID id) {
		return new Query(where("_id").is(id.toString()));
	}
	
	private Query groupedMessagesQuery() {
		return new Query(where("message_group").exists(true));
	}
	private Query messagesForGroupQuery(String groupId) {
		return new Query(where("message_group").is(groupId));
	}
	
	


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


	public MessageGroup getMessageGroup(Object groupId) {
		SimpleMessageGroup messageGroup = (SimpleMessageGroup) this.messageGroups.get(groupId);
		if (messageGroup == null){
			messageGroup = new SimpleMessageGroup(groupId);
			this.messageGroups.put(groupId, messageGroup);
		}
		return messageGroup;
	}

	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		SimpleMessageGroup messageGroup = (SimpleMessageGroup) this.getMessageGroup(groupId);
		HashMap<String, Object> headers = new HashMap<String, Object>(message.getHeaders());
		
		headers.put("message_group", groupId);
		MessageHeaders h = new MessageHeaders(headers);
		//Message<?> groupedMessage = MessageBuilder.fromMessage(message).setHeader("message_group", groupId).build();
		DirectFieldAccessor haccessor = new DirectFieldAccessor(h);
		haccessor.setPropertyValue("headers", headers);
		DirectFieldAccessor maccessor = new DirectFieldAccessor(message);
		maccessor.setPropertyValue("headers", h);
		messageGroup.add(message);
		this.addMessage(message);
		return messageGroup;
	}

	public MessageGroup markMessageGroup(MessageGroup group) {
		SimpleMessageGroup messageGroup = (SimpleMessageGroup) group;
		messageGroup.markAll();
		return messageGroup;
	}

	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		SimpleMessageGroup messageGroup = (SimpleMessageGroup) this.getMessageGroup(groupId);
		messageGroup.remove(messageToRemove);
		this.removeMessage(messageToRemove.getHeaders().getId());
		return messageGroup;
	}

	public MessageGroup markMessageFromGroup(Object groupId, Message<?> messageToMark) {
		SimpleMessageGroup messageGroup = (SimpleMessageGroup) this.getMessageGroup(groupId);
		messageGroup.mark(messageToMark);
		return messageGroup;
	}

	@SuppressWarnings("rawtypes")
	public void removeMessageGroup(Object groupId) {
		
		List<Message> groupedMessages = this.template.find(this.messagesForGroupQuery(groupId.toString()), Message.class, DEFAULT_COLLECTION_NAME);
		for (Message<?> groupedMessage : groupedMessages) {
			this.removeMessageFromGroup(groupId, groupedMessage);
		}
		this.messageGroups.remove(groupId);
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		return this.messageGroups.values().iterator();
	}

}
