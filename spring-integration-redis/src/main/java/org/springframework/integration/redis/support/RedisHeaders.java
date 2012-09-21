package org.springframework.integration.redis.support;

/**
 * Pre-defined names and prefixes to be used for
 * for dealing with headers required by Redis components
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.2
 */
public class RedisHeaders {

	public static final String PREFIX = "redis_";

	public static final String KEY = PREFIX + "key";

	public static final String MAP_KEY = PREFIX + "mapKey";

	public static final String ZSET_SCORE = PREFIX + "zsetScore";

	public static final String ZSET_INCREMENT_SCORE = PREFIX + "zsetIncrementScore";

}
