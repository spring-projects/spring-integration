/*
 * Copyright 2014-2020 the original author or authors.
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
