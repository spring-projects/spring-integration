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
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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

	protected void prepareList(JedisConnectionFactory jcf){

		RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<Object, Object>();
		redisTemplate.setConnectionFactory(jcf);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());

		BoundListOperations<Object, Object> ops = redisTemplate.boundListOps("presidents");

		ops.rightPush("John Adams");

		ops.rightPush("Barack Obama");
		ops.rightPush("Thomas Jefferson");
		ops.rightPush("John Quincy Adams");
		ops.rightPush("Zachary Taylor");

		ops.rightPush("Theodore Roosevelt");
		ops.rightPush("Woodrow Wilson");
		ops.rightPush("George W. Bush");
		ops.rightPush("Franklin D. Roosevelt");
		ops.rightPush("Ronald Reagan");
		ops.rightPush("William J. Clinton");
		ops.rightPush("Abraham Lincoln");
		ops.rightPush("George Washington");
	}

	protected void prepareZset(JedisConnectionFactory jcf){

		RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<Object, Object>();
		redisTemplate.setConnectionFactory(jcf);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());

		BoundZSetOperations<Object, Object> ops = redisTemplate.boundZSetOps("presidents");

		ops.add("John Adams", 18);

		ops.add("Barack Obama", 21);
		ops.add("Thomas Jefferson", 19);
		ops.add("John Quincy Adams", 19);
		ops.add("Zachary Taylor", 19);

		ops.add("Theodore Roosevelt", 20);
		ops.add("Woodrow Wilson", 20);
		ops.add("George W. Bush", 21);
		ops.add("Franklin D. Roosevelt", 20);
		ops.add("Ronald Reagan", 20);
		ops.add("William J. Clinton", 20);
		ops.add("Abraham Lincoln", 19);
		ops.add("George Washington", 18);
	}
}
