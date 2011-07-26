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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.message.GenericMessage;
/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SubscribableRedisChannelTests {

	@Test //@Ignore // requires instance of redis-server
	public void pubSubChanneTest() throws Exception{
		JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
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
		Thread.sleep(100);
		verify(handler, times(3)).handleMessage(Mockito.any(Message.class));
	}
}
