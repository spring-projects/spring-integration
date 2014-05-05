/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Assume;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public final class RedisAvailableRule implements MethodRule {

	public static final int REDIS_PORT = 6379;

	static ThreadLocal<JedisConnectionFactory> connectionFactoryResource = new ThreadLocal<JedisConnectionFactory>();

	public Statement apply(final Statement base, final FrameworkMethod method, Object target) {
		RedisAvailable redisAvailable = method.getAnnotation(RedisAvailable.class);
		if (redisAvailable != null) {
			JedisConnectionFactory connectionFactory = null;
			try {
				connectionFactory = new JedisConnectionFactory();
				connectionFactory.setPort(REDIS_PORT);
				connectionFactory.afterPropertiesSet();
				connectionFactory.getConnection();
				connectionFactoryResource.set(connectionFactory);
			}
			catch (Exception e) {
				if (connectionFactory != null) {
					connectionFactory.destroy();
				}
				return new Statement() {
					@Override
					public void evaluate() throws Throwable {
						Assume.assumeTrue("Skipping test due to Redis not being available on port: " + REDIS_PORT, false);
					}
				};
			}

			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					try {
						base.evaluate();
					}
					finally {
						JedisConnectionFactory connectionFactory = connectionFactoryResource.get();
						connectionFactoryResource.remove();
						if (connectionFactory != null) {
							connectionFactory.destroy();
						}
					}
				}
			};
		}

		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				base.evaluate();
			}
		};
	}

}

