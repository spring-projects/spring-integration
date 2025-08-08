/*
 * Copyright © 2021 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2021-present the original author or authors.
 */

package org.springframework.integration.mongodb.dsl;

import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.expression.Expression;
import org.springframework.integration.mongodb.inbound.MongoDbMessageSource;

/**
 * A {@link AbstractMongoDbMessageSourceSpec} implementation for a {@link MongoDbMessageSource}.
 *
 * @author Artem Bilan
 *
 * @since 5.5
 */
public class MongoDbMessageSourceSpec
		extends AbstractMongoDbMessageSourceSpec<MongoDbMessageSourceSpec, MongoDbMessageSource> {

	protected MongoDbMessageSourceSpec(MongoDatabaseFactory mongoDatabaseFactory, Expression queryExpression) {
		this.target = new MongoDbMessageSource(mongoDatabaseFactory, queryExpression);
	}

	protected MongoDbMessageSourceSpec(MongoOperations mongoTemplate, Expression queryExpression) {
		this.target = new MongoDbMessageSource(mongoTemplate, queryExpression);
	}

}
