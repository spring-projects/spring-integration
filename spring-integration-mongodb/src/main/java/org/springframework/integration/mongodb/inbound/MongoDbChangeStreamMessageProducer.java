/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mongodb.inbound;

import org.bson.Document;

import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mongodb.support.MongoHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;

/**
 * A {@link MessageProducerSupport} for MongoDB Change Stream implementation.
 * The functionality is based on the
 * {@link ReactiveMongoOperations#changeStream(String, ChangeStreamOptions, Class)}
 * and {@link MessageProducerSupport#subscribeToPublisher(org.reactivestreams.Publisher)} consumption.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class MongoDbChangeStreamMessageProducer extends MessageProducerSupport {

	private final ReactiveMongoOperations mongoOperations;

	private Class<?> domainType = Document.class;

	@Nullable
	private String collection;

	private ChangeStreamOptions options = ChangeStreamOptions.empty();

	private boolean extractBody = true;

	/**
	 * Create an instance based on the provided {@link ReactiveMongoOperations}.
	 * @param mongoOperations the {@link ReactiveMongoOperations} to use.
	 * @see ReactiveMongoOperations#changeStream(String, ChangeStreamOptions, Class)
	 */
	public MongoDbChangeStreamMessageProducer(ReactiveMongoOperations mongoOperations) {
		Assert.notNull(mongoOperations, "'mongoOperations' must not be null");
		this.mongoOperations = mongoOperations;
	}

	/**
	 * Specify an object type to convert an event body to.
	 * Defaults to {@link Document} class.
	 * @param domainType the class for event body conversion.
	 * @see ReactiveMongoOperations#changeStream(String, ChangeStreamOptions, Class)
	 */
	public void setDomainType(Class<?> domainType) {
		Assert.notNull(domainType, "'domainType' must not be null");
		this.domainType = domainType;
	}

	/**
	 * Specify a collection name to track change events from.
	 * By default tracks all the collection in the {@link #mongoOperations} configured database.
	 * @param collection a collection to use.
	 * @see ReactiveMongoOperations#changeStream(String, ChangeStreamOptions, Class)
	 */
	public void setCollection(String collection) {
		this.collection = collection;
	}

	/**
	 * Specify a {@link ChangeStreamOptions}.
	 * @param options the {@link ChangeStreamOptions} to use.
	 * @see ReactiveMongoOperations#changeStream(String, ChangeStreamOptions, Class)
	 */
	public void setOptions(ChangeStreamOptions options) {
		Assert.notNull(options, "'options' must not be null");
		this.options = options;
	}

	/**
	 * Configure this channel adapter to build a {@link Message} to produce
	 * with a payload based on a {@link org.springframework.data.mongodb.core.ChangeStreamEvent#getBody()} (by default)
	 * or use a whole {@link org.springframework.data.mongodb.core.ChangeStreamEvent} as a payload.
	 * @param extractBody to extract {@link org.springframework.data.mongodb.core.ChangeStreamEvent#getBody()} or not.
	 */
	public void setExtractBody(boolean extractBody) {
		this.extractBody = extractBody;
	}

	@Override
	public String getComponentType() {
		return "mongo:change-stream-inbound-channel-adapter";
	}

	@Override
	protected void doStart() {
		Flux<Message<?>> changeStreamFlux =
				this.mongoOperations.changeStream(this.collection, this.options, this.domainType)
						.map(event ->
								MessageBuilder
										.withPayload(
												!this.extractBody || event.getBody() == null
														? event
														: event.getBody())
										.setHeader(MongoHeaders.COLLECTION_NAME, event.getCollectionName())
										.setHeader(MongoHeaders.CHANGE_STREAM_OPERATION_TYPE, event.getOperationType())
										.setHeader(MongoHeaders.CHANGE_STREAM_TIMESTAMP, event.getTimestamp())
										.setHeader(MongoHeaders.CHANGE_STREAM_RESUME_TOKEN, event.getResumeToken())
										.build());

		subscribeToPublisher(changeStreamFlux);
	}

}
