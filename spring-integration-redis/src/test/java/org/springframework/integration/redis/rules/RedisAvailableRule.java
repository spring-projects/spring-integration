/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.redis.rules;

import java.time.Duration;

import org.junit.Assume;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public final class RedisAvailableRule implements MethodRule {

	public static final int REDIS_PORT = 6379;

	public static LettuceConnectionFactory connectionFactory;

	protected static void setupConnectionFactory() {
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
		redisStandaloneConfiguration.setPort(REDIS_PORT);

		LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
				.clientOptions(
						ClientOptions.builder()
								.socketOptions(
										SocketOptions.builder()
												.connectTimeout(Duration.ofMillis(10000))
												.build())
								.build())
				.commandTimeout(Duration.ofSeconds(10000))
				.build();

		connectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfiguration);
		connectionFactory.afterPropertiesSet();
	}


	public static void cleanUpConnectionFactoryIfAny() {
		if (connectionFactory != null) {
			connectionFactory.destroy();
		}
	}


	public Statement apply(final Statement base, final FrameworkMethod method, Object target) {
		RedisAvailable redisAvailable = method.getAnnotation(RedisAvailable.class);
		if (redisAvailable != null) {
			if (connectionFactory != null) {
				try {
					connectionFactory.getConnection();
				}
				catch (Exception e) {
					Assume.assumeTrue("Skipping test due to Redis not being available on port: " + REDIS_PORT + ": " + e, false);

				}
			}
		}

		return base;
	}

}

