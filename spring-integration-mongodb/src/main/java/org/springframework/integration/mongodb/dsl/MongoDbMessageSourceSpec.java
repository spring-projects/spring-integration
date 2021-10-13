/*
 * Copyright 2021 the original author or authors.
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
