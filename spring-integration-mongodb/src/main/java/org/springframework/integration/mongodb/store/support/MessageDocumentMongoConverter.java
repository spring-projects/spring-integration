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

package org.springframework.integration.mongodb.store.support;

import static org.springframework.integration.history.MessageHistory.NAME_PROPERTY;
import static org.springframework.integration.history.MessageHistory.TIMESTAMP_PROPERTY;
import static org.springframework.integration.history.MessageHistory.TYPE_PROPERTY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.message.MutableMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * The custom {@link MappingMongoConverter} to decompose {@link Message} as a MongoDB field set.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 4.0
 */
public class MessageDocumentMongoConverter extends MappingMongoConverter implements BeanClassLoaderAware {

	private static final Log logger = LogFactory.getLog(MessageDocumentMongoConverter.class);

	private final static String PAYLOAD_TYPE = "_payloadType";

	private final static String MESSAGE_TYPE = "_messageType";

	private final static String HEADERS = "headers";

	private final static String PAYLOAD = "payload";

	private ClassLoader classLoader;

	public MessageDocumentMongoConverter(MongoDbFactory mongoDbFactory) {
		super(mongoDbFactory, new MongoMappingContext());
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		List<Object> customConverters = new ArrayList<Object>();
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
		Assert.isInstanceOf(MessageDocument.class, source);

		MessageDocument document = (MessageDocument) source;
		Message<?> message = document.getMessage();

		target.put(MessageDocumentFields.CREATED_TIME, document.getCreatedTime());
		target.put(MessageDocumentFields.GROUP_ID, document.getGroupId());
		target.put(MessageDocumentFields.LAST_MODIFIED_TIME, document.getLastModifiedTime());
		target.put(MessageDocumentFields.LAST_RELEASED_SEQUENCE, document.getLastReleasedSequence());
		target.put(MessageDocumentFields.COMPLETE, document.isComplete());
		target.put(MessageDocumentFields.MESSAGE_ID, message.getHeaders().getId());

		target.put(HEADERS, this.convertToMongoType(message.getHeaders()));

		target.put(MESSAGE_TYPE, message.getClass().getName());
		target.put(PAYLOAD_TYPE, message.getPayload().getClass().getName());
		Object payload = message.getPayload();
		if (!this.conversions.isSimpleType(payload.getClass())) {
			DBObject dbo = new BasicDBObject();
			super.write(payload, dbo);
			payload = dbo;
		}
		else if (payload instanceof Throwable) {
			payload = this.convertToMongoType(payload);
		}

		target.put(PAYLOAD, payload);

		if (message instanceof AdviceMessage) {
			DBObject dbo = new BasicDBObject();
			super.write(((AdviceMessage) message).getInputMessage(), dbo);
			target.put("inputMessage", dbo);
		}
		target.put("_class", MessageDocument.class.getName());
	}


