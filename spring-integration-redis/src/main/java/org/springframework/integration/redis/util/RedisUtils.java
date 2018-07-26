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

package org.springframework.integration.redis.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;

/**
 * A set of utility methods for common Redis functions.
 *
 * @author Artem Bilan
 *
 * @since 5.1
 */
public final class RedisUtils {

	private static final String SECTION = "server";

	private static final String VERSION_PROPERTY = "redis_version";

	private static final Map<RedisOperations<?, ?>, Boolean> unlinkAvailable =
			new LinkedHashMap<RedisOperations<?, ?>, Boolean>() {

				@Override
				protected boolean removeEldestEntry(Map.Entry eldest) {
					return size() > 100;
				}

			};

	/**
	 * Perform an {@code INFO} command on the provided {@link RedisOperations} to check
	 * the Redis server version to be sure that {@code UNLINK} is available or not.
	 * @param redisOperations the {@link RedisOperations} to perform {@code INFO} command.
	 * @return true or false if {@code UNLINK} Redis command is available or not.
	 * @throws IllegalStateException when {@code INFO} returns null from the Redis.
	 */
	public static boolean isUnlinkAvailable(RedisOperations<?, ?> redisOperations) {
		return unlinkAvailable.computeIfAbsent(redisOperations, key -> {
			Properties info = redisOperations.execute(
					(RedisCallback<Properties>) connection -> connection.serverCommands().info(SECTION));
			if (info != null) {
				int majorVersion = Integer.parseInt(info.getProperty(VERSION_PROPERTY).split("\\.")[0]);
				return majorVersion >= 4;
			}
			else {
				throw new IllegalStateException("The INFO command cannot be used in pipeline/transaction.");
			}
		});
	}

	private RedisUtils() {
	}

}
