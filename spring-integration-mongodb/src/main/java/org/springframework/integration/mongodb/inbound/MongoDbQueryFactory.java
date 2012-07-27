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
package org.springframework.integration.mongodb.inbound;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * The factory interface that would be used by the inbound channel adapter get an instance of
 * {@link Query} to be executed on the MongoDB to get the results from the underlying Mongo Database.
 * The interface also provided the {@link Update} instance that would be executed on the
 *
 * @author Amol Nayak
 *
 * @since 2.2
 *
 */
public interface MongoDbQueryFactory {

	/**
	 * Returns an implementation that would be invoked by the inbound adapter to get the
	 * {@link Query} instance
	 *
	 * @return the instance of the {@link Query} to be executed by the inbound adapter
	 */
	Query getQuery();

	/**
	 * Boolean value indicating whether the query is static or could be potentially
	 * different on subsequent invocations. Return true to indicate the query is static and
	 * the getQuery method will be invoked only once on startup and the {@link Query} instance
	 * obtained will be used on subsequent polls. Returning false will ensure that the getQuery
	 * method is invoked on each poll of the inbound adapter
	 *
	 * @return true if the query returned by getQuery is static, else false.
	 */
	boolean isStaticQuery();


	/**
	 * The optional {@link Update} statement that would be executed on the first document
	 * selected from the database. The documents selected are those which are returned using
	 * getQuery method
	 *
	 * @return the {@link Update} instance
	 */
	Update getUpdate();


	/**
	 * Indicates whether the {@link Update} instance provided by this factory is static or
	 * a new instance can be expected by the adapter on each poll.
	 *
	 * @return true if the instance is static else a false is returned
	 */
	boolean isUpdateStatic();
}
