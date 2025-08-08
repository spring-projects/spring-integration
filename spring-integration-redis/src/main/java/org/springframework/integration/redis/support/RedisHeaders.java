/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.redis.support;

/**
 * Pre-defined names and prefixes to be used for
 * for dealing with headers required by Redis components
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Attoumane Ahamadi
 *
 * @since 2.2
 */
public final class RedisHeaders {

	private RedisHeaders() {
	}

	public static final String PREFIX = "redis_";

	public static final String KEY = PREFIX + "key";

	public static final String MAP_KEY = PREFIX + "mapKey";

	public static final String ZSET_SCORE = PREFIX + "zsetScore";

	public static final String ZSET_INCREMENT_SCORE = PREFIX + "zsetIncrementScore";

	public static final String COMMAND = PREFIX + "command";

	public static final String MESSAGE_SOURCE = PREFIX + "messageSource";

	public static final String STREAM_KEY = PREFIX + "streamKey";

	public static final String STREAM_MESSAGE_ID = PREFIX + "streamMessageId";

	public static final String CONSUMER_GROUP = PREFIX + "consumerGroup";

	public static final String CONSUMER = PREFIX + "consumer";

}
