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
package org.springframework.integration.redis.channel;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SubscribableRedisChannelTests extends RedisAvailableTests{
	

	@Test 
	@RedisAvailable
	public void pubSubChanneTest() throws Exception{
		JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
		connectionFactory.setPort(7379);
		connectionFactory.afterPropertiesSet();

		SubscribableRedisChannel channel = new SubscribableRedisChannel(connectionFactory, "si.test.channel");
		channel.setBeanFactory(mock(BeanFactory.class));
		channel.afterPropertiesSet();
		channel.start();
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);

		channel.send(new GenericMessage<String>("1"));
		channel.send(new GenericMessage<String>("2"));
		channel.send(new GenericMessage<String>("3"));
		Thread.sleep(1000);
		verify(handler, times(3)).handleMessage(Mockito.any(Message.class));
	}
}
