/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.integration.Message;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.util.Assert;

/**
 * An alternate MongoDB {@link MessageStore} and {@link MessageGroupStore} which allows the user to
 * configure the instance of {@link MongoTemplate}. The mechanism of storing the messages/group of messages
 * in the store is and is different from {@link MongoDbMessageStore}. Since the store uses serialization of the
 * messages by default, all the headers, and the payload of the Message must implement {@link java.io.Serializable}
 * interface
 *
 * @author Amol Nayak
 * @since 3.0
 *
 */
public class ConfigurableMongoDbMessageStore extends AbstractMessageGroupStore implements MessageStore, InitializingBean {

	private static final String LAST_MODIFIED_TIME = "lastModifiedTime";
	private static final String MESSAGE_ID = "messageId";
	private static final String GROUP_ID = "groupId";
	private static final String LAST_RELEASED_SEQUENCE = "lastReleasedSequence";
	private static final String COMPLETE = "complete";
	private volatile SerializingConverter serializer;
	private volatile DeserializingConverter deserializer;
	private volatile MongoTemplate mongoTemplate;
	private final String collectionName;

	public final static String DEFAULT_COLLECTION_NAME = "configurableStoreMessages";




	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.mongoTemplate, "MongoTemplate instance cannot be null");
	}

	public ConfigurableMongoDbMessageStore() {
		this.collectionName = DEFAULT_COLLECTION_NAME;
	}

	public ConfigurableMongoDbMessageStore(MongoDbFactory mongoDbFactory) {
		this(mongoDbFactory, DEFAULT_COLLECTION_NAME);
	}

	public ConfigurableMongoDbMessageStore(MongoDbFactory mongoDbFactory, String collectionName) {
		this.serializer = new SerializingConverter();
		this.deserializer = new DeserializingConverter();
		this.mongoTemplate = new MongoTemplate(mongoDbFactory);
		this.collectionName = collectionName;
	}


	@Override
	public Message<?> getMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper wrapper = this.mongoTemplate.findOne(new Query(where(MESSAGE_ID).is(id.toString())),
							MessageWrapper.class, this.collectionName);
		Message<?> message;
		if(wrapper != null) {
			message = (Message<?>)this.deserializer.convert(wrapper.getMessage());
		}
		else {
			message = null;
		}
		return message;
	}

	@Override
	public <T> Message<T> addMessage(Message<T> message) {
		UUID uid = message.getHeaders().getId();
		MessageWrapper wrapper = new MessageWrapper();
		wrapper.setMessageId(uid.toString());
		byte[] messageBytes = this.serializer.convert(message);
		wrapper.setMessage(messageBytes);
		this.mongoTemplate.insert(wrapper, this.collectionName);
		return message;
	}

	@Override
	public Message<?> removeMessage(UUID id) {
		Assert.notNull(id, "'id' must not be null");
		MessageWrapper wrapper = this.mongoTemplate.findAndRemove(new Query(where(MESSAGE_ID).is(id.toString())),
							MessageWrapper.class, this.collectionName);
		Message<?> message;
		if(wrapper != null) {
			message = (Message<?>)this.deserializer.convert(wrapper.getMessage());
		}
		else {
			message = null;
		}
		return message;
	}

	@Override
	public long getMessageCount() {
		return this.mongoTemplate.getCollection(this.collectionName).getCount();
	}


	@Override
	public int messageGroupSize(Object groupId) {
		Query query = new Query(where(GROUP_ID).is(groupId));
		long lCount = this.mongoTemplate.count(query, this.collectionName);
		Assert.isTrue(lCount <= Integer.MAX_VALUE, "Message count is out of Integer's range");
		return (int) lCount;
	}

	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		Query groupSelectQuery = new Query(where(GROUP_ID).is(groupId));
		List<MessageWrapper> groupWrapper = this.mongoTemplate.find(groupSelectQuery,
										MessageWrapper.class, this.collectionName);
		List<Message<?>> messages = new ArrayList<Message<?>>();
		MessageWrapper firstWrapper = null;
		long lastModifiedTime = 0;
		if(groupWrapper != null && !groupWrapper.isEmpty()) {
			Iterator<MessageWrapper> iterator = groupWrapper.iterator();
			firstWrapper = iterator.next();
			messages.add((Message<?>)this.deserializer.convert(firstWrapper.getMessage()));
			lastModifiedTime = firstWrapper.getLastModifiedTime();
			while(iterator.hasNext()) {
				MessageWrapper wrapper = iterator.next();
				messages.add((Message<?>)this.deserializer.convert(wrapper.getMessage()));
				long modifiedTime = wrapper.getLastModifiedTime();
				lastModifiedTime = modifiedTime > lastModifiedTime ? modifiedTime : lastModifiedTime;
			}
		}
		long createdTime = 0;
		boolean complete = false;
		int lastReleasedSequence = 0;
		if(firstWrapper != null) {
			createdTime = firstWrapper.getCreatedTime();
			complete = firstWrapper.isComplete();
			lastReleasedSequence = firstWrapper.getLastReleasedSequence();
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
		MessageGroup group = this.getMessageGroup(groupId);
		byte[] serializedMessage = this.serializer.convert(message);
		long createdTime = group.getTimestamp();

		int lastReleasedSequenceNumber = group.getLastReleasedMessageSequenceNumber();
		boolean complete = group.isComplete();
		MessageWrapper wrapper = new MessageWrapper();
		wrapper.setComplete(complete);
		wrapper.setGroupId(groupId);
		wrapper.setMessageId(message.getHeaders().getId().toString());
		wrapper.setLastReleasedSequence(lastReleasedSequenceNumber);
		if(createdTime == 0) {
			wrapper.setCreatedTime(System.currentTimeMillis());
		}
		else {
			wrapper.setCreatedTime(createdTime);
		}
		wrapper.setLastModifiedTime(System.currentTimeMillis());
		wrapper.setMessage(serializedMessage);
		this.mongoTemplate.insert(wrapper, this.collectionName);
		return this.getMessageGroup(groupId);
	}

	@Override
	public MessageGroup removeMessageFromGroup(Object key,
			Message<?> messageToRemove) {
		Assert.notNull(key, "'groupId' must not be null");
		Assert.notNull(messageToRemove, "'messageToRemove' must not be null");
		Query query = new Query(where(GROUP_ID).is(key)
								.and(MESSAGE_ID).is(messageToRemove.getHeaders().getId().toString()));
		this.mongoTemplate.remove(query, this.collectionName);
		this.updateGroup(key);
		return this.getMessageGroup(key);
	}

	/**
	 * Updates the last updated time stamp of the most recently updated message
	 * @param key
	 */
	private void updateGroup(Object key) {
		Query query = new Query(where(GROUP_ID).is(key));
		Update update = Update.update(LAST_MODIFIED_TIME, System.currentTimeMillis());
		this.mongoTemplate.updateFirst(query, update, MessageWrapper.class);
	}

	@Override
	public void removeMessageGroup(Object groupId) {
		Query query = new Query(where(GROUP_ID).is(groupId));
		this.mongoTemplate.remove(query, this.collectionName);
	}

	@Override
	public void setLastReleasedSequenceNumberForGroup(Object groupId,
			int sequenceNumber) {
		Query query = new Query(where(GROUP_ID).is(groupId));
		Update update = Update.update(LAST_RELEASED_SEQUENCE, sequenceNumber);
		this.mongoTemplate.updateMulti(query, update, this.collectionName);
	}

	@Override
	public Iterator<MessageGroup> iterator() {
		Map<Object, MessageGroup> messageGroupMap = new HashMap<Object, MessageGroup>();
		Query query = new Query(where(GROUP_ID).exists(true));
		//TODO: Need just one field, needs to be fixed when SDM starts supporting projections of limited fields
		List<MessageWrapper> messageWrappers = this.mongoTemplate.find(query, MessageWrapper.class, this.collectionName);
		for(MessageWrapper wrapper:messageWrappers) {
			Object groupId = wrapper.getGroupId();
			if(!messageGroupMap.containsKey(groupId)) {
				messageGroupMap.put(groupId, this.getMessageGroup(groupId));
			}
		}
		return messageGroupMap.values().iterator();
	}

	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Query query = new Query(where(GROUP_ID).is(groupId));
		MessageWrapper wrapper = this.mongoTemplate.findAndRemove(query, MessageWrapper.class, this.collectionName);
		Message<?> message;
		if(wrapper != null) {
			message = (Message<?>)this.deserializer.convert(wrapper.getMessage());
		}
		else {
			message = null;
		}
		return message;
	}

	@Override
	public void completeGroup(Object groupId) {
		Query query = new Query(where(GROUP_ID).is(groupId));
		Update update = Update.update(COMPLETE, true);
		this.mongoTemplate.updateMulti(query, update, this.collectionName);
	}


	/**
	 * A converter for serializing messages to byte arrays for storage.
	 *
	 * @param serializer the serializer to set
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void setSerializer(Serializer<? extends Message<?>> serializer) {
		Assert.notNull(serializer, "serializer is null");
		this.serializer = new SerializingConverter((Serializer)serializer);
	}

	/**
	 * A converter for deserializing byte arrays to messages.
	 *
	 * @param deserializer the deserializer to set
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setDeserializer(Deserializer<? extends Message<?>> deserializer) {
		Assert.notNull(deserializer, "deserializer is null");
		this.deserializer = new DeserializingConverter((Deserializer)deserializer);
	}



	/**
	 * The {@link MongoTemplate} instance
	 * @param mongoTemplate
	 */
	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		Assert.notNull(mongoTemplate, "mongoTemplate is null");
		this.mongoTemplate = mongoTemplate;
	}




	/**
	 * The wrapper class that is used to store the {@link MessageGroup} as well as the
	 * {@link MessageGroupStore} in a Mongo backed store
	 *
	 * @author Amol Nayak
	 *
	 */
	private static class MessageWrapper {
		private String messageId;
		private Object groupId;
		private long lastModifiedTime;
		private long createdTime;
		private boolean complete;
		private int lastReleasedSequence;
		private byte[] message;


		@SuppressWarnings("unused")
		public String getMessageId() {
			return messageId;
		}

		public void setMessageId(String messageId) {
			this.messageId = messageId;
		}

		public byte[] getMessage() {
			return message;
		}

		public void setMessage(byte[] message) {
			this.message = message;
		}

		public Object getGroupId() {
			return groupId;
		}

		public void setGroupId(Object groupId) {
			this.groupId = groupId;
		}

		public long getLastModifiedTime() {
			return lastModifiedTime;
		}

		public void setLastModifiedTime(long lastModifiedTime) {
			this.lastModifiedTime = lastModifiedTime;
		}

		public long getCreatedTime() {
			return createdTime;
		}

		public void setCreatedTime(long createdTime) {
			this.createdTime = createdTime;
		}

		public boolean isComplete() {
			return complete;
		}

		public void setComplete(boolean complete) {
			this.complete = complete;
		}

		public int getLastReleasedSequence() {
			return lastReleasedSequence;
		}

		public void setLastReleasedSequence(int lastReleasedSequence) {
			this.lastReleasedSequence = lastReleasedSequence;
		}
	}
}
