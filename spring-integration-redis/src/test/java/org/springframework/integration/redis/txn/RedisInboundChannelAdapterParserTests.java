/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.redis.txn;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author David Liu
 * since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisInboundChannelAdapterParserTests extends RedisAvailableTests {

	@Autowired
	private ApplicationContext context;



	private class DispatchMessageListener implements MessageListener {

		@Override
		public void onMessage(Message message, byte[] pattern) {
			try {
				assertEquals("HELLO", org.apache.commons.io.IOUtils.toString(message.getBody(), "UTF-8"));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	@RedisAvailable
	public void testInboundChannelAdapterMessaging() throws Exception {
		RedisConnectionFactory connectionFactory = context.getBean("redisConnectionFactory",
				RedisConnectionFactory.class);
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		MessageListenerAdapter messageAdapter = new MessageListenerAdapter(new DispatchMessageListener());
		messageAdapter.afterPropertiesSet();
		container.addMessageListener(messageAdapter, new ChannelTopic("outbound"));
		container.afterPropertiesSet();
		container.start();
		connectionFactory.getConnection().publish("foo".getBytes(), "Hello".getBytes());

	}

	@Test
	@RedisAvailable
	public void testException() throws Exception {
		RedisConnectionFactory connectionFactory = context.getBean("redisConnectionFactory",
				RedisConnectionFactory.class);
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		MessageListenerAdapter messageAdapter = new MessageListenerAdapter(new DispatchMessageListener());
		messageAdapter.afterPropertiesSet();
		container.addMessageListener(messageAdapter, new ChannelTopic("exceptionOutbound"));
		container.afterPropertiesSet();
		container.start();
		connectionFactory.getConnection().publish("exceptionFoo".getBytes(), "Hello".getBytes());
	}

	@SuppressWarnings("unused")
	private static class TestMessageConverter extends SimpleMessageConverter {
	}

}
