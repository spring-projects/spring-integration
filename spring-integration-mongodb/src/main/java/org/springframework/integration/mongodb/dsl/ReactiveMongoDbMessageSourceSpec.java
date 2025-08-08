/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.mongodb.dsl;

import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.expression.Expression;
import org.springframework.integration.mongodb.inbound.ReactiveMongoDbMessageSource;

/**
 * A {@link AbstractMongoDbMessageSourceSpec} implementation for a {@link ReactiveMongoDbMessageSource}.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class ReactiveMongoDbMessageSourceSpec
		extends AbstractMongoDbMessageSourceSpec<ReactiveMongoDbMessageSourceSpec, ReactiveMongoDbMessageSource> {

	protected ReactiveMongoDbMessageSourceSpec(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
			Expression queryExpression) {

		this.target = new ReactiveMongoDbMessageSource(reactiveMongoDatabaseFactory, queryExpression);
	}

	protected ReactiveMongoDbMessageSourceSpec(ReactiveMongoOperations reactiveMongoTemplate,
			Expression queryExpression) {

		this.target = new ReactiveMongoDbMessageSource(reactiveMongoTemplate, queryExpression);
	}

}
