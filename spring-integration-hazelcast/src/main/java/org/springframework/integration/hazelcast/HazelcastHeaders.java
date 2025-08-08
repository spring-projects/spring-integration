/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast;

/**
 * Hazelcast Message Headers.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
public abstract class HazelcastHeaders {

	private static final String PREFIX = "hazelcast_";

	/**
	 * The event type header name.
	 */
	public static final String EVENT_TYPE = PREFIX + "eventType";

	/**
	 * The member header name.
	 */
	public static final String MEMBER = PREFIX + "member";

	/**
	 * The cache name header name.
	 */
	public static final String CACHE_NAME = PREFIX + "cacheName";

	/**
	 * The publishing time header name.
	 */
	public static final String PUBLISHING_TIME = PREFIX + "publishingTime";

}
