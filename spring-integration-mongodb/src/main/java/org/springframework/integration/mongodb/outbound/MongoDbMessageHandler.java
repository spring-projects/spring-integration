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

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.mongodb.MessageReadingMongoConverter;
import org.springframework.integration.mongodb.MessageWrapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The Message handler implementation for the  Mongo DB outbound adapter
 *
 * @author Amol Nayak
 *
 * @since 2.2
 *
 */
public class MongoDbMessageHandler extends AbstractMessageHandler implements  BeanClassLoaderAware, InitializingBean {

	private MongoTemplate template;

	private final String collection;

	private final MessageReadingMongoConverter converter;

	private final MongoDbFactory factory;

	private boolean isCollectionNameProvided;

	/**
	 * Constructor that instantiates the MongoTemplate using the MongoDBFactory instance
	 * for the provided database and collection.
	 *
	 */
	public MongoDbMessageHandler(MongoDbFactory factory, String collection) {
		//TODO: Do we need to support for dynamically accepting the collection with the
		//messages?
		this.collection = collection;
		this.factory = factory;
		converter = new MessageReadingMongoConverter(factory, new MongoMappingContext());
	}


	/**
	 * The constructor that accepts the MongoDbFactory instance and the collection that would be used
	 * is the default one generated for the {@link MessageWrapper} class that is peristed in the store
	 *
	 * @param factory
	 */
	public MongoDbMessageHandler(MongoDbFactory factory) {
		this(factory, null);
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		Assert.notNull(classLoader, "null classloader given");
		converter.setBeanClassLoader(classLoader);
	}

	@Override
	public void onInit() {
		converter.afterPropertiesSet();
		template = new MongoTemplate(factory, converter);
		//saving it here as we dont want to invoke hasText always in handleMessage
		isCollectionNameProvided = StringUtils.hasText(collection);
	}


	@Override
	public void handleMessageInternal(Message<?> message) throws Exception {
		if(isCollectionNameProvided) {
			template.save(new MessageWrapper(message), collection);
		}
		else {
			template.save(new MessageWrapper(message));
		}
	}


}
