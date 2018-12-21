/*
 * Copyright 2018 the original author or authors.
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

import org.bson.Document;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;

/**
 * The callback to be used with the {@link MongoDbOutboundGateway}
 * as an alternative to other query options on the gateway.
 * <p>
 * Plays the same role as standard {@link org.springframework.data.mongodb.core.CollectionCallback},
 * but with {@code Message<?> requestMessage} context during {@code handleMessage()}
 * process in the {@link MongoDbOutboundGateway}.
 *
 * @author Artem Bilan
 *
 * @since 5.0.11
 *
 * @see org.springframework.data.mongodb.core.CollectionCallback
 */
@FunctionalInterface
public interface MessageCollectionCallback<T> {

	/**
	 * Perform a Mongo operation in the collection using request message as a context.
	 * @param collection never {@literal null}.
	 * @param requestMessage the request message ot use for operations
	 * @return can be {@literal null}.
	 * @throws MongoException the MongoDB-specific exception
	 * @throws DataAccessException the data access exception
	 */
	@Nullable
	T doInCollection(MongoCollection<Document> collection, Message<?> requestMessage)
			throws MongoException, DataAccessException;

}
