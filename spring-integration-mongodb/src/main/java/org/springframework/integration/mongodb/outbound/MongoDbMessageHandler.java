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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.WriteResultChecking;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.mongodb.MessageReadingMongoConverter;
import org.springframework.integration.mongodb.MessageWrapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.mongodb.WriteConcern;

/**
 * The Message handler implementation for the  Mongo DB outbound adapter
 *
 * @author Amol Nayak
 *
 * @since 2.2
 *
 */
public class MongoDbMessageHandler implements MessageHandler, BeanClassLoaderAware, InitializingBean {

	private static Log logger = LogFactory.getLog(MongoDbMessageHandler.class);

	private WriteResultChecking writeResultChecking = WriteResultChecking.NONE;

	private MongoTemplate template;

	private WriteConcern writeConcern = WriteConcern.NONE;

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

	public void afterPropertiesSet() throws Exception {
		converter.afterPropertiesSet();
		template = new MongoTemplate(factory, converter);
		template.setWriteConcern(writeConcern);
		template.setWriteResultChecking(writeResultChecking);
		//saving it here as we dont want to invoke hasText always in handleMessage
		isCollectionNameProvided = StringUtils.hasText(collection);
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.core.MessageHandler#handleMessage(org.springframework.integration.Message)
	 */
	public void handleMessage(Message<?> message) throws MessagingException {
		Assert.notNull(message, "provided message is null");
		try {
			if(isCollectionNameProvided) {
				template.save(new MessageWrapper(message), collection);
			}
			else {
				template.save(new MessageWrapper(message));
			}
		} catch (DataIntegrityViolationException e) {
			logger.error("Data integrity violation exception caught, " + e.getMessage());
			throw new MessagingException(message, "Data integrity violation exception caught", e);
			//This exception will not be caught for a bug in Spring Data Mongo which doesn't
			//consider the WriteResultChecking for save and inserts but only for updates and removes
			//issue DATAMONGO-480 is raised for this.
		}
	}

	/**
	 * Sets the {@link WriteResultChecking} for the outbound adapters. The write to mongo DB
	 * is by default asynchronous and hence any errors in writing of data will not be caught.
	 * Setting this value to <i>LOG</i> will log the error and setting to <i>EXCEPTION</i>
	 * will thrown an exception if write fails. The default is <i>NONE</i> and is the suggested value
	 * in production environments where the application is stable and performance is important.
	 * Set it to  <i>LOG</i> or <i>EXCEPTION</i> during development of the application.
	 *
	 * @param writeResultChecking
	 */
	public void setWriteResultChecking(WriteResultChecking writeResultChecking) {
		Assert.notNull(writeResultChecking, "Null WriteResultChecking value provided");
		this.writeResultChecking = writeResultChecking;
	}

	/**
	 * The write concern to be used for the writes, by default it is <i>NONE</i>.
	 * @param writeConcern
	 */
	public void setWriteConcern(WriteConcern writeConcern) {
		this.writeConcern = writeConcern;
	}
}
