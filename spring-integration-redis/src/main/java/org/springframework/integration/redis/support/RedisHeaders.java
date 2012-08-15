package org.springframework.integration.redis.support;

/**
 * Pre-defined names and prefixes to be used for
 * for dealing with headers required by Redis components
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisHeaders {

	public static final String PREFIX = "redis_";

	public static final String KEY = PREFIX + "key";

	public static final String MAP_KEY = PREFIX + "map_key";

	public static final String ZSET_SCORE = PREFIX + "zset_score";

}
