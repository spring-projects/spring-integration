/*
 * Copyright 2013-2024 the original author or authors.
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

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.Jackson2JsonMessageParser;
import org.springframework.integration.support.json.JsonInboundMessageMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Rainer Frey
 * @author Artem Vozhdayenko
 *
 * @since 3.0
 */
@SpringJUnitConfig
@DirtiesContext
class RedisQueueOutboundChannelAdapterTests implements RedisContainerTest {

	@Autowired
	private RedisConnectionFactory connectionFactory;

	@Autowired
	@Qualifier("toRedisQueueChannel")
	private MessageChannel sendChannel;

	@Test
	void testInt3015Default() {

		final String queueName = "si.test.testRedisQueueOutboundChannelAdapter";

		final RedisQueueOutboundChannelAdapter handler = new RedisQueueOutboundChannelAdapter(queueName,
				this.connectionFactory);

		String payload = "testing";
		handler.handleMessage(MessageBuilder.withPayload(payload).build());

		RedisTemplate<String, ?> redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.afterPropertiesSet();

		Object result = redisTemplate.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);
		assertThat(result)
				.isNotNull()
				.isEqualTo(payload);

		Date payload2 = new Date();
		handler.handleMessage(MessageBuilder.withPayload(payload2).build());

		RedisTemplate<String, ?> redisTemplate2 = new RedisTemplate<String, Object>();
		redisTemplate2.setConnectionFactory(this.connectionFactory);
		redisTemplate2.setEnableDefaultSerializer(false);
		redisTemplate2.setKeySerializer(new StringRedisSerializer());
		redisTemplate2.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate2.afterPropertiesSet();

		Object result2 = redisTemplate2.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);
		assertThat(result2)
				.isNotNull()
				.isEqualTo(payload2);
	}

	@Test
	void testInt3015ExtractPayloadFalse() {

		final String queueName = "si.test.testRedisQueueOutboundChannelAdapter2";

		final RedisQueueOutboundChannelAdapter handler = new RedisQueueOutboundChannelAdapter(queueName,
				this.connectionFactory);
		handler.setExtractPayload(false);

		Message<String> message = MessageBuilder.withPayload("testing").build();
		handler.handleMessage(message);

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.setEnableDefaultSerializer(false);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.afterPropertiesSet();

		Object result = redisTemplate.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);
		assertThat(result)
				.isNotNull()
				.isEqualTo(message);

	}

	@Test
	void testInt3015ExplicitSerializer() {

		final String queueName = "si.test.testRedisQueueOutboundChannelAdapter2";

		final RedisQueueOutboundChannelAdapter handler = new RedisQueueOutboundChannelAdapter(queueName,
				this.connectionFactory);
		handler.setSerializer(new Jackson2JsonRedisSerializer<Object>(Object.class));

		RedisTemplate<String, ?> redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.afterPropertiesSet();

		handler.handleMessage(new GenericMessage<Object>(Arrays.asList("foo", "bar", "baz")));

		Object result = redisTemplate.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);
		assertThat(result)
				.isNotNull()
				.isEqualTo("[\"foo\",\"bar\",\"baz\"]");

		handler.handleMessage(new GenericMessage<Object>("test"));

		result = redisTemplate.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);
		assertThat(result)
				.isNotNull()
				.isEqualTo("\"test\"");
	}

	@Test
	void testInt3017IntegrationOutbound() {

		final String queueName = "si.test.Int3017IntegrationOutbound";

		GenericMessage<Object> message = new GenericMessage<Object>(queueName);
		this.sendChannel.send(message);

		RedisTemplate<String, String> redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.afterPropertiesSet();

		String result = redisTemplate.boundListOps(queueName).rightPop(5000, TimeUnit.MILLISECONDS);
		assertThat(result).isNotNull();
		InboundMessageMapper<String> mapper = new JsonInboundMessageMapper(String.class,
				new Jackson2JsonMessageParser());
		Message<?> resultMessage = mapper.toMessage(result);
		assertThat(resultMessage.getPayload()).isEqualTo(message.getPayload());
	}

	@Test
	void testInt3932LeftPushFalse() {

		final String queueName = "si.test.Int3932LeftPushFalse";

		final RedisQueueOutboundChannelAdapter handler = new RedisQueueOutboundChannelAdapter(queueName,
				this.connectionFactory);
		handler.setLeftPush(false);

		String payload = "testing";
		handler.handleMessage(MessageBuilder.withPayload(payload).build());

		Date payload2 = new Date();
		handler.handleMessage(MessageBuilder.withPayload(payload2).build());

		RedisTemplate<String, ?> redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(this.connectionFactory);
		redisTemplate.afterPropertiesSet();

		Object result = redisTemplate.boundListOps(queueName).leftPop(5000, TimeUnit.MILLISECONDS);
		assertThat(result)
				.isNotNull()
				.isEqualTo(payload);

		RedisTemplate<String, ?> redisTemplate2 = new RedisTemplate<String, Object>();
		redisTemplate2.setConnectionFactory(this.connectionFactory);
		redisTemplate2.setEnableDefaultSerializer(false);
		redisTemplate2.setKeySerializer(new StringRedisSerializer());
		redisTemplate2.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate2.afterPropertiesSet();

		Object result2 = redisTemplate2.boundListOps(queueName).leftPop(5000, TimeUnit.MILLISECONDS);
		assertThat(result2)
				.isNotNull()
				.isEqualTo(payload2);
	}

}
