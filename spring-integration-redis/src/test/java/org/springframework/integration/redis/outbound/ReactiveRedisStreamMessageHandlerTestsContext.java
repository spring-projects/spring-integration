/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.redis.outbound;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.ReactiveMessageHandlerAdapter;
import org.springframework.integration.redis.rules.RedisAvailableRule;
import org.springframework.messaging.MessageChannel;

/**
 * @author Attoumane Ahamadi
 *
 * @since 5.4
 */
@Configuration
public class ReactiveRedisStreamMessageHandlerTestsContext {
	public static final String STREAM_KEY = "myStream";

	@Bean
	public MessageChannel streamChannel(ReactiveMessageHandlerAdapter messageHandlerAdapter) {
		DirectChannel directChannel = new DirectChannel();
		directChannel.subscribe(messageHandlerAdapter);
		return directChannel;
	}


	@Bean
	public ReactiveRedisStreamMessageHandler streamMessageHandler(ReactiveRedisConnectionFactory connectionFactory) {
		return new ReactiveRedisStreamMessageHandler(connectionFactory, STREAM_KEY);
	}

	@Bean
	public ReactiveMessageHandlerAdapter reactiveMessageHandlerAdapter(ReactiveRedisStreamMessageHandler streamMessageHandler) {
		return new ReactiveMessageHandlerAdapter(streamMessageHandler);
	}

	@Bean
	public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
		return RedisAvailableRule.connectionFactory;
	}
}
