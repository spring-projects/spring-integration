/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.mongodb.dsl;

import org.springframework.data.mongodb.core.ChangeStreamOptions;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.mongodb.inbound.MongoDbChangeStreamMessageProducer;

/**
 * A {@link MessageProducerSpec} for tne {@link MongoDbChangeStreamMessageProducer}.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class MongoDbChangeStreamMessageProducerSpec
		extends MessageProducerSpec<MongoDbChangeStreamMessageProducerSpec, MongoDbChangeStreamMessageProducer> {

	/**
	 * Construct a builder based on an initial {@link MongoDbChangeStreamMessageProducerSpec}.
	 * @param producer the {@link MongoDbChangeStreamMessageProducerSpec} to use.
	 */
	public MongoDbChangeStreamMessageProducerSpec(MongoDbChangeStreamMessageProducer producer) {
		super(producer);
	}

	/**
	 * Configure a domain type to convert change event body into.
	 * @param domainType the type to use.
	 * @return the spec.
	 */
	public MongoDbChangeStreamMessageProducerSpec domainType(Class<?> domainType) {
		this.target.setDomainType(domainType);
		return this;
	}

	/**
	 * Configure a collection to subscribe for change events.
	 * @param collection the collection to use.
	 * @return the spec.
	 */
	public MongoDbChangeStreamMessageProducerSpec collection(String collection) {
		this.target.setCollection(collection);
		return this;
	}

	/**
	 * Configure a {@link ChangeStreamOptions}.
	 * @param options the {@link ChangeStreamOptions} to use.
	 * @return the spec.
	 */
	public MongoDbChangeStreamMessageProducerSpec options(ChangeStreamOptions options) {
		this.target.setOptions(options);
		return this;
	}

	/**
	 * Configure a flag to extract body from a change event or use event as a payload.
	 * @param extractBody to extract body or not.
	 * @return the spec.
	 */
	public MongoDbChangeStreamMessageProducerSpec extractBody(boolean extractBody) {
		this.target.setExtractBody(extractBody);
		return this;
	}

}
