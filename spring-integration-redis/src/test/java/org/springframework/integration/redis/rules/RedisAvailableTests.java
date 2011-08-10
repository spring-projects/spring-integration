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

import java.util.UUID;

import org.junit.Rule;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author Oleg Zhurakousky
 *
 */
public class RedisAvailableTests {
	@Rule
	public RedisAvailableRule redisAvailableRule = new RedisAvailableRule();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JedisConnectionFactory getConnectionFactoryForTest(){
		JedisConnectionFactory jcf = new JedisConnectionFactory();
		jcf.setPort(7379);
		jcf.afterPropertiesSet();
		RedisTemplate rt = new RedisTemplate<UUID, Object>();
		rt.setConnectionFactory(jcf);
		rt.execute(new RedisCallback() {

			public Object doInRedis(RedisConnection connection)
					throws DataAccessException {
				connection.flushDb();
				return null;
			}
		});
		return jcf;
	}
}
