/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.mongodb;

import static org.springframework.integration.history.MessageHistory.NAME_PROPERTY;
import static org.springframework.integration.history.MessageHistory.TIMESTAMP_PROPERTY;
import static org.springframework.integration.history.MessageHistory.TYPE_PROPERTY;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.CREATED_DATE;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.GROUP_COMPLETE_KEY;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.GROUP_ID_KEY;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.GROUP_TIMESTAMP_KEY;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.GROUP_UPDATE_TIMESTAMP_KEY;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.LAST_RELEASED_SEQUENCE_NUMBER;
import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.PAYLOAD_TYPE_KEY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * 	Custom implementation of the {@link MappingMongoConverter} strategy.
 *
 *	@author Mark Fisher
 *	@author Oleg Zhurakousky
 *	@author Sean Brandt
 *	@author Amol Nayak
 *
 * 	@since 2.2
 *
 */
public class MessageReadingMongoConverter extends MappingMongoConverter implements BeanClassLoaderAware {

	private final Log logger = LogFactory.getLog(MessageReadingMongoConverter.class);

	private volatile ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

	public MessageReadingMongoConverter(MongoDbFactory mongoDbFactory,
			MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {
		super(mongoDbFactory, mappingContext);
	}



	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}



	@Override
	public void afterPropertiesSet() {
		List<Converter<?, ?>> customConverters = new ArrayList<Converter<?,?>>();
		customConverters.add(new UuidToDBObjectConverter());
		customConverters.add(new DBObjectToUUIDConverter());
		customConverters.add(new MessageHistoryToDBObjectConverter());
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
			Map<String, Object> headers = this.normalizeHeaders((Map<String, Object>) source.get("headers"));

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
			innerMap.put(MessageHeaders.ID, headers.get(MessageHeaders.ID));
			innerMap.put(MessageHeaders.TIMESTAMP, headers.get(MessageHeaders.TIMESTAMP));
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
				wrapper.set_Group_complete(completeGroup.booleanValue());
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
				Object type = source.get("_class");
				try {
					Class<?> typeClass = ClassUtils.forName(type.toString(), classLoader);
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

	private static class UuidToDBObjectConverter implements Converter<UUID, DBObject> {
		public DBObject convert(UUID source) {
			BasicDBObject dbObject = new BasicDBObject();
			dbObject.put("_value", source.toString());
			dbObject.put("_class", source.getClass().getName());
			return dbObject;
		}
	}

	private static class DBObjectToUUIDConverter implements Converter<DBObject, UUID> {
		public UUID convert(DBObject source) {
			UUID id = UUID.fromString((String) source.get("_value"));
			return id;
		}
	}


	private static class MessageHistoryToDBObjectConverter implements Converter<MessageHistory,DBObject> {

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
}
