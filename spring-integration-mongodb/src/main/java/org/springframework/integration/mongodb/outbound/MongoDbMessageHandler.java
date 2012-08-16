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
package org.springframework.integration.mongodb.outbound;

import static org.springframework.integration.mongodb.MongoDbIntegrationConstants.DEFAULT_COLLECTION_NAME;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.mongodb.MessageReadingMongoConverter;
import org.springframework.integration.mongodb.MessageWrapper;
import org.springframework.integration.mongodb.MongoDbIntegrationConstants;
import org.springframework.util.Assert;

/**
 * The Message handler implementation for the  Mongo DB outbound adapter
 *
 * @author Amol Nayak
 *
 * @since 2.2
 *
 */
public class MongoDbMessageHandler extends AbstractMessageHandler implements  BeanClassLoaderAware, InitializingBean {

	private final MongoOperations mongoOperations;

	private final String collection;

	private final MessageReadingMongoConverter converter;

	/**
	 * Constructor that instantiates the MongoOperations using the MongoDBFactory instance
	 * for the provided database and collection.
	 *
	 */
	public MongoDbMessageHandler(MongoDbFactory factory, String collection) {
		Assert.notNull(factory, "'factory' must not be null");
		this.collection = collection == null?DEFAULT_COLLECTION_NAME:collection;
		converter = new MessageReadingMongoConverter(factory, new MongoMappingContext());
		converter.afterPropertiesSet();
		mongoOperations = new MongoTemplate(factory, converter);
	}

	/**
	 * Constructor that instantiates the MongoTemplate using the MongoDBFactory instance
	 * for the provided database and the DEFAULT_COLLECTION_NAME from {@link MongoDbIntegrationConstants}
	 *
	 */
	public MongoDbMessageHandler(MongoDbFactory factory) {
		this(factory,DEFAULT_COLLECTION_NAME);
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		Assert.notNull(classLoader, "null classloader given");
		converter.setBeanClassLoader(classLoader);
	}




	@Override
	public void handleMessageInternal(Message<?> message) throws Exception {
		mongoOperations.save(new MessageWrapper(message), collection);
	}

}
