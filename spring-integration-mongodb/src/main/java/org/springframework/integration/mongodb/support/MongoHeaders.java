/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.mongodb.support;

/**
 * Pre-defined names and prefixes to be used
 * for dealing with headers required by Mongo components.
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
