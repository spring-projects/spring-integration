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
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.mongodb.store.ConfigurableMongoDbMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 4.0
 */
public class MessageReadingMongoConverter extends MappingMongoConverter implements BeanClassLoaderAware {

	private static final Log logger = LogFactory.getLog(MessageReadingMongoConverter.class);

	private final static String CREATED_TIME_KEY = "createdTime";

	private final static String PAYLOAD_TYPE_KEY = "_payloadType";

	private final static String HEADERS_KEY = "headers";

	private final static String PAYLOAD_KEY = "payload";

	private ClassLoader classLoader;

	public MessageReadingMongoConverter(MongoDbFactory mongoDbFactory) {
		super(mongoDbFactory, new MongoMappingContext());
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		List<Converter<?, ?>> customConverters = new ArrayList<Converter<?,?>>();
		customConverters.add(new MessageHistoryToDBObjectConverter());
		customConverters.add(new DBObjectToGenericMessageConverter());
		this.setCustomConversions(new CustomConversions(customConverters));
		super.afterPropertiesSet();
	}

	@Override
	public void write(Object source, DBObject target) {
		Assert.isInstanceOf(MessageDocument.class, source);

		MessageDocument document = (MessageDocument) source;
		Message<?> message = document.getMessage();

		target.put(CREATED_TIME_KEY, document.getCreatedTime());
		target.put(ConfigurableMongoDbMessageStore.GROUP_ID, document.getGroupId());
		target.put(ConfigurableMongoDbMessageStore.LAST_MODIFIED_TIME, document.getLastModifiedTime());
		target.put(ConfigurableMongoDbMessageStore.LAST_RELEASED_SEQUENCE, document.getLastReleasedSequence());
		target.put(ConfigurableMongoDbMessageStore.COMPLETE, document.isComplete());
		target.put(ConfigurableMongoDbMessageStore.MESSAGE_ID, message.getHeaders().getId());

		target.put(HEADERS_KEY, this.convertToMongoType(message.getHeaders()));

		target.put(PAYLOAD_TYPE_KEY, message.getPayload().getClass().getName());
		Object payload = message.getPayload();
		if (!this.conversions.isSimpleType(payload.getClass())) {
			DBObject dbo = new BasicDBObject();
			super.write(payload, dbo);
			payload = dbo;
		}
		target.put(PAYLOAD_KEY, payload);
		target.put("_class", MessageDocument.class.getName());
	}


	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <S> S read(Class<S> clazz, DBObject source) {
		if (!MessageDocument.class.equals(clazz)) {
			return super.read(clazz, source);
		}
		if (source != null) {
			Map<String, Object> headers = this.normalizeHeaders((Map<String, Object>) source.get(HEADERS_KEY));

			Object payload = source.get(PAYLOAD_KEY);
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
			innerMap.put(MessageHeaders.ID, headers.get(MessageHeaders.ID));
			innerMap.put(MessageHeaders.TIMESTAMP, headers.get(MessageHeaders.TIMESTAMP));
			Long groupTimestamp = (Long)source.get(CREATED_TIME_KEY);
			Long lastModified = (Long)source.get(ConfigurableMongoDbMessageStore.LAST_MODIFIED_TIME);
			Integer lastReleasedSequenceNumber = (Integer)source.get(ConfigurableMongoDbMessageStore.LAST_RELEASED_SEQUENCE);
			Boolean completeGroup = (Boolean)source.get(ConfigurableMongoDbMessageStore.COMPLETE);

			MessageDocument document = new MessageDocument(message);

			if (source.containsField(ConfigurableMongoDbMessageStore.GROUP_ID)){
				document.setGroupId(source.get(ConfigurableMongoDbMessageStore.GROUP_ID));
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


	private static class MessageHistoryToDBObjectConverter implements Converter<MessageHistory,DBObject> {

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
			Map<String, Object> headers = MessageReadingMongoConverter.this.normalizeHeaders((Map<String, Object>) source.get("headers"));

			Object payload = source.get(PAYLOAD_KEY);
			Object payloadType = source.get(PAYLOAD_TYPE_KEY);
			if (payloadType != null && payload instanceof DBObject) {
				try {
					Class<?> payloadClass = ClassUtils.forName(payloadType.toString(), classLoader);
					payload = MessageReadingMongoConverter.this.read(payloadClass, (DBObject) payload);
				}
				catch (Exception e) {
					throw new IllegalStateException("failed to load class: " + payloadType, e);
				}
			}

			@SuppressWarnings("rawtypes")
			GenericMessage<Object> message = new GenericMessage(payload, headers);
			Map<String, Object> innerMap = (Map<String, Object>) new DirectFieldAccessor(message.getHeaders()).getPropertyValue("headers");
			// using reflection to set ID and TIMESTAMP since they are immutable through MessageHeaders
			innerMap.put(MessageHeaders.ID, headers.get(MessageHeaders.ID));
			innerMap.put(MessageHeaders.TIMESTAMP, headers.get(MessageHeaders.TIMESTAMP));

			return message;
		}

	}

}