	@Override
	@SuppressWarnings("unchecked")
	public <S> S read(Class<S> clazz, DBObject source) {
		if (!MessageDocument.class.equals(clazz)) {
			return super.read(clazz, source);
		}
		if (source != null) {
			Message<?> message = null;
			Object messageType = source.get(MESSAGE_TYPE);
			try {
				message = (Message<?>) this.read(ClassUtils.forName(messageType.toString(), classLoader), source);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalStateException("failed to load class: " + messageType, e);
			}

			Long groupTimestamp = (Long)source.get(MessageDocumentFields.CREATED_TIME);
			Long lastModified = (Long)source.get(MessageDocumentFields.LAST_MODIFIED_TIME);
			Integer lastReleasedSequenceNumber = (Integer)source.get(MessageDocumentFields.LAST_RELEASED_SEQUENCE);
			Boolean completeGroup = (Boolean)source.get(MessageDocumentFields.COMPLETE);

			MessageDocument document = new MessageDocument(message);

			if (source.containsField(MessageDocumentFields.GROUP_ID)){
				document.setGroupId(source.get(MessageDocumentFields.GROUP_ID));
			}
			if (groupTimestamp != null){
				document.setCreatedTime(groupTimestamp);
			}
			if (lastModified != null){
				document.setLastModifiedTime(lastModified);
			}
			if (lastReleasedSequenceNumber != null){
				document.setLastReleasedSequence(lastReleasedSequenceNumber);
			}

			if (completeGroup != null){
				document.setComplete(completeGroup);
			}

			return (S) document;
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
		Object payload = source.get(PAYLOAD);
		if (payload instanceof DBObject) {
			DBObject payloadObject = (DBObject) payload;
			Object payloadType = payloadObject.get("_class");
			try {
				Class<?> payloadClass = ClassUtils.forName(payloadType.toString(), classLoader);
				payload = MessageDocumentMongoConverter.this.read(payloadClass, payloadObject);
			}
			catch (Exception e) {
				throw new IllegalStateException("failed to load class: " + payloadType, e);
			}
		}
		return payload;
	}


	private static class MessageHistoryToDBObjectConverter implements Converter<MessageHistory, DBObject> {

		@Override
		public DBObject convert(MessageHistory source) {
			BasicDBObject obj = new BasicDBObject();
			obj.put("_class", MessageHistory.class.getName());
			BasicDBList dbList = new BasicDBList();
			for (Properties properties : source) {
				BasicDBObject dbo = new BasicDBObject();
				dbo.put(NAME_PROPERTY, properties.getProperty(NAME_PROPERTY));
				dbo.put(TYPE_PROPERTY, properties.getProperty(TYPE_PROPERTY));
				dbo.put(TIMESTAMP_PROPERTY, properties.getProperty(TIMESTAMP_PROPERTY));
				dbList.add(dbo);
			}
			obj.put("components", dbList);
			return obj;
		}
	}

	private class DBObjectToGenericMessageConverter implements Converter<DBObject, GenericMessage<?>> {

		@Override
		@SuppressWarnings("unchecked")
		public GenericMessage<?> convert(DBObject source) {
			Map<String, Object> headers = MessageDocumentMongoConverter.this.normalizeHeaders((Map<String, Object>) source.get("headers"));

			GenericMessage<?> message = new GenericMessage<Object>(MessageDocumentMongoConverter.this.extractPayload(source), headers);
			Map<String, Object> innerMap = (Map<String, Object>) new DirectFieldAccessor(message.getHeaders()).getPropertyValue("headers");
			// using reflection to set ID and TIMESTAMP since they are immutable through MessageHeaders
			innerMap.put(MessageHeaders.ID, headers.get(MessageHeaders.ID));
			innerMap.put(MessageHeaders.TIMESTAMP, headers.get(MessageHeaders.TIMESTAMP));

			return message;
		}

	}

	private class DBObjectToMutableMessageConverter implements Converter<DBObject, MutableMessage<?>> {

		@Override
		public MutableMessage<?> convert(DBObject source) {
			@SuppressWarnings("unchecked")
			Map<String, Object> headers = MessageDocumentMongoConverter.this.normalizeHeaders((Map<String, Object>) source.get("headers"));

			return new MutableMessage<Object>(MessageDocumentMongoConverter.this.extractPayload(source), headers);
		}

	}

	private class DBObjectToAdviceMessageConverter implements Converter<DBObject, AdviceMessage> {

		@Override
		@SuppressWarnings("unchecked")
		public AdviceMessage convert(DBObject source) {
			Map<String, Object> headers = MessageDocumentMongoConverter.this.normalizeHeaders((Map<String, Object>) source.get("headers"));

			Message<?> inputMessage = null;

			if (source.get("inputMessage") != null) {
				DBObject inputMessageObject = (DBObject) source.get("inputMessage");
				Object inputMessageType = inputMessageObject.get("_class");
				try {
					Class<?> messageClass = ClassUtils.forName(inputMessageType.toString(), classLoader);
					inputMessage = (Message<?>) MessageDocumentMongoConverter.this.read(messageClass, inputMessageObject);
				}
				catch (Exception e) {
					throw new IllegalStateException("failed to load class: " + inputMessageType, e);
				}
			}

			AdviceMessage message = new AdviceMessage(MessageDocumentMongoConverter.this.extractPayload(source), headers, inputMessage);
			Map<String, Object> innerMap = (Map<String, Object>) new DirectFieldAccessor(message.getHeaders()).getPropertyValue("headers");
			// using reflection to set ID and TIMESTAMP since they are immutable through MessageHeaders
			innerMap.put(MessageHeaders.ID, headers.get(MessageHeaders.ID));
			innerMap.put(MessageHeaders.TIMESTAMP, headers.get(MessageHeaders.TIMESTAMP));

			return message;
		}

	}

	private class DBObjectToErrorMessageConverter implements Converter<DBObject, ErrorMessage> {

		private final Converter<byte[], Object> deserializingConverter = new DeserializingConverter();

		@Override
		@SuppressWarnings("unchecked")
		public ErrorMessage convert(DBObject source) {
			Map<String, Object> headers = MessageDocumentMongoConverter.this.normalizeHeaders((Map<String, Object>) source.get("headers"));

			Object payload = this.deserializingConverter.convert((byte[]) source.get(PAYLOAD));
			ErrorMessage message = new ErrorMessage((Throwable) payload, headers);
			Map<String, Object> innerMap = (Map<String, Object>) new DirectFieldAccessor(message.getHeaders()).getPropertyValue("headers");
			// using reflection to set ID and TIMESTAMP since they are immutable through MessageHeaders
			innerMap.put(MessageHeaders.ID, headers.get(MessageHeaders.ID));
			innerMap.put(MessageHeaders.TIMESTAMP, headers.get(MessageHeaders.TIMESTAMP));

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

}
