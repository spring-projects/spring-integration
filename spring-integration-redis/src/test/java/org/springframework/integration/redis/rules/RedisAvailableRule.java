/*
 * Copyright 2002-2011 the original author or authors.
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

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

/**
 * @author Oleg Zhurakousky
 *
 */
public final class RedisAvailableRule implements MethodRule{

	public Statement apply(final Statement base, final FrameworkMethod method, Object target) {
		return new Statement(){

			@Override
			public void evaluate() throws Throwable {
				RedisAvailable redisAvailable = method.getAnnotation(RedisAvailable.class);
				if (redisAvailable != null){
					try {
						JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
						connectionFactory.setPort(7379);
						connectionFactory.afterPropertiesSet();
						connectionFactory.getConnection();
					} catch (Exception e) {
						System.out.println("Redis is not available. Skipping the test.");
						return;
					}
				}
				base.evaluate();
			}		
		};
		
	}

}
