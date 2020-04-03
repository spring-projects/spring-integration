/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.mongodb.support;

/**
 * Pre-defined names and prefixes to be used for
 * for dealing with headers required by Mongo components
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 */
public final class MongoHeaders {

	private MongoHeaders() {
	}

	/**
	 * A prefix for MongoDb-specific message headers.
	 */
	public static final String PREFIX = "mongo_";

	/**
	 * The prefix for change stream event headers.
	 * @since 5.3
	 */
	public static final String PREFIX_CHANGE_STREAM = PREFIX + "changeStream_";

	/**
	 * The header for MongoDb collection name.
	 */
	public static final String COLLECTION_NAME = PREFIX + "collectionName";

	/**
	 * The header for change stream event type.
	 * @since 5.3
	 */
	public static final String CHANGE_STREAM_OPERATION_TYPE = PREFIX_CHANGE_STREAM + "operationType";

	/**
	 * The header for change stream event timestamp.
	 * @since 5.3
	 */
	public static final String CHANGE_STREAM_TIMESTAMP = PREFIX_CHANGE_STREAM + "timestamp";

	/**
	 * The header for change stream event resume token.
	 * @since 5.3
	 */
	public static final String CHANGE_STREAM_RESUME_TOKEN = PREFIX_CHANGE_STREAM + "resumeToken";

}
