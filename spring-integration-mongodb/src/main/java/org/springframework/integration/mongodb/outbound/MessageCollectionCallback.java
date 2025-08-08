/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.mongodb.outbound;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * The callback to be used with the {@link MongoDbOutboundGateway}
 * as an alternative to other query options on the gateway.
 * <p>
 * Plays the same role as standard {@link CollectionCallback},
 * but with {@code Message<?> requestMessage} context during {@code handleMessage()}
 * process in the {@link MongoDbOutboundGateway}.
 *
 * @param <T> the expected item type.
 *
 * @author Artem Bilan
 *
 * @since 5.0.11
 *
 * @see CollectionCallback
 */
@FunctionalInterface
public interface MessageCollectionCallback<T> extends CollectionCallback<T> {

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

	@Override
	default T doInCollection(MongoCollection<Document> collection) throws MongoException, DataAccessException {
		throw new UnsupportedOperationException("The 'doInCollection(MongoCollection<Document>, Message<?>)' " +
				"must be implemented instead.");
	}

}
